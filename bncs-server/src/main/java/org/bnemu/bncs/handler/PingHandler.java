package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketHandler;
import org.bnemu.bncs.net.packet.BncsPacketId;

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