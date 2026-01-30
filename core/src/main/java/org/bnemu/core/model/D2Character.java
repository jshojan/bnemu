package org.bnemu.core.model;

/**
 * Diablo 2 character model with all fields needed for MCP protocol.
 */
public class D2Character {
    private String id;
    private String accountName;
    private String name;
    private DiabloClass charClass;
    private int level;
    private boolean expansion;    // LoD character
    private boolean hardcore;
    private boolean dead;
    private boolean ladder;
    private long createdAt;
    private long lastPlayedAt;
    private byte[] saveData;

    public D2Character() {
        this.level = 1;
        this.expansion = true;
        this.hardcore = false;
        this.dead = false;
        this.ladder = false;
        this.createdAt = System.currentTimeMillis();
        this.lastPlayedAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public DiabloClass getCharClass() { return charClass; }
    public void setCharClass(DiabloClass charClass) { this.charClass = charClass; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public boolean isExpansion() { return expansion; }
    public void setExpansion(boolean expansion) { this.expansion = expansion; }

    public boolean isHardcore() { return hardcore; }
    public void setHardcore(boolean hardcore) { this.hardcore = hardcore; }

    public boolean isDead() { return dead; }
    public void setDead(boolean dead) { this.dead = dead; }

    public boolean isLadder() { return ladder; }
    public void setLadder(boolean ladder) { this.ladder = ladder; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getLastPlayedAt() { return lastPlayedAt; }
    public void setLastPlayedAt(long lastPlayedAt) { this.lastPlayedAt = lastPlayedAt; }

    public byte[] getSaveData() { return saveData; }
    public void setSaveData(byte[] saveData) { this.saveData = saveData; }

    /**
     * Build the flags DWORD for MCP protocol.
     * Bit layout:
     * - Bit 2: Hardcore
     * - Bit 3: Dead
     * - Bit 5: Expansion (LoD)
     * - Bit 6: Ladder
     */
    public int getFlags() {
        int flags = 0;
        if (hardcore) flags |= 0x04;   // Bit 2
        if (dead)     flags |= 0x08;   // Bit 3
        if (expansion) flags |= 0x20;  // Bit 5
        if (ladder)   flags |= 0x40;   // Bit 6
        return flags;
    }

    /**
     * Set flags from MCP protocol DWORD.
     */
    public void setFlags(int flags) {
        this.hardcore = (flags & 0x04) != 0;
        this.dead = (flags & 0x08) != 0;
        this.expansion = (flags & 0x20) != 0;
        this.ladder = (flags & 0x40) != 0;
    }
}
