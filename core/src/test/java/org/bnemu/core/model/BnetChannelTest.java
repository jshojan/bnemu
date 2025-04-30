package org.bnemu.core.model;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class BnetChannelTest {
    @Test
    public void testBnetChannelProperties() {
        BnetChannel channel = new BnetChannel();
        channel.setName("General");
        channel.setUsers(Set.of("user1", "user2"));
        channel.setTopic("Welcome!");
        channel.setPrivate(true);

        assertEquals("General", channel.getName());
        assertTrue(channel.getUsers().contains("user1"));
        assertEquals("Welcome!", channel.getTopic());
        assertTrue(channel.isPrivate());
    }
}