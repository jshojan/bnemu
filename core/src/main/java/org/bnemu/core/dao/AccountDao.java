package org.bnemu.core.dao;

import org.bnemu.core.model.Account;

/**
 * DAO interface for account persistence operations.
 */
public interface AccountDao {
    /**
     * Retrieves an account by username.
     * Returns null if account not found.
     */
    Account findAccount(String username);

    /**
     * Creates a new account with given username and password hash (from client).
     * Returns true if creation succeeded.
     */
    boolean createAccount(String username, byte[] passwordHash);

    /**
     * Validates a logon attempt using Battle.net XSha1 proof.
     * @return true if password hash matches expected proof.
     */
    boolean validatePassword(String username, byte[] clientProof, int clientToken, int serverToken);
}
