package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketBuffer;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.dao.AccountDao;
import org.bnemu.core.model.Account;
import org.bnemu.core.session.SessionManager;
import org.bnemu.crypto.BrokenSHA1;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Arrays;

public class LogonResponse2Handler extends BncsPacketHandler {
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
        } else {
            byte[] storedHash = account.getPasswordHashBytes();
            byte[] expectedProof = computeProof(clientToken, serverToken, storedHash);

            System.out.println("[LOGIN] Stored passwordHash: " + Arrays.toString(storedHash));
            System.out.println("[LOGIN] ClientProof from StealthBot: " + Arrays.toString(clientProof));
            System.out.println("[LOGIN] ExpectedProof computed: " + Arrays.toString(expectedProof));
            System.out.println("[LOGIN] ClientToken used: " + clientToken);
            System.out.println("[LOGIN] (IGNORING) ServerToken sent by client: " + serverToken);

            if (MessageDigest.isEqual(expectedProof, clientProof)) {
                statusCode = 0x00;
                sessionManager.setUsername(ctx.channel(), usernameLower);
                sessionManager.markAuthenticated(ctx.channel());
            } else {
                statusCode = 0x02;
            }
        }

        sendLoginResponse(ctx, statusCode);
    }

    private void sendLoginResponse(ChannelHandlerContext ctx, int statusCode) {
        var output = new BncsPacketBuffer().writeDword(statusCode);
        ctx.writeAndFlush(new BncsPacket(BncsPacketId.SID_LOGONRESPONSE2, output));
    }

    private byte[] computeProof(int clientToken, int serverToken, byte[] passwordHash) {
        ByteBuffer buf = ByteBuffer.allocate(4 + 4 + 1 + passwordHash.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(clientToken);
        buf.putInt(serverToken);
        buf.put(passwordHash);
        buf.put((byte) 0);

        byte[] inputArray = buf.array();
        System.out.println("[DEBUG] Hashing input bytes: " + Arrays.toString(inputArray));

        int[] hashBuffer = BrokenSHA1.calcHashBuffer(inputArray);

        ByteBuffer out = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN); // .order(ByteOrder.BIG_ENDIAN);
        for (int i = 0; i < 5; i++) {
            out.putInt(hashBuffer[i]);
        }

        byte[] result = out.array();
        System.out.println("[DEBUG] Result hash buffer: " + Arrays.toString(result));

        return result;
    }
}
