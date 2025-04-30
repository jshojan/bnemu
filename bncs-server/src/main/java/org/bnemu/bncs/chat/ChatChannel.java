package org.bnemu.bncs.chat;

import io.netty.channel.Channel;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.core.session.SessionManager;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatChannel {
    private final String name;
    private final Set<Channel> members = ConcurrentHashMap.newKeySet();
    private final SessionManager sessionManager;

    public ChatChannel(String name, SessionManager sessionManager) {
        this.name = name;
        this.sessionManager = sessionManager;
    }

    public void addMember(Channel newChannel, String username) {
        // Send EID_SHOWUSER for each existing user to the new user
        for (Channel existing : members) {
            if (!existing.equals(newChannel) && existing.isActive()) {
                String otherUser = sessionManager.get(existing, "username");
                if (otherUser != null) {
                    BncsPacket showUser = ChatEventBuilder.build(
                            ChatEventIds.EID_SHOWUSER, 0, 0, 0, 0, 0, otherUser, null
                    );
                    newChannel.writeAndFlush(showUser);
                }
            }
        }

        // Notify existing users that the new user joined (EID_JOIN)
        for (Channel ch : members) {
            if (!ch.equals(newChannel) && ch.isActive()) {
                BncsPacket joinNotify = ChatEventBuilder.build(
                        ChatEventIds.EID_JOIN, 0, 0, 0, 0, 0, username, null
                );
                ch.writeAndFlush(joinNotify);
            }
        }

        // Add the new member AFTER notifying
        members.add(newChannel);
        sessionManager.set(newChannel, "channel", name);
        sessionManager.set(newChannel, "username", username);

        // Send EID_SHOWUSER to the new user for themselves
        BncsPacket showSelf = ChatEventBuilder.build(
                ChatEventIds.EID_SHOWUSER, 0, 0, 0, 0, 0, username, null
        );
        newChannel.writeAndFlush(showSelf);

        // Send EID_CHANNEL message
        BncsPacket channelInfo = ChatEventBuilder.build(
                ChatEventIds.EID_CHANNEL, 0, 0, 0, 0, 0, username, name
        );
        newChannel.writeAndFlush(channelInfo);

        // Welcome message
        sendSystemMessage(newChannel, "Welcome to " + name + "\nEnjoy chatting!");
    }

    public void removeMember(Channel channel) {
        members.remove(channel);
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
        for (Channel ch : members) {
            if (ch.isActive()) {
                int flags = 0;
                int ping = 0;
                int ip = 0x00000000;
                int acc = 0x0BADF00D;
                int reg = 0x0BADF00D;
                BncsPacket packet = ChatEventBuilder.build(eid, flags, ping, ip, acc, reg, user, text);
                ch.writeAndFlush(packet.retainedDuplicate());
            }
        }
    }

    public void sendSystemMessage(Channel target, String message) {
        if (target.isActive()) {
            BncsPacket packet = ChatEventBuilder.build(
                    ChatEventIds.EID_BROADCAST, 0, 0, 0, 0, 0, "Battle.net", message
            );
            target.writeAndFlush(packet);
        }
    }

    public void sendInfoMessage(Channel target, String message) {
        if (target.isActive()) {
            String username = sessionManager.get(target, "username");
            if (username == null) username = "Battle.net";
            BncsPacket packet = ChatEventBuilder.build(
                    ChatEventIds.EID_INFO, 0, 0, 0, 0, 0, username, message
            );
            target.writeAndFlush(packet);
        }
    }
}
