package org.bnemu.bncs.net.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BncsPacketEncoder extends MessageToByteEncoder<BncsPacket> {
    private static final Logger logger = LoggerFactory.getLogger(BncsPacketEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, BncsPacket msg, ByteBuf out) {
        ByteBuf encoded = msg.payload().withHeader(msg.packetId());
        logger.debug("Encoding packet 0x{} ({} bytes): {}",
            String.format("%02X", msg.packetId().getCode()),
            encoded.readableBytes(),
            ByteBufUtil.hexDump(encoded));
        out.writeBytes(encoded);
    }
}