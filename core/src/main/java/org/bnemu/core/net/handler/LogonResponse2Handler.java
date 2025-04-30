package org.bnemu.core.net.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.bnemu.core.dao.AccountDao;
import org.bnemu.core.model.Account;
import org.bnemu.core.net.packet.BncsPacket;
import org.bnemu.core.net.packet.BncsPacketHandler;
import org.bnemu.core.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionManager;
import org.bnemu.crypto.BrokenSHA1;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Arrays;

public class LogonResponse2Handler implements BncsPacketHandler {
    private final AccountDao accountDao;
    private final SessionManager sessionManager;

    public LogonResponse2Handler(AccountDao accountDao, SessionManager sessionManager) {
        this.accountDao = accountDao;
        this.sessionManager = sessionManager;
    }

    @Override
    public boolean supports(byte packetId) {
        return packetId == BncsPacketId.SID_LOGONRESPONSE2;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        ByteBuf buf = packet.getPayload();

        int clientToken = buf.readIntLE();
        int serverToken = buf.readIntLE(); // Still read it for compatibility (ignore after)

        byte[] clientProof = new byte[20];
        buf.readBytes(clientProof);

        int usernameStartIndex = buf.readerIndex();
        int usernameLength = buf.readableBytes();
        byte[] rawUsernameBytes = new byte[usernameLength];
        buf.getBytes(usernameStartIndex, rawUsernameBytes);
        System.out.println("[LOGIN] Raw username bytes: " + Arrays.toString(rawUsernameBytes));

        String username = readCString(buf);
        String usernameLower = username.toLowerCase();

        int statusCode;
        Account account = accountDao.findAccount(usernameLower);
        if (account == null) {
            statusCode = 0x01; // Account does not exist
        } else {
            byte[] storedHash = account.getPasswordHashBytes();

            // Force serverToken to 0 for proof calculation
            byte[] expectedProof = computeProof(clientToken, serverToken, storedHash);

            System.out.println("[LOGIN] Stored passwordHash: " + Arrays.toString(storedHash));
            System.out.println("[LOGIN] ClientProof from StealthBot: " + Arrays.toString(clientProof));
            System.out.println("[LOGIN] ExpectedProof computed: " + Arrays.toString(expectedProof));
            System.out.println("[LOGIN] ClientToken used: " + clientToken);
            System.out.println("[LOGIN] (IGNORING) ServerToken sent by client: " + serverToken);

            if (MessageDigest.isEqual(expectedProof, clientProof)) {
                statusCode = 0x00; // Success
                sessionManager.setUsername(ctx.channel(), usernameLower);
                sessionManager.markAuthenticated(ctx.channel());
            } else {
                statusCode = 0x02; // Invalid password
            }
        }

        sendLoginResponse(ctx, statusCode);
    }


    private void sendLoginResponse(ChannelHandlerContext ctx, int statusCode) {
        ByteBuf out = ctx.alloc().buffer(4);
        out.writeIntLE(statusCode);
        ctx.writeAndFlush(new BncsPacket(BncsPacketId.SID_LOGONRESPONSE2, out));
    }

    private String readCString(ByteBuf buf) {
        StringBuilder sb = new StringBuilder();
        while (buf.isReadable()) {
            byte b = buf.readByte();
            if (b == 0x00) break;
            if (b >= 32 && b <= 126) {
                sb.append((char) b);
            }
        }
        return sb.toString();
    }

    private byte[] computeProof(int clientToken, int serverToken, byte[] passwordHash) {
        ByteBuffer buf = ByteBuffer.allocate(4 + 4 + 1 + passwordHash.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(clientToken);
        buf.putInt(serverToken);
        buf.put(passwordHash);
        buf.put((byte)0);

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
