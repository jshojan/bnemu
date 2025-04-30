package org.bnemu.core.chat;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.core.session.SessionManager;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatChannelManager {

    private final Map<String, ChatChannel> channels = new ConcurrentHashMap<>();
    private final SessionManager sessionManager;

    public ChatChannelManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public ChatChannel getOrCreateChannel(String name) {
        return channels.computeIfAbsent(name, n -> new ChatChannel(n, sessionManager));
    }

    public void joinChannel(String channelName, ChannelHandlerContext ctx, String username) {
        ChatChannel newChannel = getOrCreateChannel(channelName);

        // Leave old channel
        String oldChannel = sessionManager.get(ctx.channel(), "channel");
        if (oldChannel != null && !oldChannel.equals(channelName)) {
            ChatChannel old = channels.get(oldChannel);
            if (old != null) {
                old.removeMember(ctx.channel());
                if (old.getUserCount() == 0) {
                    channels.remove(oldChannel);
                }
            }
        }

        sessionManager.set(ctx.channel(), "channel", channelName);
        newChannel.addMember(ctx.channel(), username);
        //newChannel.broadcastJoinEvents(ctx.channel(), username);
    }

    public void leaveChannel(ChannelHandlerContext ctx) {
        String current = sessionManager.get(ctx.channel(), "channel");
        if (current != null) {
            ChatChannel chan = channels.get(current);
            if (chan != null) {
                chan.removeMember(ctx.channel());
                if (chan.getUserCount() == 0) {
                    channels.remove(current);
                }
            }
            sessionManager.set(ctx.channel(), "channel", null);
        }
    }

    public Set<String> getAllChannels() {
        return Collections.unmodifiableSet(channels.keySet());
    }

    public ChatChannel getChannel(String name) {
        return channels.get(name);
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }
}
