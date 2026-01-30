package org.bnemu.d2cs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.core.auth.RealmToken;
import org.bnemu.core.auth.RealmTokenStore;
import org.bnemu.d2cs.net.packet.McpPacket;
import org.bnemu.d2cs.net.packet.McpPacketBuffer;
import org.bnemu.d2cs.net.packet.McpPacketId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles MCP_STARTUP (0x01) - Initial handshake from client.
 * Validates the MCP cookie from BNCS LOGONREALMEX.
 */
public class StartupHandler extends McpPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(StartupHandler.class);

    private static final int RESULT_SUCCESS = 0x00;
    private static final int RESULT_FAIL = 0x02; // Invalid token

    private final RealmTokenStore tokenStore;

    public StartupHandler(RealmTokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public McpPacketId packetId() {
        return McpPacketId.MCP_STARTUP;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, McpPacket packet) {
        var input = packet.payload();

        // C->S format per MCP protocol:
        // (DWORD) MCP Cookie
        // (DWORD) MCP Status
        // (DWORD[2]) MCP Chunk 1
        // (DWORD[12]) MCP Chunk 2
        // (STRING) Unique name
        int mcpCookie = input.readDword();
        int mcpStatus = input.readDword();
        int chunk1_0 = input.readDword(); // Client token
        int chunk1_1 = input.readDword(); // Server token
        input.skipBytes(48); // MCP Chunk 2 (12 DWORDs)
        String uniqueName = input.readString();

        logger.debug("MCP_STARTUP: cookie={}, status={}, user='{}'", mcpCookie, mcpStatus, uniqueName);

        // Validate token from BNCS
        RealmToken token = tokenStore.validateAndConsume(mcpCookie);
        if (token == null) {
            logger.warn("MCP_STARTUP: Invalid or expired token for cookie {}", mcpCookie);
            sendResult(ctx, RESULT_FAIL);
            return;
        }

        // Verify tokens match
        if (token.getClientToken() != chunk1_0 || token.getServerToken() != chunk1_1) {
            logger.warn("MCP_STARTUP: Token mismatch for user '{}'", uniqueName);
            sendResult(ctx, RESULT_FAIL);
            return;
        }

        logger.info("MCP_STARTUP: User '{}' authenticated successfully", token.getAccountName());

        // Store account name in channel for later use
        ctx.channel().attr(io.netty.util.AttributeKey.valueOf("accountName")).set(token.getAccountName());

        sendResult(ctx, RESULT_SUCCESS);
    }

    private void sendResult(ChannelHandlerContext ctx, int result) {
        // S->C format:
        // (DWORD) Result
        var output = new McpPacketBuffer()
            .writeDword(result);
        send(ctx, output);
    }
}
