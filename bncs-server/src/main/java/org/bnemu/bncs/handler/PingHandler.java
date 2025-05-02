package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketId;

public class PingHandler extends BncsPacketHandler {

    @Override
    public BncsPacketId bncsPacketId() {
        return BncsPacketId.SID_PING;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        ctx.writeAndFlush(packet);
    }
}