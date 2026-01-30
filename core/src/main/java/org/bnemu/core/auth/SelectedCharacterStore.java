package org.bnemu.core.auth;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores selected realm character info for sharing between D2CS and BNCS.
 * Uses MongoDB for cross-process persistence.
 */
public class SelectedCharacterStore {
    private static final Logger logger = LoggerFactory.getLogger(SelectedCharacterStore.class);
    private static final String COLLECTION_NAME = "selected_characters";

    private static SelectedCharacterStore INSTANCE;

    private final MongoCollection<Document> collection;

    private SelectedCharacterStore(MongoDatabase db) {
        this.collection = db.getCollection(COLLECTION_NAME);
    }

    public static synchronized void initialize(MongoDatabase db) {
        if (INSTANCE == null) {
            INSTANCE = new SelectedCharacterStore(db);
        }
    }

    public static SelectedCharacterStore getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("SelectedCharacterStore not initialized. Call initialize() first.");
        }
        return INSTANCE;
    }

    /**
     * Store the selected character for an account.
     */
    public void setSelectedCharacter(String accountName, String realmName, String characterName,
                                     String charClass, int level, boolean expansion,
                                     boolean hardcore, boolean ladder) {
        Document doc = new Document()
            .append("accountName", accountName.toLowerCase())
            .append("realmName", realmName)
            .append("characterName", characterName)
            .append("charClass", charClass)
            .append("level", level)
            .append("expansion", expansion)
            .append("hardcore", hardcore)
            .append("ladder", ladder)
            .append("selectedAt", System.currentTimeMillis());

        collection.replaceOne(
            Filters.eq("accountName", accountName.toLowerCase()),
            doc,
            new ReplaceOptions().upsert(true)
        );

        logger.debug("Stored selected character: account={}, char={}, realm={}",
            accountName, characterName, realmName);
    }

    /**
     * Get the selected character for an account.
     * @return SelectedCharacter or null if none selected
     */
    public SelectedCharacter getSelectedCharacter(String accountName) {
        Document doc = collection.find(Filters.eq("accountName", accountName.toLowerCase())).first();
        if (doc == null) {
            return null;
        }

        // Check if selection is recent (within 5 minutes)
        long selectedAt = doc.getLong("selectedAt");
        if (System.currentTimeMillis() - selectedAt > 5 * 60 * 1000) {
            // Selection expired, remove it
            collection.deleteOne(Filters.eq("accountName", accountName.toLowerCase()));
            return null;
        }

        return new SelectedCharacter(
            doc.getString("accountName"),
            doc.getString("realmName"),
            doc.getString("characterName"),
            doc.getString("charClass"),
            doc.getInteger("level", 1),
            doc.getBoolean("expansion", false),
            doc.getBoolean("hardcore", false),
            doc.getBoolean("ladder", false)
        );
    }

    /**
     * Clear the selected character for an account.
     */
    public void clearSelectedCharacter(String accountName) {
        collection.deleteOne(Filters.eq("accountName", accountName.toLowerCase()));
    }

    /**
     * Record class for selected character info.
     */
    public record SelectedCharacter(
        String accountName,
        String realmName,
        String characterName,
        String charClass,
        int level,
        boolean expansion,
        boolean hardcore,
        boolean ladder
    ) {
        /**
         * Build a D2 statstring for this character.
         * Format is 33 bytes + null terminator as string.
         */
        public String buildStatstring() {
            StringBuilder sb = new StringBuilder();

            // Byte 1-2: Header
            sb.append((char) 0x84);
            sb.append((char) 0x80);

            // Bytes 3-13: Equipment (11 bytes) - 0xFF = none
            for (int i = 0; i < 11; i++) {
                sb.append((char) 0xFF);
            }

            // Byte 14: Character class (1-indexed)
            int classCode = switch (charClass) {
                case "AMAZON" -> 1;
                case "SORCERESS" -> 2;
                case "NECROMANCER" -> 3;
                case "PALADIN" -> 4;
                case "BARBARIAN" -> 5;
                case "DRUID" -> 6;
                case "ASSASSIN" -> 7;
                default -> 1;
            };
            sb.append((char) classCode);

            // Bytes 15-25: Colors (11 bytes) - 0xFF = default
            for (int i = 0; i < 11; i++) {
                sb.append((char) 0xFF);
            }

            // Byte 26: Level
            sb.append((char) Math.max(1, Math.min(99, level)));

            // Byte 27: Flags (0x80 base + 0x04 hardcore + 0x08 dead + 0x20 expansion + 0x40 ladder)
            int flags = 0x80;
            if (hardcore) flags |= 0x04;
            if (expansion) flags |= 0x20;
            if (ladder) flags |= 0x40;
            sb.append((char) flags);

            // Byte 28: Current act (0x80 = Act 1 Normal active)
            sb.append((char) 0x80);

            // Bytes 29-30: Unknown
            sb.append((char) 0xFF);
            sb.append((char) 0xFF);

            // Byte 31: Ladder
            sb.append((char) (ladder ? 0x01 : 0xFF));

            // Bytes 32-33: Unknown
            sb.append((char) 0xFF);
            sb.append((char) 0xFF);

            return sb.toString();
        }
    }
}
