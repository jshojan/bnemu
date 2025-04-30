package org.bnemu.core.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class D2CharacterTest {
    @Test
    public void testD2CharacterProperties() {
        D2Character d2 = new D2Character();
        d2.setId(1);
        d2.setAccountId(100);
        d2.setName("Necro");
        d2.setCharClass("NECROMANCER");
        d2.setLevel(80);
        d2.setSaveData(new byte[] {1, 2, 3});

        assertEquals(1, d2.getId());
        assertEquals(100, d2.getAccountId());
        assertEquals("Necro", d2.getName());
        assertEquals("NECROMANCER", d2.getCharClass());
        assertEquals(80, d2.getLevel());
        assertArrayEquals(new byte[] {1, 2, 3}, d2.getSaveData());
    }
}