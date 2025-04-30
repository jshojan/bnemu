package org.bnemu.core.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class AccountTest {

    @Test
    public void testAccountProperties() {
        Account acc = new Account();
        acc.setId("507f1f77bcf86cd799439011"); // Example ObjectId in hex string
        acc.setUsername("jeff");
        acc.setPasswordHash("hashed");
        acc.setPasswordHashBytes(new byte[] {1, 2, 3});
        acc.setEmail("test@example.com");
        acc.setCreateDate(Instant.now());

        assertEquals("507f1f77bcf86cd799439011", acc.getId());
        assertEquals("jeff", acc.getUsername());
        assertEquals("hashed", acc.getPasswordHash());
        assertTrue(Arrays.equals(new byte[] {1, 2, 3}, acc.getPasswordHashBytes()));
        assertEquals("test@example.com", acc.getEmail());
        assertNotNull(acc.getCreateDate());
    }
}
