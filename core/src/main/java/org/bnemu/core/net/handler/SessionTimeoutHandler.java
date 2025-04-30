package org.bnemu.core.net.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import org.bnemu.core.chat.ChatChannelManager;
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
        if (evt instanceof IdleStateEvent idleEvent && idleEvent.state() == IdleStateEvent.READER_IDLE_STATE_EVENT.state()) {
            String username = channelManager.getSessionManager().get(ctx.channel(), "username");
            String channel = channelManager.getSessionManager().get(ctx.channel(), "channel");

            logger.info("Disconnecting idle user: {}", username != null ? username : ctx.channel().remoteAddress());

            // Leave current chat channel gracefully
            channelManager.leaveChannel(ctx);
            logger.debug("Removed user '{}' from channel '{}'", username, channel);

            ctx.close(); // Close connection
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
