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

public class LogonResponse2Handler extends BncsPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(LogonResponse2Handler.class);

    private final AccountDao accountDao;
    private final SessionManager sessionManager;

    public LogonResponse2Handler(AccountDao accountDao, SessionManager sessionManager) {
        this.accountDao = accountDao;
        this.sessionManager = sessionManager;
    }

    @Override
    public BncsPacketId bncsPacketId() {
        return BncsPacketId.SID_LOGONRESPONSE2;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        var input = packet.payload();
        var clientToken = input.readDword();
        var serverToken = input.readDword();
        var clientProof = input.readBytes(20);
        var username = input.readString();
 
        String usernameLower = username.toLowerCase();

        int statusCode;
        Account account = accountDao.findAccount(usernameLower);
        if (account == null) {
            statusCode = 0x01;
            logger.info("Login failed for '{}': account not found", username);
        } else {
            byte[] storedHash = account.getPasswordHashBytes();
            byte[] expectedProof = computeProof(clientToken, serverToken, storedHash);

            logger.debug("Login attempt for '{}': clientToken=0x{}, serverToken=0x{}",
                username, Integer.toHexString(clientToken), Integer.toHexString(serverToken));
            logger.debug("Stored hash:    {}", Arrays.toString(storedHash));
            logger.debug("Client proof:   {}", Arrays.toString(clientProof));
            logger.debug("Expected proof: {}", Arrays.toString(expectedProof));

            if (MessageDigest.isEqual(expectedProof, clientProof)) {
                statusCode = 0x00;
                sessionManager.setUsername(ctx.channel(), usernameLower);
                sessionManager.markAuthenticated(ctx.channel());
                logger.info("Login successful for '{}'", username);
            } else {
                statusCode = 0x02;
                logger.info("Login failed for '{}': invalid password (proof mismatch)", username);
            }
        }

        sendLoginResponse(ctx, statusCode);
    }

    private void sendLoginResponse(ChannelHandlerContext ctx, int statusCode) {
        var output = new BncsPacketBuffer().writeDword(statusCode);
        ctx.writeAndFlush(new BncsPacket(BncsPacketId.SID_LOGONRESPONSE2, output));
    }

    private byte[] computeProof(int clientToken, int serverToken, byte[] passwordHash) {
        ByteBuffer buf = ByteBuffer.allocate(4 + 4 + passwordHash.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(clientToken);
        buf.putInt(serverToken);
        buf.put(passwordHash);

        byte[] inputArray = buf.array();
        logger.trace("Hashing input bytes: {}", Arrays.toString(inputArray));

        int[] hashBuffer = BrokenSHA1.calcHashBuffer(inputArray);

        ByteBuffer out = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN); // .order(ByteOrder.BIG_ENDIAN);
        for (int i = 0; i < 5; i++) {
            out.putInt(hashBuffer[i]);
        }

        byte[] result = out.array();
        logger.trace("Result hash buffer: {}", Arrays.toString(result));

        return result;
    }
}
