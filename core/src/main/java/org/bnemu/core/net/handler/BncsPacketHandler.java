package org.bnemu.core.net.handler;

import org.bnemu.core.net.packet.BncsPacket;
import io.netty.channel.ChannelHandlerContext;

public interface BncsPacketHandler {
    boolean supports(byte packetId);
    void handle(ChannelHandlerContext ctx, BncsPacket packet);
}