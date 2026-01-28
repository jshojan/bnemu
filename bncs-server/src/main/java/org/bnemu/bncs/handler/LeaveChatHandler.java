package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.chat.ChatChannel;
import org.bnemu.bncs.chat.ChatChannelManager;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeaveChatHandler extends BncsPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(LeaveChatHandler.class);

    private final SessionManager sessions;
    private final ChatChannelManager channelManager;

    public LeaveChatHandler(SessionManager sessions, ChatChannelManager channelManager) {
        this.sessions = sessions;
        this.channelManager = channelManager;
    }

    @Override
    public BncsPacketId bncsPacketId() {
        return BncsPacketId.SID_LEAVECHAT;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        String username = sessions.getUsername(ctx.channel());
        String channelName = sessions.get(ctx.channel(), "channel");

        logger.debug("User '{}' leaving chat from channel '{}'", username, channelName);

        if (channelName != null) {
            ChatChannel channel = channelManager.getChannel(channelName);
            if (channel != null) {
                channel.removeMember(ctx.channel());
            }
            sessions.set(ctx.channel(), "channel", null);
        }

        // SID_LEAVECHAT has no response per BNetDocs spec
    }
}
