package org.bnemu.core.dao;

import org.bnemu.core.model.D2Character;

import java.util.List;

/**
 * Data access interface for D2 characters.
 */
public interface D2CharacterDao {
    /**
     * Find all characters for an account.
     */
    List<D2Character> findByAccountName(String accountName);

    /**
     * Find a character by account and name.
     */
    D2Character findByAccountAndName(String accountName, String characterName);

    /**
     * Check if a character name is available.
     */
    boolean isNameAvailable(String characterName);

    /**
     * Save a new character.
     */
    void save(D2Character character);

    /**
     * Update an existing character.
     */
    void update(D2Character character);

    /**
     * Delete a character.
     */
    void delete(String accountName, String characterName);
}
