package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketBuffer;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.dao.AccountDao;

public class CreateAccount2Handler implements BncsPacketHandler {
    private final AccountDao accountDao;

    public CreateAccount2Handler(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    @Override
    public BncsPacketId bncsPacketId() {
        return BncsPacketId.SID_CREATEACCOUNT2;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        var input = packet.payload();
        var passwordHash = input.readBytes(20);
        var username = input.readString();

        String usernameLower = username.toLowerCase();
        boolean created = accountDao.createAccount(usernameLower, passwordHash);

        sendResponse(ctx, created ? 0x00 : 0x05, "");
    }

    private void sendResponse(ChannelHandlerContext ctx, int status, String suggestion) {
        var output = new BncsPacketBuffer()
                .writeDword(status)
                .writeString(suggestion);
        ctx.writeAndFlush(new BncsPacket(BncsPacketId.SID_CREATEACCOUNT2, output));
    }
}
