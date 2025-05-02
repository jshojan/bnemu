package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketBuffer;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.dao.AccountDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class CreateAccountHandler implements BncsPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(CreateAccountHandler.class);
    private final AccountDao accountDao;

    public CreateAccountHandler(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    @Override
    public BncsPacketId bncsPacketId() {
        return BncsPacketId.SID_CREATEACCOUNT;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        var input = packet.payload();
        var username = input.readString();
        var password = input.readString();
        byte[] passwordHash = password.getBytes(StandardCharsets.US_ASCII);

        boolean success = accountDao.createAccount(username, passwordHash);
        logger.debug("Account creation for '{}' success: {}", username, success);

        var output = new BncsPacketBuffer()
                .writeByte(success ? 0x00 : 0x01);
        ctx.writeAndFlush(new BncsPacket(BncsPacketId.SID_CREATEACCOUNT, output));
    }
}
