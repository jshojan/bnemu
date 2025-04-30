package org.bnemu.core.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigLoaderTest {
    @Test
    public void testLoadConfig() throws Exception {
        CoreConfig config = ConfigLoader.load("config.yml");

        assertNotNull(config);

        // MongoDB config checks
        assertEquals("localhost", config.getMongo().getHost());
        assertEquals(27017, config.getMongo().getPort());
        assertEquals("bnemu", config.getMongo().getDatabase());

        // Server config checks
        assertEquals(6112, config.getServer().getBncs().getPort());
        assertEquals(6113, config.getServer().getD2cs().getPort());
        assertEquals(6114, config.getServer().getD2dbs().getPort());
        assertEquals(4000, config.getServer().getD2gs().getPort());
        assertEquals(23, config.getServer().getTelnet().getPort());
    }
}
