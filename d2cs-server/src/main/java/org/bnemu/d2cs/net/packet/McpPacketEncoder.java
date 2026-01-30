package org.bnemu.d2cs.net.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encodes MCP packets with 3-byte header: [SIZE:2][ID:1]
 */
public class McpPacketEncoder extends MessageToByteEncoder<McpPacket> {
    private static final Logger logger = LoggerFactory.getLogger(McpPacketEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, McpPacket msg, ByteBuf out) {
        ByteBuf encoded = msg.payload().withHeader(msg.packetId());
        logger.debug("Encoding MCP packet 0x{} ({} bytes): {}",
            String.format("%02X", msg.packetId().getCode()),
            encoded.readableBytes(),
            ByteBufUtil.hexDump(encoded));
        out.writeBytes(encoded);
    }
}
