package org.bnemu.d2cs.game;

import org.bnemu.core.model.GameInfo;
import org.bnemu.core.model.GameInfo.GameCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory registry of active D2 games. Games are transient and not persisted.
 */
public class GameRegistry {
    private static final Logger logger = LoggerFactory.getLogger(GameRegistry.class);
    private static final int DEFAULT_MAX_PLAYERS = 8;

    private final ConcurrentHashMap<String, GameInfo> gamesByName = new ConcurrentHashMap<>();
    private final AtomicLong nextGameId = new AtomicLong(1);

    public GameInfo createGame(String name, String password, String description,
                               int difficulty, int maxPlayers, String creatorAccount) {
        var game = new GameInfo();
        game.setId(nextGameId.getAndIncrement());
        game.setName(name);
        game.setPassword(password != null ? password : "");
        game.setDescription(description != null ? description : "");
        game.setDifficulty(difficulty);
        game.setMaxPlayers(maxPlayers > 0 ? maxPlayers : DEFAULT_MAX_PLAYERS);
        game.setCurrentPlayers(0);
        game.setCreatorAccount(creatorAccount);
        game.setGameToken(ThreadLocalRandom.current().nextInt());
        game.setGameHash(ThreadLocalRandom.current().nextInt());
        game.setCreatedAt(System.currentTimeMillis());

        gamesByName.put(name.toLowerCase(), game);
        logger.info("Game created: '{}' by '{}' (difficulty={}, maxPlayers={})",
                name, creatorAccount, difficulty, game.getMaxPlayers());
        return game;
    }

    public GameInfo findByName(String name) {
        return gamesByName.get(name.toLowerCase());
    }

    public boolean exists(String name) {
        return gamesByName.containsKey(name.toLowerCase());
    }

    public Collection<GameInfo> listGames(String filter) {
        if (filter == null || filter.isEmpty()) {
            return gamesByName.values();
        }
        String lowerFilter = filter.toLowerCase();
        return gamesByName.values().stream()
                .filter(g -> g.getName().toLowerCase().contains(lowerFilter))
                .collect(Collectors.toList());
    }

    public boolean addCharacter(String gameName, String charName, int charClass, int level) {
        GameInfo game = findByName(gameName);
        if (game == null) {
            return false;
        }
        synchronized (game) {
            if (game.getCurrentPlayers() >= game.getMaxPlayers()) {
                return false;
            }
            game.getCharacters().add(new GameCharacter(charName, charClass, level));
            game.setCurrentPlayers(game.getCurrentPlayers() + 1);
        }
        logger.debug("Character '{}' joined game '{}'", charName, gameName);
        return true;
    }

    public void removeCharacter(String gameName, String charName) {
        GameInfo game = findByName(gameName);
        if (game == null) {
            return;
        }
        synchronized (game) {
            game.getCharacters().removeIf(c -> c.name().equalsIgnoreCase(charName));
            game.setCurrentPlayers(game.getCharacters().size());
            if (game.getCurrentPlayers() == 0) {
                gamesByName.remove(gameName.toLowerCase());
                logger.info("Game '{}' removed (no players remaining)", gameName);
            }
        }
    }

    public void removeGame(String gameName) {
        gamesByName.remove(gameName.toLowerCase());
        logger.info("Game '{}' removed", gameName);
    }
}
