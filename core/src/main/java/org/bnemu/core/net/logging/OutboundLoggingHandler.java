package org.bnemu.core.net.logging;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.bnemu.core.net.packet.BncsPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutboundLoggingHandler extends ChannelOutboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(OutboundLoggingHandler.class);

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof BncsPacket) {
            BncsPacket packet = (BncsPacket) msg;
            logger.debug("Outbound packet sent: 0x{}", String.format("%02X", packet.getCommand()));
        }
        super.write(ctx, msg, promise);
    }
}
