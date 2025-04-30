package org.bnemu.core.net.packet;

import io.netty.channel.ChannelHandlerContext;

public interface BncsPacketHandler {
    /**
     * Returns true if the handler supports the given packet ID.
     */
    boolean supports(byte packetId);

    /**
     * Handles the packet logic when matched by dispatcher.
     */
    void handle(ChannelHandlerContext ctx, BncsPacket packet);
}
