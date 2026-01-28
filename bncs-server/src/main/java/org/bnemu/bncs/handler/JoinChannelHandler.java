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

    // Join channel flags per BNetDocs specification
    private static final int FLAG_NOCREATE = 0x00;    // Only join if channel exists and not empty
    private static final int FLAG_FIRST_JOIN = 0x01;  // Create/join with MOTD
    private static final int FLAG_FORCED_JOIN = 0x02; // Create/join without MOTD
    private static final int FLAG_D2_FIRST = 0x05;    // D2 first join (same as 0x01)

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

        // Some clients (like StealthBot) send flags=0 with format: Product(4) + Username + Channel
        // e.g., "PXESleaddark gates" where PXES=product, lead=username, dark gates=channel
        // We need to parse out the actual channel name
        if (flags == FLAG_NOCREATE && channelName.length() > 4 + username.length()) {
            String possibleProduct = channelName.substring(0, 4);
            // Check if it starts with a known product code pattern (4 uppercase chars or reversed)
            if (channelName.startsWith(possibleProduct) && channelName.substring(4).startsWith(username)) {
                // Extract the actual channel name after product + username
                String actualChannel = channelName.substring(4 + username.length());
                logger.debug("Parsed extended JoinChannel format: product='{}', username='{}', channel='{}'",
                    possibleProduct, username, actualChannel);
                channelName = actualChannel;
            }
        }

        if (channelName == null || channelName.isEmpty()) {
            channelName = "The Void";
        }

        logger.debug("JoinChannel request: flags={}, channel='{}'", flags, channelName);

        if (flags == FLAG_NOCREATE) {
            // NoCreate join: only join if channel exists and has users
            ChatChannel existing = channelManager.getChannel(channelName);
            if (existing == null || existing.getUserCount() == 0) {
                var output = ChatEventBuilder.build(
                    ChatEventIds.EID_CHANNELDOESNOTEXIST.getId(),
                    0, 0, 0, 0, 0,
                    channelName,
                    null
                );
                send(ctx, BncsPacketId.SID_CHATEVENT, output);
                return;
            }
        }

        // Use joinChannel to properly leave old channel and join new one
        channelManager.joinChannel(channelName, ctx, username);
    }
}
