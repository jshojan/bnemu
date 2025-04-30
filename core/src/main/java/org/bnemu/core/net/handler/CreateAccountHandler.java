package org.bnemu.core.net.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.bnemu.core.dao.AccountDao;
import org.bnemu.core.net.packet.BncsPacket;
import org.bnemu.core.net.packet.BncsPacketHandler;
import org.bnemu.core.net.packet.BncsPacketId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Handles SID_CREATEACCOUNT packets by invoking persistence via AccountDao.
 */
public class CreateAccountHandler implements BncsPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(CreateAccountHandler.class);
    private final AccountDao accountDao;

    // Dependency injection of the AccountDao interface.
    public CreateAccountHandler(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    @Override
    public boolean supports(byte command) {
        return command == BncsPacketId.SID_CREATEACCOUNT;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        ByteBuf buf = packet.getPayload().duplicate();
        String username = readNullTerminatedString(buf);
        if (!buf.isReadable()) {
            // Missing password hash
            logger.warn("Malformed account creation payload: missing password");
            ByteBuf errorBuf = Unpooled.buffer(1).writeByte(0x01);
            ctx.writeAndFlush(new BncsPacket(BncsPacketId.SID_CREATEACCOUNT, errorBuf));
            return;
        }

        String passwordHashStr = readNullTerminatedString(buf);
        byte[] passwordHash = passwordHashStr.getBytes(StandardCharsets.US_ASCII);

        // Use AccountDao to create account
        boolean success = accountDao.createAccount(username, passwordHash);
        logger.debug("Account creation for '{}' success: {}", username, success);

        // 0x00 = success, 0x01 = failure
        ByteBuf responseBuf = Unpooled.buffer(1).writeByte(success ? (byte)0x00 : (byte)0x01);
        ctx.writeAndFlush(new BncsPacket(BncsPacketId.SID_CREATEACCOUNT, responseBuf));
    }

    private String readNullTerminatedString(ByteBuf buf) {
        StringBuilder sb = new StringBuilder();
        while (buf.isReadable()) {
            byte b = buf.readByte();
            if (b == 0x00) {
                break;
            }
            sb.append((char) b);
        }
        return sb.toString();
    }
}
