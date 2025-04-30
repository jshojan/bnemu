package org.bnemu.core.net.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.bnemu.core.dao.AccountDao;
import org.bnemu.core.net.packet.BncsPacket;
import org.bnemu.core.net.packet.BncsPacketHandler;
import org.bnemu.core.net.packet.BncsPacketId;

import org.bnemu.crypto.BattleNetXSha1;
import org.bnemu.crypto.BrokenSHA1;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class CreateAccount2Handler implements BncsPacketHandler {
    private final AccountDao accountDao;

    public CreateAccount2Handler(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    @Override
    public boolean supports(byte packetId) {
        return packetId == BncsPacketId.SID_CREATEACCOUNT2;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        ByteBuf buf = packet.getPayload();

        byte[] passwordHash = new byte[20];
        buf.readBytes(passwordHash);

        int usernameStartIndex = buf.readerIndex();
        int usernameLength = buf.readableBytes();
        byte[] rawUsernameBytes = new byte[usernameLength];
        buf.getBytes(usernameStartIndex, rawUsernameBytes);
        System.out.println("[CREATE2] Raw incoming username bytes: " + Arrays.toString(rawUsernameBytes));

        String username = readCString(buf);
        String usernameLower = username.toLowerCase();

        String password = "testpass";
        byte[] passwordBytes = password.getBytes(); // Convert string to byte array

        // Get the hashed buffer from BrokenSHA1
        int[] hashBuffer = BrokenSHA1.calcHashBuffer(passwordBytes);

        // Convert the first 5 ints (20 bytes) into a byte array
        ByteBuffer byteBuffer = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < 5; i++) {
            byteBuffer.putInt(hashBuffer[i]);
        }

        byte[] hashedBytes = byteBuffer.array();

        System.out.println("[CREATE2] Incoming username: " + usernameLower);
        System.out.println("[CREATE2] Expected password hash: " + Arrays.toString(hashedBytes));
        System.out.println("[CREATE2] Incoming passwordHash: " + Arrays.toString(passwordHash));

        boolean created = accountDao.createAccount(usernameLower, passwordHash);

        sendResponse(ctx, created ? 0x00 : 0x05, "");
    }

    private void sendResponse(ChannelHandlerContext ctx, int status, String suggestion) {
        ByteBuf out = ctx.alloc().buffer();
        out.writeIntLE(status);
        out.writeBytes(suggestion.getBytes(StandardCharsets.US_ASCII));
        out.writeByte(0x00); // C-string terminator
        ctx.writeAndFlush(new BncsPacket(BncsPacketId.SID_CREATEACCOUNT2, out));
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
}
