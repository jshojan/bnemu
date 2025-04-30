package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.chat.ChatChannel;
import org.bnemu.bncs.chat.ChatChannelManager;
import org.bnemu.bncs.chat.ChatEventIds;
import org.bnemu.bncs.chat.WhisperManager;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketHandler;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Set;

public class ChatCommandHandler implements BncsPacketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatCommandHandler.class);
    private final SessionManager sessions;
    private final ChatChannelManager channelManager;
    private final WhisperManager whisperManager;

    public ChatCommandHandler(SessionManager sessions, ChatChannelManager channelManager) {
        this.sessions = sessions;
        this.channelManager = channelManager;
        this.whisperManager = new WhisperManager(sessions);
    }

    @Override
    public boolean supports(byte packetId) {
        return packetId == BncsPacketId.SID_CHATCOMMAND;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        String username = sessions.getUsername(ctx.channel());
        String currentChannel = sessions.get(ctx.channel(), "channel");

        if (username == null) {
            return; // Not logged in
        }

        byte[] messageBytes = new byte[packet.getPayload().readableBytes()];
        packet.getPayload().readBytes(messageBytes);
        String message = new String(messageBytes, StandardCharsets.US_ASCII).trim();

        logger.debug("Received chat command: '{}'", message);

        if (message.startsWith("/")) {
            if (message.startsWith("/join ")) {
                String newChannel = message.substring(6).trim();
                if (!newChannel.isEmpty()) {
                    channelManager.joinChannel(newChannel, ctx, username);
                }
                return;
            }

            if (message.startsWith("/whisper ")) {
                String[] parts = message.split(" ", 3);
                if (parts.length >= 3) {
                    String targetUser = parts[1];
                    String whisperMessage = parts[2];
                    whisperManager.sendWhisper(username, targetUser, whisperMessage);
                }
                return;
            }

            if (message.startsWith("/emote ")) {
                String emote = message.substring(7);
                ChatChannel chan = channelManager.getChannel(currentChannel);
                if (chan != null) {
                    chan.broadcastChatEvent(ChatEventIds.EID_EMOTE, username, emote);
                }
                return;
            }

            if (message.startsWith("/whois ")) {
                String targetUser = message.substring(7).trim();
                // TODO: Implement /whois lookup
                return;
            }

            if (message.equals("/users")) {
                ChatChannel chan = channelManager.getChannel(currentChannel);
                if (chan != null) {
                    Set<String> names = chan.getUsernames();
                    String userList = String.join(", ", names);
                    chan.sendSystemMessage(ctx.channel(), "Users in channel: " + userList);
                }
                return;
            }

            if (message.equals("/whoami")) {
                ChatChannel chan = channelManager.getChannel(currentChannel);
                if (chan != null) {
                    chan.sendInfoMessage(ctx.channel(), "You are: " + username);
                }
                return;
            }

            ChatChannel chan = channelManager.getChannel(currentChannel);
            if (chan != null) {
                chan.sendSystemMessage(ctx.channel(), "Unknown command: " + message);
            }
            return;
        }

        // Normal message
        if (!message.startsWith("/") && message.startsWith("You are:")) {
            logger.debug("Filtered self-echoed whoami reply: '{}'", message);
            return;
        }

        ChatChannel chan = channelManager.getChannel(currentChannel);
        if (chan != null) {
            chan.broadcastChatEvent(ChatEventIds.EID_TALK, username, message);
        }
    }
}
