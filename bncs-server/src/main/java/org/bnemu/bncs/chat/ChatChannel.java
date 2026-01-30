package org.bnemu.bncs.chat;

import io.netty.channel.Channel;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public class ChatChannel {
    private static final Logger logger = LoggerFactory.getLogger(ChatChannel.class);

    // User flags per BNetDocs
    private static final int FLAG_OPERATOR = 0x02;  // Channel operator

    private final String name;
    private final Set<Channel> members = ConcurrentHashMap.newKeySet();
    private final SessionManager sessionManager;
    private Channel operator;  // Track channel operator

    // Ban list: case-insensitive usernames banned from this channel
    private final Set<String> banList = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    // Designated next operator (set via /designate, activated when current op leaves)
    private String designatedOperator;

    // Channel topic (set via /topic by operator, null = no topic)
    private String topic;

    public ChatChannel(String name, SessionManager sessionManager) {
        this.name = name;
        this.sessionManager = sessionManager;
    }

    /**
     * Add a member to the channel. Returns false if user is banned.
     */
    public boolean addMember(Channel newChannel, String username) {
        logger.debug("Adding member '{}' to channel '{}' (current members: {})", username, name, members.size());

        // Check ban list
        if (isBanned(username)) {
            logger.debug("Rejecting banned user '{}' from channel '{}'", username, name);
            return false;
        }

        // Get the new user's statstring
        String newUserStatstring = getStatstring(newChannel);

        // Determine flags for new user - first user becomes operator
        int newUserFlags = members.isEmpty() ? FLAG_OPERATOR : 0;
        if (newUserFlags == FLAG_OPERATOR) {
            operator = newChannel;
        }
        sessionManager.set(newChannel, "flags", String.valueOf(newUserFlags));

        // 1. Send EID_CHANNEL first (tells client which channel they're joining)
        var channelInfo = ChatEventBuilder.build(
            ChatEventIds.EID_CHANNEL.getId(), 0, 0, 0, 0, 0, "", name
        );
        newChannel.writeAndFlush(new BncsPacket(BncsPacketId.SID_CHATEVENT, channelInfo));

        // 2. Send EID_SHOWUSER for each existing user to the new user
        for (Channel existing : members) {
            if (!existing.equals(newChannel) && existing.isActive()) {
                String otherUser = sessionManager.get(existing, "username");
                String otherStatstring = getStatstring(existing);
                int otherFlags = getFlags(existing);
                if (otherUser != null) {
                    var showUser = ChatEventBuilder.build(
                        ChatEventIds.EID_SHOWUSER.getId(), otherFlags, getPing(existing), 0, 0, 0, otherUser, otherStatstring
                    );
                    newChannel.writeAndFlush(new BncsPacket(BncsPacketId.SID_CHATEVENT, showUser));
                }
            }
        }

        // 3. Send EID_SHOWUSER to the new user for themselves
        var showSelf = ChatEventBuilder.build(
            ChatEventIds.EID_SHOWUSER.getId(), newUserFlags, getPing(newChannel), 0, 0, 0, username, newUserStatstring
        );
        newChannel.writeAndFlush(new BncsPacket(BncsPacketId.SID_CHATEVENT, showSelf));

        // 4. Add the new member
        members.add(newChannel);
        sessionManager.set(newChannel, "channel", name);
        sessionManager.set(newChannel, "username", username);

        // 5. Notify existing users that the new user joined (EID_JOIN)
        logger.debug("Sending EID_JOIN for '{}' to {} members (excluding self)", username, members.size());
        for (Channel ch : members) {
            String targetUser = sessionManager.get(ch, "username");
            boolean isSelf = ch.equals(newChannel);
            logger.debug("  -> Member '{}': isSelf={}, active={}", targetUser, isSelf, ch.isActive());
            if (!isSelf && ch.isActive()) {
                var joinNotify = ChatEventBuilder.build(
                    ChatEventIds.EID_JOIN.getId(), newUserFlags, getPing(newChannel), 0, 0, 0, username, newUserStatstring
                );
                ch.writeAndFlush(new BncsPacket(BncsPacketId.SID_CHATEVENT, joinNotify));
            }
        }

        // 6. Channel topic as EID_INFO (only if set)
        if (topic != null) {
            sendInfoMessage(newChannel, "Topic: " + topic);
        }
        return true;
    }

    private String getStatstring(Channel channel) {
        String statstring = sessionManager.get(channel, "statstring");
        return statstring != null ? statstring : "";
    }

    private int getFlags(Channel channel) {
        String flagsStr = sessionManager.get(channel, "flags");
        if (flagsStr != null) {
            try {
                return Integer.parseInt(flagsStr);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private int getPing(Channel channel) {
        String pingStr = sessionManager.get(channel, "ping");
        if (pingStr != null) {
            try {
                return Integer.parseInt(pingStr);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    public void removeMember(Channel channel) {
        String username = sessionManager.get(channel, "username");
        logger.debug("Removing member '{}' from channel '{}' (current members: {})", username, name, members.size());

        if (members.remove(channel)) {
            int flags = getFlags(channel);
            boolean wasOperator = channel.equals(operator);

            if (wasOperator && !members.isEmpty()) {
                promoteNextOperator();
            }

            if (username != null) {
                // Broadcast EID_LEAVE to remaining members
                for (Channel ch : members) {
                    if (ch.isActive()) {
                        var leaveEvent = ChatEventBuilder.build(
                            ChatEventIds.EID_LEAVE.getId(), flags, 0, 0, 0, 0, username, ""
                        );
                        ch.writeAndFlush(new BncsPacket(BncsPacketId.SID_CHATEVENT, leaveEvent));
                    }
                }
            }
        }
    }

    /**
     * Kick a member from the channel (no ban). Returns the kicked Channel, or null if not found.
     * Clears the kicked user's channel session and broadcasts EID_LEAVE to remaining members.
     */
    public Channel kickMember(String targetUsername) {
        Channel target = findMemberByUsername(targetUsername);
        if (target == null) return null;

        String username = sessionManager.get(target, "username");
        int flags = getFlags(target);
        boolean wasOperator = target.equals(operator);

        members.remove(target);

        // Clear kicked user's channel session so they can't send messages
        sessionManager.set(target, "channel", null);

        if (wasOperator && !members.isEmpty()) {
            promoteNextOperator();
        }

        // Broadcast EID_LEAVE to remaining members
        if (username != null) {
            for (Channel ch : members) {
                if (ch.isActive()) {
                    var leaveEvent = ChatEventBuilder.build(
                        ChatEventIds.EID_LEAVE.getId(), flags, 0, 0, 0, 0, username, ""
                    );
                    ch.writeAndFlush(new BncsPacket(BncsPacketId.SID_CHATEVENT, leaveEvent));
                }
            }
        }

        return target;
    }

    /**
     * Send "The Void" channel events to a user (after kick/ban).
     * Per PvPGN: EID_CHANNEL "The Void", EID_SHOWUSER (self), EID_INFO no chat privileges.
     */
    public void sendToVoid(Channel target) {
        if (!target.isActive()) return;

        String username = sessionManager.get(target, "username");
        int flags = getFlags(target);

        // 1. EID_CHANNEL - tells client they're now in "The Void"
        var channelEvent = ChatEventBuilder.build(
            ChatEventIds.EID_CHANNEL.getId(), 0, 0, 0, 0, 0, "", "The Void"
        );
        target.writeAndFlush(new BncsPacket(BncsPacketId.SID_CHATEVENT, channelEvent));

        // 2. EID_SHOWUSER - show themselves (only user visible in The Void)
        if (username != null) {
            var showSelf = ChatEventBuilder.build(
                ChatEventIds.EID_SHOWUSER.getId(), flags, getPing(target), 0, 0, 0,
                username, getStatstring(target)
            );
            target.writeAndFlush(new BncsPacket(BncsPacketId.SID_CHATEVENT, showSelf));
        }

        // 3. EID_INFO - no chat privileges
        sendInfoMessage(target, "This channel does not have chat privileges.");
    }

    /**
     * Promote the next operator after the current one leaves or resigns.
     * Checks designated operator first, then falls back to next member.
     */
    private void promoteNextOperator() {
        Channel newOp = null;

        // Check designated operator first
        if (designatedOperator != null) {
            newOp = findMemberByUsername(designatedOperator);
            designatedOperator = null; // Consume the designation
        }

        // Fall back to first available member
        if (newOp == null) {
            newOp = members.iterator().next();
        }

        promoteOperator(newOp);
    }

    /**
     * Promote a specific channel member to operator.
     */
    public void promoteOperator(Channel newOp) {
        // Demote current operator if exists and still in channel
        if (operator != null && members.contains(operator)) {
            sessionManager.set(operator, "flags", "0");
            String oldOpName = sessionManager.get(operator, "username");
            if (oldOpName != null) {
                broadcastUserFlags(operator, 0);
            }
        }

        operator = newOp;
        sessionManager.set(newOp, "flags", String.valueOf(FLAG_OPERATOR));

        String newOpName = sessionManager.get(newOp, "username");
        if (newOpName != null) {
            broadcastUserFlags(newOp, FLAG_OPERATOR);
        }
    }

    /**
     * Broadcast EID_USERFLAGS for a user to all channel members.
     */
    private void broadcastUserFlags(Channel target, int flags) {
        String username = sessionManager.get(target, "username");
        if (username == null) return;

        for (Channel ch : members) {
            if (ch.isActive()) {
                var flagsUpdate = ChatEventBuilder.build(
                    ChatEventIds.EID_USERFLAGS.getId(), flags, getPing(target), 0, 0, 0,
                    username, getStatstring(target)
                );
                ch.writeAndFlush(new BncsPacket(BncsPacketId.SID_CHATEVENT, flagsUpdate));
            }
        }
    }

    /**
     * Find a member Channel by username (case-insensitive).
     */
    public Channel findMemberByUsername(String username) {
        // Strip leading '*' — Battle.net convention for account-name lookup
        if (username.startsWith("*")) {
            username = username.substring(1);
        }
        // Exact match first
        for (Channel ch : members) {
            if (ch.isActive()) {
                String memberName = sessionManager.get(ch, "username");
                if (memberName != null && memberName.equalsIgnoreCase(username)) {
                    return ch;
                }
            }
        }
        // Fallback: match by character name or account name for D2 "CharName*AccountName" format
        for (Channel ch : members) {
            if (ch.isActive()) {
                String memberName = sessionManager.get(ch, "username");
                if (memberName != null && memberName.contains("*")) {
                    int star = memberName.indexOf('*');
                    String charPart = memberName.substring(0, star);
                    String acctPart = memberName.substring(star + 1);
                    if (charPart.equalsIgnoreCase(username) || acctPart.equalsIgnoreCase(username)) {
                        return ch;
                    }
                }
            }
        }
        return null;
    }

    // --- Ban list ---

    public boolean isBanned(String username) {
        if (banList.contains(username)) return true;

        // For D2 "CharName*AccountName" display names, also check each part
        if (username.contains("*")) {
            int star = username.indexOf('*');
            String charPart = username.substring(0, star);
            String acctPart = username.substring(star + 1);
            if (banList.contains(charPart) || banList.contains(acctPart)) return true;
        }
        return false;
    }

    public void ban(String username) {
        banList.add(username);
    }

    public void unban(String username) {
        banList.remove(username);
    }

    // --- Operator checks ---

    public boolean isOperator(Channel channel) {
        return channel.equals(operator);
    }

    public Channel getOperator() {
        return operator;
    }

    // --- Designated operator ---

    public String getDesignatedOperator() {
        return designatedOperator;
    }

    public void setDesignatedOperator(String username) {
        this.designatedOperator = username;
    }

    // --- Topic ---

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    // --- Existing methods ---

    public String getName() {
        return name;
    }

    public int getUserCount() {
        return members.size();
    }

    public Set<Channel> getMembers() {
        return members;
    }

    public Set<String> getUsernames() {
        Set<String> names = new HashSet<>();
        for (Channel ch : members) {
            if (ch.isActive()) {
                String name = sessionManager.get(ch, "username");
                if (name != null) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    public void broadcastChatEvent(int eid, String user, String text) {
        // Look up sender's channel to get their flags
        Channel senderChannel = sessionManager.getChannelByUsername(user);
        int senderFlags = senderChannel != null ? getFlags(senderChannel) : 0;

        // Only skip sender for EID_TALK - clients display their own chat locally
        // For EID_EMOTE and other events, send to everyone including sender
        boolean skipSender = (eid == ChatEventIds.EID_TALK.getId());

        int writeCount = 0;
        logger.debug("Broadcasting EID {} from '{}' to {} members in channel '{}' (skipSender={})",
                String.format("0x%02X", eid), user, members.size(), name, skipSender);

        for (Channel ch : members) {
            String targetUser = sessionManager.get(ch, "username");
            if (skipSender && ch.equals(senderChannel)) {
                logger.debug("  -> Skipping sender '{}'", targetUser);
                continue;
            }

            // Check if recipient has squelched the sender
            if (isSquelched(ch, user)) {
                logger.debug("  -> Skipping '{}' (squelched sender '{}')", targetUser, user);
                continue;
            }

            if (ch.isActive()) {
                writeCount++;
                logger.debug("  -> Write #{}: to '{}' text='{}'", writeCount, targetUser, text);
                int senderPing = senderChannel != null ? getPing(senderChannel) : 0;
                var packet = ChatEventBuilder.build(eid, senderFlags, senderPing, 0, 0, 0, user, text);
                ch.writeAndFlush(new BncsPacket(BncsPacketId.SID_CHATEVENT, packet));
            } else {
                logger.debug("  -> Skipping '{}' (active=false)", targetUser);
            }
        }
        logger.debug("Broadcast complete: {} total writes", writeCount);
    }

    /**
     * Check if a recipient has squelched the sender.
     */
    private boolean isSquelched(Channel recipient, String senderUsername) {
        String squelchList = sessionManager.get(recipient, "squelch");
        if (squelchList == null || squelchList.isEmpty()) return false;
        for (String squelched : squelchList.split(",")) {
            if (squelched.equalsIgnoreCase(senderUsername)) return true;
        }
        return false;
    }

    public void sendSystemMessage(Channel target, String message) {
        if (target.isActive()) {
            var packet = ChatEventBuilder.build(
                ChatEventIds.EID_BROADCAST.getId(), 0, 0, 0, 0, 0, "Battle.net", message
            );
            var output = new BncsPacket(BncsPacketId.SID_CHATEVENT, packet);
            target.writeAndFlush(output);
        }
    }

    public void sendInfoMessage(Channel target, String message) {
        if (target.isActive()) {
            // Per BNetDocs, EID_INFO uses empty username
            var packet = ChatEventBuilder.build(
                ChatEventIds.EID_INFO.getId(), 0, 0, 0, 0, 0, "", message
            );
            var output = new BncsPacket(BncsPacketId.SID_CHATEVENT, packet);
            target.writeAndFlush(output);
        }
    }

    public void sendErrorMessage(Channel target, String message) {
        if (target.isActive()) {
            var packet = ChatEventBuilder.build(
                ChatEventIds.EID_ERROR.getId(), 0, 0, 0, 0, 0, "", message
            );
            var output = new BncsPacket(BncsPacketId.SID_CHATEVENT, packet);
            target.writeAndFlush(output);
        }
    }
}
