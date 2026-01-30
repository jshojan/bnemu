package org.bnemu.core.auth;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * MongoDB-backed store for realm authentication tokens.
 * Shared between BNCS (creates tokens) and D2CS (validates tokens).
 */
public class RealmTokenStore {
    private static final Logger logger = LoggerFactory.getLogger(RealmTokenStore.class);
    private static final String COLLECTION_NAME = "realm_tokens";
    private static final long TOKEN_TTL_SECONDS = 300; // 5 minutes

    private static RealmTokenStore INSTANCE;
    private final MongoCollection<Document> tokens;
    private final Random random = new Random();

    private RealmTokenStore(MongoDatabase db) {
        this.tokens = db.getCollection(COLLECTION_NAME);
        // Create TTL index for automatic expiration
        try {
            tokens.createIndex(
                Indexes.ascending("createdAt"),
                new IndexOptions().expireAfter(TOKEN_TTL_SECONDS, TimeUnit.SECONDS)
            );
        } catch (Exception e) {
            logger.debug("TTL index may already exist: {}", e.getMessage());
        }
    }

    public static synchronized void initialize(MongoDatabase db) {
        if (INSTANCE == null) {
            INSTANCE = new RealmTokenStore(db);
        }
    }

    public static RealmTokenStore getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("RealmTokenStore not initialized. Call initialize() first.");
        }
        return INSTANCE;
    }

    /**
     * Generate a new realm token for the given account.
     */
    public RealmToken createToken(String accountName, int clientToken, int serverToken) {
        int cookie = random.nextInt() & 0x7FFFFFFF;

        Document doc = new Document()
            .append("cookie", cookie)
            .append("accountName", accountName)
            .append("clientToken", clientToken)
            .append("serverToken", serverToken)
            .append("createdAt", new Date());

        tokens.insertOne(doc);
        logger.debug("Created realm token: cookie={}, account={}", cookie, accountName);

        return new RealmToken(cookie, accountName, clientToken, serverToken);
    }

    /**
     * Validate and consume a token. Returns null if invalid or expired.
     */
    public RealmToken validateAndConsume(int cookie) {
        Document doc = tokens.findOneAndDelete(Filters.eq("cookie", cookie));
        if (doc == null) {
            logger.debug("Token not found for cookie: {}", cookie);
            return null;
        }

        String accountName = doc.getString("accountName");
        int clientToken = doc.getInteger("clientToken");
        int serverToken = doc.getInteger("serverToken");
        Date createdAt = doc.getDate("createdAt");

        logger.debug("Validated and consumed token: cookie={}, account={}", cookie, accountName);
        return new RealmToken(cookie, accountName, clientToken, serverToken, createdAt.getTime());
    }

    /**
     * Get token without consuming it (for verification).
     */
    public RealmToken get(int cookie) {
        Document doc = tokens.find(Filters.eq("cookie", cookie)).first();
        if (doc == null) {
            return null;
        }

        String accountName = doc.getString("accountName");
        int clientToken = doc.getInteger("clientToken");
        int serverToken = doc.getInteger("serverToken");
        Date createdAt = doc.getDate("createdAt");

        return new RealmToken(cookie, accountName, clientToken, serverToken, createdAt.getTime());
    }
}
