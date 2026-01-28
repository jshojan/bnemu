package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingHandler extends BncsPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(PingHandler.class);

    @Override
    public BncsPacketId bncsPacketId() {
        return BncsPacketId.SID_PING;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        // Server receives ping response from client - do NOT echo back
        // Receiving this packet already resets the read idle timer
        logger.debug("Received ping response from {}", ctx.channel().remoteAddress());
    }
}