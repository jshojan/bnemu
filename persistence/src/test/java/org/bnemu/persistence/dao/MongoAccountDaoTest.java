package org.bnemu.persistence.dao;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bnemu.core.model.Account;
import org.junit.jupiter.api.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MongoAccountDaoTest {
    private MongoAccountDao dao;
    private MongoDatabase db;
    private final String username = "testuser123";
    private final String password = "testpass";

    private byte[] passwordHash;

    @BeforeAll
    public void setup() throws Exception {
        // Hash the password EXACTLY as the client would send (NO lowercasing passwords)
        passwordHash = sha1(password.getBytes(StandardCharsets.US_ASCII));

        String uri = "mongodb://root:rootpass@localhost:27017/bnemu";
        MongoClient client = MongoClients.create(uri);
        db = client.getDatabase("bnemu");
        dao = new MongoAccountDao(db);

        db.getCollection("accounts").deleteOne(new Document("username", username.toLowerCase()));
    }

    @AfterEach
    public void cleanup() {
        db.getCollection("accounts").deleteOne(new Document("username", username.toLowerCase()));
    }

    @Test
    public void testCreateAndFindAccount() {
        boolean created = dao.createAccount(username.toLowerCase(), passwordHash);
        assertTrue(created, "Account should be created successfully");

        Account account = dao.findAccount(username.toLowerCase());
        assertNotNull(account, "Account should be found");
        assertEquals(username.toLowerCase(), account.getUsername());

        // Stored hash must match what we originally hashed
        assertArrayEquals(passwordHash, account.getPasswordHashBytes());
    }

    @Test
    public void testFindAccountNotFound() {
        Account account = dao.findAccount("nonexistent_" + new Random().nextInt(10000));
        assertNull(account, "Non-existent account should return null");
    }

    @Test
    public void testGetRawHashNonexistentUser() throws Exception {
        byte[] storedHash = dao.getRawHash("no_user");
        assertNull(storedHash, "No stored hash should be found for nonexistent user");
    }

    @Test
    public void testPasswordProofWrongPassword() throws Exception {
        dao.createAccount(username.toLowerCase(), passwordHash);

        // Now pretend to login with WRONG password
        byte[] wrongPasswordHash = sha1("wrongpass".getBytes(StandardCharsets.US_ASCII));
        int clientToken = 123456;
        int serverToken = 654321;
        byte[] fakeProof = computeXsha1(clientToken, serverToken, wrongPasswordHash);

        byte[] storedHash = dao.getRawHash(username.toLowerCase());
        byte[] expectedProof = computeXsha1(clientToken, serverToken, storedHash);

        assertFalse(MessageDigest.isEqual(expectedProof, fakeProof), "Password proof should NOT match");
    }

    @Test
    public void testPasswordProofSuccess() throws Exception {
        dao.createAccount(username.toLowerCase(), passwordHash);

        int clientToken = 123456;
        int serverToken = 654321;
        byte[] clientProof = computeXsha1(clientToken, serverToken, passwordHash);

        byte[] storedHash = dao.getRawHash(username.toLowerCase());
        byte[] expectedProof = computeXsha1(clientToken, serverToken, storedHash);

        assertTrue(MessageDigest.isEqual(expectedProof, clientProof), "Password proof should match exactly");
    }

    /** Hash password as the client does: simple SHA-1 (ASCII bytes, no lowercase) */
    private byte[] sha1(byte[] input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        return digest.digest(input);
    }

    /** Compute X-SHA1 for login proof */
    private byte[] computeXsha1(int clientToken, int serverToken, byte[] passwordHash) throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(8 + passwordHash.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(clientToken);
        buf.putInt(serverToken);
        buf.put(passwordHash);
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        return digest.digest(buf.array());
    }
}
