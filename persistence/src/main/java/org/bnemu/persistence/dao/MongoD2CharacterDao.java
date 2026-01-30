package org.bnemu.persistence.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bnemu.core.dao.D2CharacterDao;
import org.bnemu.core.model.D2Character;
import org.bnemu.core.model.DiabloClass;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * MongoDB implementation of D2CharacterDao.
 */
public class MongoD2CharacterDao implements D2CharacterDao {
    private final MongoCollection<Document> characters;

    public MongoD2CharacterDao(MongoDatabase db) {
        this.characters = db.getCollection("d2_characters");

        // Create unique index on character name (case-insensitive)
        try {
            characters.createIndex(
                Indexes.ascending("nameLower"),
                new IndexOptions().unique(true)
            );
            // Index on account for fast lookups
            characters.createIndex(Indexes.ascending("accountName"));
        } catch (Exception e) {
            // Indexes may already exist
        }
    }

    @Override
    public List<D2Character> findByAccountName(String accountName) {
        List<D2Character> result = new ArrayList<>();
        for (Document doc : characters.find(Filters.eq("accountName", accountName.toLowerCase()))) {
            result.add(documentToCharacter(doc));
        }
        return result;
    }

    @Override
    public D2Character findByAccountAndName(String accountName, String characterName) {
        Document doc = characters.find(Filters.and(
            Filters.eq("accountName", accountName.toLowerCase()),
            Filters.eq("nameLower", characterName.toLowerCase())
        )).first();
        return doc != null ? documentToCharacter(doc) : null;
    }

    @Override
    public boolean isNameAvailable(String characterName) {
        return characters.find(Filters.eq("nameLower", characterName.toLowerCase())).first() == null;
    }

    @Override
    public void save(D2Character character) {
        Document doc = new Document()
            .append("accountName", character.getAccountName().toLowerCase())
            .append("name", character.getName())
            .append("nameLower", character.getName().toLowerCase())
            .append("charClass", character.getCharClass().name())
            .append("level", character.getLevel())
            .append("expansion", character.isExpansion())
            .append("hardcore", character.isHardcore())
            .append("dead", character.isDead())
            .append("ladder", character.isLadder())
            .append("createdAt", new Date(character.getCreatedAt()))
            .append("lastPlayedAt", new Date(character.getLastPlayedAt()));

        if (character.getSaveData() != null) {
            doc.append("saveData", new Binary(character.getSaveData()));
        }

        characters.insertOne(doc);
        character.setId(doc.getObjectId("_id").toString());
    }

    @Override
    public void update(D2Character character) {
        Document update = new Document("$set", new Document()
            .append("level", character.getLevel())
            .append("expansion", character.isExpansion())
            .append("hardcore", character.isHardcore())
            .append("dead", character.isDead())
            .append("ladder", character.isLadder())
            .append("lastPlayedAt", new Date(character.getLastPlayedAt()))
            .append("saveData", character.getSaveData() != null ? new Binary(character.getSaveData()) : null)
        );

        characters.updateOne(
            Filters.eq("nameLower", character.getName().toLowerCase()),
            update
        );
    }

    @Override
    public void delete(String accountName, String characterName) {
        characters.deleteOne(Filters.and(
            Filters.eq("accountName", accountName.toLowerCase()),
            Filters.eq("nameLower", characterName.toLowerCase())
        ));
    }

    private D2Character documentToCharacter(Document doc) {
        D2Character character = new D2Character();
        character.setId(doc.getObjectId("_id").toString());
        character.setAccountName(doc.getString("accountName"));
        character.setName(doc.getString("name"));

        String classStr = doc.getString("charClass");
        if (classStr != null) {
            character.setCharClass(DiabloClass.valueOf(classStr));
        }

        character.setLevel(doc.getInteger("level", 1));
        character.setExpansion(doc.getBoolean("expansion", true));
        character.setHardcore(doc.getBoolean("hardcore", false));
        character.setDead(doc.getBoolean("dead", false));
        character.setLadder(doc.getBoolean("ladder", false));

        Date createdAt = doc.getDate("createdAt");
        if (createdAt != null) {
            character.setCreatedAt(createdAt.getTime());
        }

        Date lastPlayedAt = doc.getDate("lastPlayedAt");
        if (lastPlayedAt != null) {
            character.setLastPlayedAt(lastPlayedAt.getTime());
        }

        Binary saveData = doc.get("saveData", Binary.class);
        if (saveData != null) {
            character.setSaveData(saveData.getData());
        }

        return character;
    }
}
