package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.bnemu.bncs.chat.ChatChannelManager;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketBuffer;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionTimeoutHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(SessionTimeoutHandler.class);
    private final ChatChannelManager channelManager;

    public SessionTimeoutHandler(ChatChannelManager channelManager) {
        this.channelManager = channelManager;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idleEvent) {
            if (idleEvent.state() == IdleState.WRITER_IDLE) {
                // Send SID_PING to keep connection alive
                int pingValue = (int) System.currentTimeMillis();
                var pingPayload = new BncsPacketBuffer().writeDword(pingValue);
                ctx.writeAndFlush(new BncsPacket(BncsPacketId.SID_PING, pingPayload));
                logger.debug("Sent keep-alive ping to {}", ctx.channel().remoteAddress());
            } else if (idleEvent.state() == IdleState.READER_IDLE) {
                // No response for too long, disconnect
                String username = channelManager.getSessionManager().get(ctx.channel(), "username");
                String channel = channelManager.getSessionManager().get(ctx.channel(), "channel");

                logger.info("Disconnecting idle user: {}", username != null ? username : ctx.channel().remoteAddress());

                // Leave current chat channel gracefully
                channelManager.leaveChannel(ctx);
                logger.debug("Removed user '{}' from channel '{}'", username, channel);

                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String username = channelManager.getSessionManager().get(ctx.channel(), "username");
        String channel = channelManager.getSessionManager().get(ctx.channel(), "channel");

        if (channel != null) {
            logger.info("User disconnected: {}", username != null ? username : ctx.channel().remoteAddress());
            channelManager.leaveChannel(ctx);
            logger.debug("Removed user '{}' from channel '{}' on disconnect", username, channel);
        }

        super.channelInactive(ctx);
    }
}
