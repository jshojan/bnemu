package org.bnemu.core.model;

import java.util.ArrayList;
import java.util.List;

public class GameInfo {
    private String name;
    private String password;
    private String description;
    private int maxPlayers;
    private int currentPlayers;
    private int difficulty;
    private long id;
    private int gameToken;
    private int gameHash;
    private long createdAt;
    private String creatorAccount;
    private List<GameCharacter> characters = new ArrayList<>();

    public record GameCharacter(String name, int charClass, int level) {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }

    public int getCurrentPlayers() { return currentPlayers; }
    public void setCurrentPlayers(int currentPlayers) { this.currentPlayers = currentPlayers; }

    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getGameToken() { return gameToken; }
    public void setGameToken(int gameToken) { this.gameToken = gameToken; }

    public int getGameHash() { return gameHash; }
    public void setGameHash(int gameHash) { this.gameHash = gameHash; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getCreatorAccount() { return creatorAccount; }
    public void setCreatorAccount(String creatorAccount) { this.creatorAccount = creatorAccount; }

    public List<GameCharacter> getCharacters() { return characters; }
    public void setCharacters(List<GameCharacter> characters) { this.characters = characters; }
}
