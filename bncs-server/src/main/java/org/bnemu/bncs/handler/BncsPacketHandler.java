package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketBuffer;
import org.bnemu.bncs.net.packet.BncsPacketId;

public abstract class BncsPacketHandler {
    abstract BncsPacketId bncsPacketId();

    abstract void handle(ChannelHandlerContext ctx, BncsPacket packet);

    protected void send(ChannelHandlerContext ctx, BncsPacketBuffer buffer) {
        ctx.writeAndFlush(new BncsPacket(bncsPacketId(), buffer));
    }

    protected void send(ChannelHandlerContext ctx, BncsPacketId id, BncsPacketBuffer buffer) {
        ctx.writeAndFlush(new BncsPacket(id, buffer));
    }
}
