package org.bnemu.bncs.chat;

import io.netty.channel.Channel;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatChannel {
    private static final Logger logger = LoggerFactory.getLogger(ChatChannel.class);

    // User flags per BNetDocs
    private static final int FLAG_OPERATOR = 0x02;  // Channel operator

    private final String name;
    private final Set<Channel> members = ConcurrentHashMap.newKeySet();
    private final SessionManager sessionManager;
    private Channel operator;  // Track channel operator

    public ChatChannel(String name, SessionManager sessionManager) {
        this.name = name;
        this.sessionManager = sessionManager;
    }

    public void addMember(Channel newChannel, String username) {
        logger.debug("Adding member '{}' to channel '{}' (current members: {})", username, name, members.size());

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
                        ChatEventIds.EID_SHOWUSER.getId(), otherFlags, 0, 0, 0, 0, otherUser, otherStatstring
                    );
                    newChannel.writeAndFlush(new BncsPacket(BncsPacketId.SID_CHATEVENT, showUser));
                }
            }
        }

        // 3. Send EID_SHOWUSER to the new user for themselves
        var showSelf = ChatEventBuilder.build(
            ChatEventIds.EID_SHOWUSER.getId(), newUserFlags, 0, 0, 0, 0, username, newUserStatstring
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
                    ChatEventIds.EID_JOIN.getId(), newUserFlags, 0, 0, 0, 0, username, newUserStatstring
                );
                ch.writeAndFlush(new BncsPacket(BncsPacketId.SID_CHATEVENT, joinNotify));
            }
        }

        // 6. Channel topic as EID_INFO
        sendInfoMessage(newChannel, "Topic: Welcome to " + name);
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

    public void removeMember(Channel channel) {
        String username = sessionManager.get(channel, "username");
        logger.debug("Removing member '{}' from channel '{}' (current members: {})", username, name, members.size());

        if (members.remove(channel)) {
            int flags = getFlags(channel);

            // If operator left, promote next user
            if (channel.equals(operator) && !members.isEmpty()) {
                Channel newOp = members.iterator().next();
                operator = newOp;
                sessionManager.set(newOp, "flags", String.valueOf(FLAG_OPERATOR));

                // Notify all users of the flag change
                String newOpName = sessionManager.get(newOp, "username");
                if (newOpName != null) {
                    for (Channel ch : members) {
                        if (ch.isActive()) {
                            var flagsUpdate = ChatEventBuilder.build(
                                ChatEventIds.EID_USERFLAGS.getId(), FLAG_OPERATOR, 0, 0, 0, 0,
                                newOpName, getStatstring(newOp)
                            );
                            ch.writeAndFlush(new BncsPacket(BncsPacketId.SID_CHATEVENT, flagsUpdate));
                        }
                    }
                }
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

    public int getUserCount() {
        return members.size();
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
            if (ch.isActive()) {
                writeCount++;
                logger.debug("  -> Write #{}: to '{}' text='{}'", writeCount, targetUser, text);
                var packet = ChatEventBuilder.build(eid, senderFlags, 0, 0, 0, 0, user, text);
                ch.writeAndFlush(new BncsPacket(BncsPacketId.SID_CHATEVENT, packet));
            } else {
                logger.debug("  -> Skipping '{}' (active=false)", targetUser);
            }
        }
        logger.debug("Broadcast complete: {} total writes", writeCount);
    }

    public void sendSystemMessage(Channel target, String message) {
        if (target.isActive()) {
            var packet = ChatEventBuilder.build(
                ChatEventIds.EID_BROADCAST.getId(), 0, 0, 0, 0, 0, "Battle.net", message
            );
            var output = new BncsPacket(BncsPacketId.SID_CHATEVENT, packet);
            // TODO: ...because we aren't here
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
}
