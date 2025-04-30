package org.bnemu.core.session;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SessionContextTest {
    @Test
    public void testSessionContextFields() {
        SessionContext session = new SessionContext();
        session.setAccountName("TestUser");
        session.setCurrentChannel("Lobby");

        assertEquals("TestUser", session.getAccountName());
        assertEquals("Lobby", session.getCurrentChannel());
    }
}