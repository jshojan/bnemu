package org.bnemu.d2cs.net.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Decodes MCP packets with 3-byte header: [SIZE:2][ID:1]
 * First byte from client is protocol ID (0x01), which we consume.
 */
public class McpPacketDecoder extends ByteToMessageDecoder {
    private static final Logger logger = LoggerFactory.getLogger(McpPacketDecoder.class);
    private static final int HEADER_SIZE = 3;

    private boolean protocolReceived = false;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // First byte is protocol ID (0x01)
        if (!protocolReceived) {
            if (in.readableBytes() < 1) {
                return;
            }
            byte protocolId = in.readByte();
            logger.debug("MCP protocol byte received: 0x{}", String.format("%02X", protocolId));
            if (protocolId != 0x01) {
                logger.warn("Invalid MCP protocol byte: 0x{}", String.format("%02X", protocolId));
                ctx.close();
                return;
            }
            protocolReceived = true;
        }

        // Need at least header to determine packet size
        while (in.readableBytes() >= HEADER_SIZE) {
            in.markReaderIndex();

            // Read header: [SIZE:2 little-endian][ID:1]
            int length = in.readUnsignedShortLE();
            byte packetId = in.readByte();

            // Validate length
            if (length < HEADER_SIZE || length > 65535) {
                logger.warn("Invalid MCP packet length: {}", length);
                ctx.close();
                return;
            }

            int payloadSize = length - HEADER_SIZE;

            // Wait for full payload
            if (in.readableBytes() < payloadSize) {
                in.resetReaderIndex();
                return;
            }

            // Read payload
            ByteBuf payload = in.readBytes(payloadSize);
            McpPacketId id = McpPacketId.fromCode(packetId & 0xFF);

            logger.debug("Decoded MCP packet: ID=0x{} ({}) Length={} PayloadSize={}",
                String.format("%02X", packetId), id.name(), length, payloadSize);

            out.add(new McpPacket(id, packetId, new McpPacketBuffer(payload)));
        }
    }
}
