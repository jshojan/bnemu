package org.bnemu.bncs.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.chat.*;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

public class ChatCommandHandler extends BncsPacketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatCommandHandler.class);

    // Product tags (wire byte order) → human-readable names
    private static final Map<String, String> PRODUCT_NAMES = Map.of(
        "RATS", "Starcraft",
        "PXES", "Starcraft: Brood War",
        "VD2D", "Diablo II",
        "PX2D", "Diablo II: Lord of Destruction",
        "3RAW", "Warcraft III: Reign of Chaos",
        "PX3W", "Warcraft III: The Frozen Throne",
        "NB2W", "Warcraft II: Battle.net Edition",
        "RHSD", "Diablo"
    );

    private final SessionManager sessions;
    private final ChatChannelManager channelManager;
    private final WhisperManager whisperManager;
    private final String realmName;

    public ChatCommandHandler(SessionManager sessions, ChatChannelManager channelManager, String realmName) {
        this.sessions = sessions;
        this.channelManager = channelManager;
        this.whisperManager = new WhisperManager(sessions);
        this.realmName = realmName;
    }

    @Override
    public BncsPacketId bncsPacketId() {
        return BncsPacketId.SID_CHATCOMMAND;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        String username = sessions.getUsername(ctx.channel());
        String currentChannel = sessions.get(ctx.channel(), "channel");

        if (username == null) {
            return;
        }

        var message = packet.payload().readString();
        logger.debug("Received chat command: '{}'", message);

        if (message.startsWith("/")) {
            handleCommand(ctx, username, currentChannel, message);
            return;
        }

        // Normal message
        ChatChannel chan = channelManager.getChannel(currentChannel);
        if (chan != null) {
            chan.broadcastChatEvent(ChatEventIds.EID_TALK.getId(), username, message);
        }
    }

    private void handleCommand(ChannelHandlerContext ctx, String username, String currentChannel, String message) {
        String lower = message.toLowerCase();

        // --- Channel navigation ---
        if (lower.startsWith("/join ") || lower.startsWith("/channel ") || lower.startsWith("/j ")) {
            String arg = message.substring(message.indexOf(' ') + 1).trim();
            if (!arg.isEmpty()) {
                channelManager.joinChannel(arg, ctx, username);
            }
            return;
        }

        if (lower.equals("/rejoin")) {
            if (currentChannel != null) {
                channelManager.joinChannel(currentChannel, ctx, username);
            }
            return;
        }

        // --- Whisper ---
        if (lower.startsWith("/whisper ") || lower.startsWith("/w ") || lower.startsWith("/msg ") || lower.startsWith("/m ")) {
            String[] parts = message.split(" ", 3);
            if (parts.length >= 3) {
                whisperManager.sendWhisper(ctx.channel(), username, parts[1], parts[2]);
            }
            return;
        }

        // --- Emote ---
        if (lower.startsWith("/emote ") || lower.startsWith("/me ")) {
            String emote = message.substring(message.indexOf(' ') + 1);
            ChatChannel chan = channelManager.getChannel(currentChannel);
            if (chan != null) {
                chan.broadcastChatEvent(ChatEventIds.EID_EMOTE.getId(), username, emote);
            }
            return;
        }

        // --- Operator moderation ---
        if (lower.startsWith("/kick ")) {
            handleKick(ctx, username, currentChannel, message.substring(6).trim());
            return;
        }

        if (lower.startsWith("/ban ")) {
            handleBan(ctx, username, currentChannel, message.substring(5).trim());
            return;
        }

        if (lower.startsWith("/unban ")) {
            handleUnban(ctx, username, currentChannel, message.substring(7).trim());
            return;
        }

        // --- Operator management ---
        if (lower.startsWith("/designate ")) {
            handleDesignate(ctx, username, currentChannel, message.substring(11).trim());
            return;
        }

        if (lower.equals("/resign")) {
            handleResign(ctx, username, currentChannel);
            return;
        }

        // --- User status ---
        if (lower.equals("/away") || lower.startsWith("/away ")) {
            handleAway(ctx, username, message.length() > 5 ? message.substring(6).trim() : "");
            return;
        }

        if (lower.equals("/dnd") || lower.startsWith("/dnd ")) {
            handleDnd(ctx, username, message.length() > 4 ? message.substring(5).trim() : "");
            return;
        }

        // --- Squelch ---
        if (lower.startsWith("/squelch ") || lower.startsWith("/ignore ")) {
            String target = message.substring(message.indexOf(' ') + 1).trim();
            handleSquelch(ctx, username, target);
            return;
        }

        if (lower.startsWith("/unsquelch ") || lower.startsWith("/unignore ")) {
            String target = message.substring(message.indexOf(' ') + 1).trim();
            handleUnsquelch(ctx, username, target);
            return;
        }

        // --- Information ---
        if (lower.equals("/who") || lower.startsWith("/who ")) {
            String chanName = lower.equals("/who") ? currentChannel : message.substring(5).trim();
            handleWho(ctx, chanName);
            return;
        }

        if (lower.startsWith("/whois ") || lower.startsWith("/where ") || lower.startsWith("/whereis ")) {
            String target = message.substring(message.indexOf(' ') + 1).trim();
            handleWhois(ctx, target);
            return;
        }

        if (lower.equals("/whoami")) {
            handleWhoami(ctx, username, currentChannel);
            return;
        }

        if (lower.equals("/time")) {
            sendInfo(ctx, "Server time: " +
                ZonedDateTime.now().format(DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy z")));
            return;
        }

        if (lower.equals("/users")) {
            handleUsers(ctx, currentChannel);
            return;
        }

        if (lower.equals("/topic") || lower.startsWith("/topic ")) {
            String topicText = message.length() > 6 ? message.substring(7).trim() : "";
            handleTopic(ctx, username, currentChannel, topicText);
            return;
        }

        // Unknown command — no response per PvPGN (just ignore)
    }

    // --- Operator moderation ---

    private void handleKick(ChannelHandlerContext ctx, String username, String currentChannel, String targetUser) {
        ChatChannel chan = channelManager.getChannel(currentChannel);
        if (chan == null) return;

        if (!chan.isOperator(ctx.channel())) {
            sendError(ctx, "You are not a channel operator.");
            return;
        }

        if (targetUser.equalsIgnoreCase(username)) {
            sendError(ctx, "You can't kick yourself.");
            return;
        }

        Channel kicked = chan.kickMember(targetUser);
        if (kicked == null) {
            sendError(ctx, "That user is not in the channel.");
            return;
        }

        // Notify the kicked user, then send them to The Void
        chan.sendInfoMessage(kicked, "You were kicked out of the channel by " + username + ".");
        chan.sendToVoid(kicked);

        // Notify the operator
        sendInfo(ctx, targetUser + " was kicked out of the channel by " + username + ".");
    }

    private void handleBan(ChannelHandlerContext ctx, String username, String currentChannel, String targetUser) {
        ChatChannel chan = channelManager.getChannel(currentChannel);
        if (chan == null) return;

        if (!chan.isOperator(ctx.channel())) {
            sendError(ctx, "You are not a channel operator.");
            return;
        }

        if (targetUser.equalsIgnoreCase(username)) {
            sendError(ctx, "You can't ban yourself.");
            return;
        }

        // Add to ban list
        chan.ban(targetUser);

        // Kick if currently in channel, then send to The Void
        Channel banned = chan.kickMember(targetUser);
        if (banned != null) {
            chan.sendInfoMessage(banned, "You were banned from the channel by " + username + ".");
            chan.sendToVoid(banned);
        }

        sendInfo(ctx, targetUser + " was banned from the channel by " + username + ".");
    }

    private void handleUnban(ChannelHandlerContext ctx, String username, String currentChannel, String targetUser) {
        ChatChannel chan = channelManager.getChannel(currentChannel);
        if (chan == null) return;

        if (!chan.isOperator(ctx.channel())) {
            sendError(ctx, "You are not a channel operator.");
            return;
        }

        if (!chan.isBanned(targetUser)) {
            sendError(ctx, "That user is not banned.");
            return;
        }

        chan.unban(targetUser);
        sendInfo(ctx, targetUser + " is no longer banned from this channel.");
    }

    // --- Operator management ---

    private void handleDesignate(ChannelHandlerContext ctx, String username, String currentChannel, String targetUser) {
        ChatChannel chan = channelManager.getChannel(currentChannel);
        if (chan == null) return;

        if (!chan.isOperator(ctx.channel())) {
            sendError(ctx, "You are not a channel operator.");
            return;
        }

        // Target must be in the channel
        Channel target = chan.findMemberByUsername(targetUser);
        if (target == null) {
            sendError(ctx, "That user is not in the channel.");
            return;
        }

        chan.setDesignatedOperator(targetUser);
        sendInfo(ctx, targetUser + " is the designated heir.");
    }

    private void handleResign(ChannelHandlerContext ctx, String username, String currentChannel) {
        ChatChannel chan = channelManager.getChannel(currentChannel);
        if (chan == null) return;

        if (!chan.isOperator(ctx.channel())) {
            sendError(ctx, "You are not a channel operator.");
            return;
        }

        // Find next operator: designated first, then any other member
        Channel nextOp = null;
        String designated = chan.getDesignatedOperator();
        if (designated != null) {
            nextOp = chan.findMemberByUsername(designated);
            chan.setDesignatedOperator(null);
        }

        if (nextOp == null) {
            for (String name : chan.getUsernames()) {
                if (!name.equalsIgnoreCase(username)) {
                    nextOp = chan.findMemberByUsername(name);
                    if (nextOp != null) break;
                }
            }
        }

        if (nextOp != null) {
            // promoteOperator demotes current op and promotes the new one
            chan.promoteOperator(nextOp);
        }
        // If no other members, stay as operator (nothing to resign to)
    }

    // --- User status ---

    private void handleAway(ChannelHandlerContext ctx, String username, String awayMsg) {
        String current = sessions.get(ctx.channel(), "away");

        if (awayMsg.isEmpty() && current != null) {
            // Toggle off
            sessions.set(ctx.channel(), "away", null);
            sendInfo(ctx, "You are no longer marked as away.");
        } else if (!awayMsg.isEmpty()) {
            sessions.set(ctx.channel(), "away", awayMsg);
            sendInfo(ctx, "You are now marked as being away.");
        } else {
            sendInfo(ctx, "You are no longer marked as away.");
            sessions.set(ctx.channel(), "away", null);
        }
    }

    private void handleDnd(ChannelHandlerContext ctx, String username, String dndMsg) {
        String current = sessions.get(ctx.channel(), "dnd");

        if (dndMsg.isEmpty() && current != null) {
            // Toggle off
            sessions.set(ctx.channel(), "dnd", null);
            sendInfo(ctx, "Do Not Disturb mode cancelled.");
        } else if (!dndMsg.isEmpty()) {
            sessions.set(ctx.channel(), "dnd", dndMsg);
            sendInfo(ctx, "Do Not Disturb mode engaged.");
        } else {
            sendInfo(ctx, "Do Not Disturb mode cancelled.");
            sessions.set(ctx.channel(), "dnd", null);
        }
    }

    // --- Squelch ---

    private void handleSquelch(ChannelHandlerContext ctx, String username, String targetUser) {
        String squelchList = sessions.get(ctx.channel(), "squelch");
        if (squelchList == null) squelchList = "";

        // Check if already squelched
        for (String s : squelchList.split(",")) {
            if (s.equalsIgnoreCase(targetUser)) {
                sendError(ctx, targetUser + " is already being ignored.");
                return;
            }
        }

        // Add to squelch list
        if (squelchList.isEmpty()) {
            squelchList = targetUser;
        } else {
            squelchList += "," + targetUser;
        }
        sessions.set(ctx.channel(), "squelch", squelchList);
        sendInfo(ctx, "Ignoring " + targetUser + ".");
    }

    private void handleUnsquelch(ChannelHandlerContext ctx, String username, String targetUser) {
        String squelchList = sessions.get(ctx.channel(), "squelch");
        if (squelchList == null || squelchList.isEmpty()) {
            // Silently succeed — PvPGN does not send an error here.
            return;
        }

        StringBuilder newList = new StringBuilder();
        boolean found = false;
        for (String s : squelchList.split(",")) {
            if (s.equalsIgnoreCase(targetUser)) {
                found = true;
            } else {
                if (newList.length() > 0) newList.append(",");
                newList.append(s);
            }
        }

        if (!found) {
            // Silently succeed — PvPGN does not send an error here.
            // StealthBot periodically sends /unignore as maintenance.
            return;
        }

        sessions.set(ctx.channel(), "squelch", newList.length() > 0 ? newList.toString() : null);
        sendInfo(ctx, targetUser + " is no longer being ignored.");
    }

    // --- Information ---

    private void handleWho(ChannelHandlerContext ctx, String channelName) {
        if (channelName == null || channelName.isEmpty()) {
            sendError(ctx, "You are not in a channel.");
            return;
        }

        ChatChannel chan = channelManager.getChannel(channelName);
        if (chan == null || chan.getUserCount() == 0) {
            sendError(ctx, "That channel does not exist.");
            return;
        }

        Set<String> names = chan.getUsernames();
        sendInfo(ctx, "Users in channel " + channelName + ":");
        for (String name : names) {
            sendInfo(ctx, name);
        }
        sendInfo(ctx, "Total of " + names.size() + " user(s).");
    }

    private void handleWhois(ChannelHandlerContext ctx, String targetUser) {
        Channel target = sessions.getChannelByUsername(targetUser);
        if (target == null || !target.isActive()) {
            sendError(ctx, "That user is not logged on.");
            return;
        }

        String targetChannel = sessions.get(target, "channel");
        String product = sessions.get(target, "product");
        String productName = PRODUCT_NAMES.getOrDefault(product, product != null ? product : "Unknown");

        String info = targetUser + " is using " + productName;
        if (targetChannel != null) {
            info += " in the channel " + targetChannel + ".";
        } else {
            info += ".";
        }
        sendInfo(ctx, info);

        if (realmName != null && !realmName.isEmpty()) {
            sendInfo(ctx, "On server @" + realmName + ".");
        }
    }

    private void handleWhoami(ChannelHandlerContext ctx, String username, String currentChannel) {
        String product = sessions.get(ctx.channel(), "product");
        String productName = PRODUCT_NAMES.getOrDefault(product, product != null ? product : "Unknown");
        String info = "You are " + username + ", using " + productName;
        if (currentChannel != null) {
            info += " in the channel " + currentChannel + ".";
        } else {
            info += ".";
        }
        sendInfo(ctx, info);

        if (realmName != null && !realmName.isEmpty()) {
            sendInfo(ctx, "On server @" + realmName + ".");
        }
    }

    private void handleUsers(ChannelHandlerContext ctx, String currentChannel) {
        ChatChannel chan = channelManager.getChannel(currentChannel);
        if (chan == null) {
            sendError(ctx, "You are not in a channel.");
            return;
        }
        Set<String> names = chan.getUsernames();
        String userList = String.join(", ", names);
        sendInfo(ctx, "Users in channel: " + userList);
    }

    private void handleTopic(ChannelHandlerContext ctx, String username, String currentChannel, String topicText) {
        ChatChannel chan = channelManager.getChannel(currentChannel);
        if (chan == null) return;

        if (!chan.isOperator(ctx.channel())) {
            sendError(ctx, "You are not a channel operator.");
            return;
        }

        if (topicText.isEmpty()) {
            // Show current topic
            String current = chan.getTopic();
            if (current != null) {
                sendInfo(ctx, "Topic: " + current);
            } else {
                sendInfo(ctx, "No topic is set.");
            }
            return;
        }

        chan.setTopic(topicText);

        // Broadcast the new topic to all channel members
        for (io.netty.channel.Channel ch : chan.getMembers()) {
            if (ch.isActive()) {
                chan.sendInfoMessage(ch, "Topic: " + topicText);
            }
        }
    }

    // --- Helpers ---

    private void sendInfo(ChannelHandlerContext ctx, String message) {
        var packet = ChatEventBuilder.build(
            ChatEventIds.EID_INFO.getId(), 0, 0, 0, 0, 0, "", message
        );
        ctx.writeAndFlush(new BncsPacket(BncsPacketId.SID_CHATEVENT, packet));
    }

    private void sendError(ChannelHandlerContext ctx, String message) {
        var packet = ChatEventBuilder.build(
            ChatEventIds.EID_ERROR.getId(), 0, 0, 0, 0, 0, "", message
        );
        ctx.writeAndFlush(new BncsPacket(BncsPacketId.SID_CHATEVENT, packet));
    }
}
