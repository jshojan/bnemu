package org.bnemu.persistence.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bnemu.core.dao.AccountDao;
import org.bnemu.core.model.Account;
import org.bnemu.crypto.BrokenSHA1;

import org.bson.Document;
import org.bson.types.Binary;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Arrays;

import static com.mongodb.client.model.Filters.eq;

public class MongoAccountDao implements AccountDao {
    private final MongoCollection<Document> accounts;

    public MongoAccountDao(MongoDatabase db) {
        this.accounts = db.getCollection("accounts");
    }

    @Override
    public boolean createAccount(String username, byte[] passwordHash) {
        String usernameLower = username.toLowerCase();
        if (accounts.find(eq("username", usernameLower)).first() != null) {
            return false;
        }
        Document doc = new Document("username", usernameLower)
                .append("passwordHash", new Binary(passwordHash));
        accounts.insertOne(doc);
        return true;
    }

    @Override
    public Account findAccount(String username) {
        String usernameLower = username.toLowerCase();
        Document doc = accounts.find(eq("username", usernameLower)).first();
        if (doc == null) return null;

        Account acc = new Account();
        acc.setUsername(doc.getString("username"));
        Binary binary = doc.get("passwordHash", Binary.class);
        if (binary != null) {
            acc.setPasswordHashBytes(binary.getData());
        }
        return acc;
    }

    @Override
    public boolean validatePassword(String username, byte[] clientProof, int clientToken, int serverToken) {
        String usernameLower = username.toLowerCase();
        byte[] storedHash = getRawHash(usernameLower);
        if (storedHash == null) {
            return false;
        }

        // Compute expected proof
        ByteBuffer buf = ByteBuffer.allocate(8 + storedHash.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(clientToken);
        buf.putInt(serverToken);
        buf.put(storedHash);

        byte[] input = buf.array();
        int[] hashBuffer = BrokenSHA1.calcHashBuffer(input);

        ByteBuffer out = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN);
        for (int i = 0; i < 5; i++) {
            out.putInt(hashBuffer[i]);
        }
        byte[] expectedProof = out.array();

        return MessageDigest.isEqual(expectedProof, clientProof);
    }

    public byte[] getRawHash(String username) {
        String usernameLower = username.toLowerCase();
        Document doc = accounts.find(eq("username", usernameLower)).first();
        if (doc == null) return null;
        Binary binary = doc.get("passwordHash", Binary.class);
        return binary != null ? binary.getData() : null;
    }
}
