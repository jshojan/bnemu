package org.bnemu.d2cs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.d2cs.net.packet.McpPacket;
import org.bnemu.d2cs.net.packet.McpPacketBuffer;
import org.bnemu.d2cs.net.packet.McpPacketId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles MCP_MOTD (0x12) - Message of the Day request.
 */
public class MotdHandler extends McpPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(MotdHandler.class);

    @Override
    public McpPacketId packetId() {
        return McpPacketId.MCP_MOTD;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, McpPacket packet) {
        var input = packet.payload();

        // C->S format varies - some clients send empty payload, some send (DWORD)
        int requestedData = 0;
        if (input.length() >= 4) {
            requestedData = input.readDword();
        }

        logger.debug("MCP_MOTD: requested data = 0x{} (payload size={})",
            String.format("%02X", requestedData), packet.payload().length());

        // S->C format:
        // (BYTE) Unknown (1 per protocol)
        // (STRING) MOTD message
        var output = new McpPacketBuffer()
            .writeByte(1)
            .writeString("Welcome to bnemu realm!");

        send(ctx, output);
    }
}
