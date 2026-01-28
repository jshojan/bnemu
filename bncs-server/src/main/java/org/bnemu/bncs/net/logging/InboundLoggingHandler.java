package org.bnemu.bncs.net.logging;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InboundLoggingHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(InboundLoggingHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof BncsPacket packet) {
            logger.debug("Inbound packet received: 0x{} ({})",
                    String.format("%02X", packet.rawPacketId()), packet.packetId());
        }
        super.channelRead(ctx, msg);
    }
}
