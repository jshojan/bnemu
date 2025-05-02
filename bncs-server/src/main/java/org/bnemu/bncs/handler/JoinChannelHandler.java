package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.chat.ChatChannel;
import org.bnemu.bncs.chat.ChatChannelManager;
import org.bnemu.bncs.chat.ChatEventBuilder;
import org.bnemu.bncs.chat.ChatEventIds;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JoinChannelHandler extends BncsPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(JoinChannelHandler.class);
    private final SessionManager sessions;
    private final ChatChannelManager channelManager;

    public JoinChannelHandler(SessionManager sessions, ChatChannelManager channelManager) {
        this.sessions = sessions;
        this.channelManager = channelManager;
    }

    @Override
    public BncsPacketId bncsPacketId() {
        return BncsPacketId.SID_JOINCHANNEL;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        String username = sessions.getUsername(ctx.channel());
        if (username == null) {
            logger.warn("JoinChannelHandler: Session username is null. User may not be logged in.");
            return;
        }

        var input = packet.payload();
        var flags = input.readDword();
        var channelName = input.readString();

        if (channelName == null || channelName.isEmpty()) {
            channelName = "The Void";
        }

        if (flags == 0) {
            logger.debug("zero");
            var output = ChatEventBuilder.build(
                ChatEventIds.EID_CHANNELDOESNOTEXIST,
                0,
                0,
                0,
                0,
                0,
                channelName,
                null
            );
            send(ctx, BncsPacketId.SID_CHATEVENT, output);
        } else {
            logger.debug("non-zero");
            sessions.set(ctx.channel(), "channel", channelName);
            sessions.set(ctx.channel(), "username", username);
            ChatChannel channel = channelManager.getOrCreateChannel(channelName);
            channel.addMember(ctx.channel(), username);
        }
    }
}
