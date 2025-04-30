package org.bnemu.bncs.net.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class BncsPacketEncoder extends MessageToByteEncoder<BncsPacket> {
    @Override
    protected void encode(ChannelHandlerContext ctx, BncsPacket msg, ByteBuf out) {
        out.writeByte(0xFF);
        out.writeByte(msg.getCommand());
        out.writeShortLE(msg.getLength());
        out.writeBytes(msg.getPayload());
    }
}