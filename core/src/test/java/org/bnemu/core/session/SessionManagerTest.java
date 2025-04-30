package org.bnemu.core.session;

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SessionManagerTest {
    private SessionManager sessionManager;
    private EmbeddedChannel channel;

    @BeforeEach
    public void setUp() {
        sessionManager = new SessionManager();
        channel = new EmbeddedChannel();
    }

    @Test
    public void testSetAndGetUsername() {
        sessionManager.setUsername(channel, "testuser");
        assertEquals("testuser", sessionManager.getUsername(channel));
        assertTrue(sessionManager.hasUsername(channel));
    }

    @Test
    public void testAuthenticationFlow() {
        assertFalse(sessionManager.isAuthenticated(channel));
        sessionManager.markAuthenticated(channel);
        assertTrue(sessionManager.isAuthenticated(channel));
    }

    @Test
    public void testClearSession() {
        sessionManager.setUsername(channel, "testuser");
        sessionManager.markAuthenticated(channel);
        sessionManager.clear(channel);

        assertNull(sessionManager.getUsername(channel));
        assertFalse(sessionManager.isAuthenticated(channel));
    }

    @Test
    public void testSetGenericKey() {
        sessionManager.set(channel, "custom.key", "customValue");
        assertEquals("customValue", sessionManager.get(channel, "custom.key"));
        assertTrue(sessionManager.has(channel, "custom.key"));
    }
}