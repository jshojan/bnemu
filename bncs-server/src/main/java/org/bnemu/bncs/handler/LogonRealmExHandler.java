package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketBuffer;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.auth.RealmToken;
import org.bnemu.core.auth.RealmTokenStore;
import org.bnemu.core.config.PortConfig;
import org.bnemu.core.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles SID_LOGONREALMEX (0x3E) for Diablo 2.
 * Generates auth token and returns D2CS connection info.
 */
public class LogonRealmExHandler extends BncsPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(LogonRealmExHandler.class);

    private static final int STATUS_SUCCESS = 0x00;

    private final SessionManager sessionManager;
    private final PortConfig d2csConfig;
    private final RealmTokenStore tokenStore;

    public LogonRealmExHandler(SessionManager sessionManager, PortConfig d2csConfig) {
        this.sessionManager = sessionManager;
        this.d2csConfig = d2csConfig;
        this.tokenStore = RealmTokenStore.getInstance();
    }

    @Override
    public BncsPacketId bncsPacketId() {
        return BncsPacketId.SID_LOGONREALMEX;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        var input = packet.payload();

        // C->S format per BNetDocs:
        // (DWORD) Client Token
        // (DWORD[5]) Hashed Realm Password (20 bytes - we ignore this)
        // (STRING) Realm title
        int clientToken = input.readDword();
        input.skipBytes(20); // Skip hashed password
        String realmTitle = input.readString();

        String username = sessionManager.getUsername(ctx.channel());
        if (username == null) {
            logger.warn("SID_LOGONREALMEX: No username in session");
            sendError(ctx);
            return;
        }

        // Get server token from session
        String serverTokenStr = sessionManager.get(ctx.channel(), "serverToken");
        int serverToken = serverTokenStr != null ? Integer.parseInt(serverTokenStr) : 0;

        logger.debug("SID_LOGONREALMEX: user='{}', realm='{}', clientToken={}",
            username, realmTitle, clientToken);

        // Generate realm token
        RealmToken token = tokenStore.createToken(username, clientToken, serverToken);

        // Convert IP string to integer (network byte order)
        int ipAddress = ipToInt(d2csConfig.getHost());

        // S->C format per BNetDocs:
        // (DWORD) MCP Cookie
        // (DWORD) MCP Status (0 = success)
        // (DWORD[2]) MCP Chunk 1 (auth data - can be tokens)
        // (DWORD) IP Address
        // (DWORD) Port
        // (DWORD[12]) MCP Chunk 2 (more auth data - padding)
        // (STRING) Unique name (MCSID)
        var output = new BncsPacketBuffer()
            .writeDword(token.getCookie())     // MCP Cookie
            .writeDword(STATUS_SUCCESS)        // MCP Status
            .writeDword(clientToken)           // MCP Chunk 1[0] - client token
            .writeDword(serverToken)           // MCP Chunk 1[1] - server token
            .writeDword(ipAddress)             // IP Address
            .writeDword(portToInt(d2csConfig.getPort())); // Port (byte-swapped)

        // MCP Chunk 2 - 12 DWORDs of padding/auth data
        for (int i = 0; i < 12; i++) {
            output.writeDword(0);
        }

        output.writeString(username);          // Unique name

        logger.debug("Sending realm logon response: cookie={}, ip={}, port={}",
            token.getCookie(), d2csConfig.getHost(), d2csConfig.getPort());

        send(ctx, output);
    }

    private void sendError(ChannelHandlerContext ctx) {
        var output = new BncsPacketBuffer()
            .writeDword(0)  // Cookie
            .writeDword(1); // Status (1 = error)
        send(ctx, output);
    }

    /**
     * Convert IP address string to integer (for D2 client).
     * The D2 client reads IP bytes directly as octets, so we need to
     * arrange them so when written little-endian they appear in correct order.
     */
    private int ipToInt(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            logger.warn("Invalid IP address format: {}, using 127.0.0.1", ip);
            return 0x0100007F; // 127.0.0.1
        }
        try {
            int a = Integer.parseInt(parts[0]);
            int b = Integer.parseInt(parts[1]);
            int c = Integer.parseInt(parts[2]);
            int d = Integer.parseInt(parts[3]);
            // Written little-endian, bytes appear as: a, b, c, d
            return (d << 24) | (c << 16) | (b << 8) | a;
        } catch (NumberFormatException e) {
            logger.warn("Invalid IP address: {}", ip, e);
            return 0x0100007F; // 127.0.0.1
        }
    }

    /**
     * Convert port to format expected by D2 client.
     * Port needs to be byte-swapped so it reads correctly.
     */
    private int portToInt(int port) {
        // Swap bytes so when written little-endian, high byte comes first
        return ((port & 0xFF) << 8) | ((port >> 8) & 0xFF);
    }
}
