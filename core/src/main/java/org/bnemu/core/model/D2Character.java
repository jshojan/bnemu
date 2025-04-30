package org.bnemu.core.model;

public class D2Character {
    private long id;
    private long accountId;
    private String name;
    private String charClass;
    private int level;
    private byte[] saveData;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getAccountId() { return accountId; }
    public void setAccountId(long accountId) { this.accountId = accountId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCharClass() { return charClass; }
    public void setCharClass(String charClass) { this.charClass = charClass; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public byte[] getSaveData() { return saveData; }
    public void setSaveData(byte[] saveData) { this.saveData = saveData; }
}