package org.bnemu.bncs.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.chat.ChatChannel;
import org.bnemu.bncs.chat.ChatChannelManager;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketHandler;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JoinChannelHandler implements BncsPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(JoinChannelHandler.class);
    private final SessionManager sessions;
    private final ChatChannelManager channelManager;

    public JoinChannelHandler(SessionManager sessions, ChatChannelManager channelManager) {
        this.sessions = sessions;
        this.channelManager = channelManager;
    }

    @Override
    public boolean supports(byte packetId) {
        return packetId == BncsPacketId.SID_JOINCHANNEL;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        String sessionUsername = sessions.getUsername(ctx.channel());
        if (sessionUsername == null) {
            logger.warn("JoinChannelHandler: Session username is null. User may not be logged in.");
            return;
        }

        ByteBuf buf = packet.getPayload();
        if (buf.readableBytes() < 6) {
            logger.warn("JoinChannelHandler: Not enough bytes in payload.");
            return;
        }

        int flags = buf.readIntLE();
        String clientTag = readCString(buf);
        String accountNameFromClient = readCString(buf);
        String channelName = readCString(buf);

        // Prefer the account name from client if it’s valid
        String username = (accountNameFromClient != null && !accountNameFromClient.isBlank())
                ? accountNameFromClient
                : sessionUsername;

        logger.debug("[JoinChannel] Flags: {}, ClientTag: '{}', AccountFromClient: '{}', ChannelName: '{}', UsingUsername: '{}'",
                flags, clientTag, accountNameFromClient, channelName, username);

        if (channelName == null || channelName.isEmpty()) {
            channelName = "The Void";
        }

        sessions.set(ctx.channel(), "channel", channelName);
        sessions.set(ctx.channel(), "clientTag", clientTag);
        sessions.set(ctx.channel(), "username", username);  // <- use this username going forward

        ChatChannel channel = channelManager.getOrCreateChannel(channelName);
        channel.addMember(ctx.channel(), username);
    }

    private String readCString(ByteBuf buf) {
        StringBuilder sb = new StringBuilder();
        while (buf.isReadable()) {
            byte b = buf.readByte();
            if (b == 0x00) break;
            sb.append((char) b);
        }
        return sb.toString();
    }
}
