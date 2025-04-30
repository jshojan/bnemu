package org.bnemu.core.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

public class ChatMessageTest {
    @Test
    public void testChatMessageProperties() {
        ChatMessage msg = new ChatMessage();
        msg.setChannel("General");
        msg.setSender("user1");
        msg.setText("Hello");
        msg.setTimestamp(Instant.now());

        assertEquals("General", msg.getChannel());
        assertEquals("user1", msg.getSender());
        assertEquals("Hello", msg.getText());
        assertNotNull(msg.getTimestamp());
    }
}