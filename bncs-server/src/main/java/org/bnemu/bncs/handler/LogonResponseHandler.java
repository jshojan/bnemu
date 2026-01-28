package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketBuffer;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.dao.AccountDao;
import org.bnemu.core.model.Account;
import org.bnemu.core.session.SessionManager;
import org.bnemu.crypto.BrokenSHA1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * Handles SID_LOGONRESPONSE (0x29) for older games like Warcraft 2 BNE.
 * This is the old login packet.
 *
 * Result codes for SID_LOGONRESPONSE differ from SID_LOGONRESPONSE2:
 * - 0 = Invalid password
 * - 1 = Success
 * - 2 = Account does not exist
 * - 6 = Account closed
 */
public class LogonResponseHandler extends BncsPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(LogonResponseHandler.class);

    // Result codes for SID_LOGONRESPONSE (different from SID_LOGONRESPONSE2!)
    private static final int RESULT_SUCCESS = 1;
    private static final int RESULT_INVALID_PASSWORD = 0;
    private static final int RESULT_ACCOUNT_NOT_FOUND = 2;

    private final AccountDao accountDao;
    private final SessionManager sessionManager;

    public LogonResponseHandler(AccountDao accountDao, SessionManager sessionManager) {
        this.accountDao = accountDao;
        this.sessionManager = sessionManager;
    }

    @Override
    public BncsPacketId bncsPacketId() {
        return BncsPacketId.SID_LOGONRESPONSE;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        var input = packet.payload();

        // Parse C->S format per BNetDocs:
        // (DWORD) Client Token
        // (DWORD) Server Token
        // (DWORD[5]) Password Hash (20 bytes)
        // (STRING) Username
        int clientToken = input.readDword();
        int serverToken = input.readDword();
        byte[] clientProof = input.readBytes(20);
        String username = input.readString();

        String usernameLower = username.toLowerCase();

        int statusCode;
        Account account = accountDao.findAccount(usernameLower);
        if (account == null) {
            logger.debug("Login failed for '{}': account not found", username);
            statusCode = RESULT_ACCOUNT_NOT_FOUND;
        } else {
            byte[] storedHash = account.getPasswordHashBytes();
            byte[] expectedProof = computeProof(clientToken, serverToken, storedHash);

            logger.debug("Login attempt for '{}': clientToken={}, serverToken={}", username, clientToken, serverToken);
            logger.trace("Stored passwordHash: {}", Arrays.toString(storedHash));
            logger.trace("ClientProof received: {}", Arrays.toString(clientProof));
            logger.trace("ExpectedProof computed: {}", Arrays.toString(expectedProof));

            if (MessageDigest.isEqual(expectedProof, clientProof)) {
                logger.debug("Login successful for '{}'", username);
                statusCode = RESULT_SUCCESS;
                sessionManager.setUsername(ctx.channel(), usernameLower);
                sessionManager.markAuthenticated(ctx.channel());
            } else {
                logger.debug("Login failed for '{}': invalid password", username);
                statusCode = RESULT_INVALID_PASSWORD;
            }
        }

        var output = new BncsPacketBuffer().writeDword(statusCode);
        send(ctx, output);
    }

    private byte[] computeProof(int clientToken, int serverToken, byte[] passwordHash) {
        ByteBuffer buf = ByteBuffer.allocate(4 + 4 + 1 + passwordHash.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(clientToken);
        buf.putInt(serverToken);
        buf.put(passwordHash);
        buf.put((byte) 0);

        byte[] inputArray = buf.array();
        logger.trace("Hashing input bytes: {}", Arrays.toString(inputArray));

        int[] hashBuffer = BrokenSHA1.calcHashBuffer(inputArray);

        ByteBuffer out = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < 5; i++) {
            out.putInt(hashBuffer[i]);
        }

        return out.array();
    }
}
