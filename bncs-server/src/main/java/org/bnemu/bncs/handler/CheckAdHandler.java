package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketId;

/**
 * Handles SID_CHECKAD (0x15) - Ad banner check.
 * No-op: the emulator has no ads to serve.
 */
public class CheckAdHandler extends BncsPacketHandler {

    @Override
    public BncsPacketId bncsPacketId() {
        return BncsPacketId.SID_CHECKAD;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        // Intentionally empty — no ads to serve.
    }
}
