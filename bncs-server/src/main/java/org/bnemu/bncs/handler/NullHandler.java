package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketId;

/**
 * Handler for SID_NULL (0x00) - Keep-alive packet.
 * Per BNetDocs: This packet is used as a keep-alive. No response is required.
 */
public class NullHandler extends BncsPacketHandler {

    @Override
    public BncsPacketId bncsPacketId() {
        return BncsPacketId.SID_NULL;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        // Keep-alive packet - no response needed
    }
}
