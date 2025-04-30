package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;

public interface BncsPacketHandler {
    boolean supports(byte packetId);
    void handle(ChannelHandlerContext ctx, BncsPacket packet);
}