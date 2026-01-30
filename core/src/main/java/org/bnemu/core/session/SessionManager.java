package org.bnemu.core.session;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class SessionManager {
    private static final AttributeKey<String> USERNAME_KEY = AttributeKey.valueOf("session.username");
    private static final AttributeKey<Boolean> AUTH_KEY = AttributeKey.valueOf("session.authenticated");
    private final Map<String, Channel> userToChannelMap = new ConcurrentHashMap<>();

    public void set(Channel channel, String key, String value) {
        AttributeKey<String> attrKey = AttributeKey.valueOf(key);
        channel.attr(attrKey).set(value);
    }

    public String get(Channel channel, String key) {
        AttributeKey<String> attrKey = AttributeKey.valueOf(key);
        return channel.attr(attrKey).get();
    }

    public boolean has(Channel channel, String key) {
        AttributeKey<String> attrKey = AttributeKey.valueOf(key);
        return channel.hasAttr(attrKey);
    }

    public void setUsername(Channel channel, String username) {
        channel.attr(USERNAME_KEY).set(username);
        userToChannelMap.put(username.toLowerCase(), channel); // case-insensitive
    }

    public Channel getChannelByUsername(String username) {
        // Strip leading '*' — Battle.net convention for account-name lookup
        if (username.startsWith("*")) {
            username = username.substring(1);
        }
        String lower = username.toLowerCase();
        Channel ch = userToChannelMap.get(lower);
        if (ch != null) return ch;

        // Fallback: for D2 users with "CharName*AccountName" display names,
        // match by either the character name or account name part
        for (Map.Entry<String, Channel> entry : userToChannelMap.entrySet()) {
            String key = entry.getKey();
            int star = key.indexOf('*');
            if (star >= 0) {
                String charPart = key.substring(0, star);
                String acctPart = key.substring(star + 1);
                if (charPart.equals(lower) || acctPart.equals(lower)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    public String getUsername(Channel channel) {
        return channel.attr(USERNAME_KEY).get();
    }

    public boolean hasUsername(Channel channel) {
        return channel.hasAttr(USERNAME_KEY);
    }

    public void markAuthenticated(Channel channel) {
        channel.attr(AUTH_KEY).set(true);
    }

    public boolean isAuthenticated(Channel channel) {
        Boolean val = channel.attr(AUTH_KEY).get();
        return val != null && val;
    }

    public void clear(Channel channel) {
        String username = getUsername(channel);
        if (username != null) {
            userToChannelMap.remove(username.toLowerCase());
        }
        channel.attr(USERNAME_KEY).set(null);
        channel.attr(AUTH_KEY).set(null);
    }
}