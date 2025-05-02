package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketId;

public interface BncsPacketHandler {
    BncsPacketId bncsPacketId();

    void handle(ChannelHandlerContext ctx, BncsPacket packet);
}
