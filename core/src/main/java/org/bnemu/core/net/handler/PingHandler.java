package org.bnemu.core.net.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.core.net.packet.BncsPacket;
import org.bnemu.core.net.packet.BncsPacketHandler;
import org.bnemu.core.net.packet.BncsPacketId;

public class PingHandler implements BncsPacketHandler {

    @Override
    public boolean supports(byte packetId) {
        return packetId == BncsPacketId.SID_PING;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        // Echo the ping back as-is
        ctx.writeAndFlush(packet);
    }
}