package org.bnemu.core.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GameInfoTest {
    @Test
    public void testGameInfoProperties() {
        GameInfo game = new GameInfo();
        game.setId(42);
        game.setName("HellRun");
        game.setPassword("1234");
        game.setMaxPlayers(8);
        game.setCurrentPlayers(2);
        game.setDifficulty(2);

        assertEquals(42, game.getId());
        assertEquals("HellRun", game.getName());
        assertEquals("1234", game.getPassword());
        assertEquals(8, game.getMaxPlayers());
        assertEquals(2, game.getCurrentPlayers());
        assertEquals(2, game.getDifficulty());
    }
}