package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketBuffer;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.dao.AccountDao;
import org.bnemu.core.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountLogonProofHandler extends BncsPacketHandler {

    private static final Logger logger = LoggerFactory.getLogger(AccountLogonProofHandler.class);
    private final AccountDao accountDao;
    private final SessionManager sessions;

    public AccountLogonProofHandler(AccountDao accountDao, SessionManager sessions) {
        this.accountDao = accountDao;
        this.sessions = sessions;
    }

    @Override
    public BncsPacketId bncsPacketId() {
        return BncsPacketId.SID_AUTH_ACCOUNTLOGONPROOF;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        var input = packet.payload();
        var clientToken = input.readDword();
        var serverToken = input.readDword();
        var clientHash = input.readBytes(20);

        String username = sessions.get(ctx.channel(), "username");

        boolean verified = false;
        if (username != null) {
            verified = accountDao.validatePassword(username, clientHash, clientToken, serverToken);
        }
        logger.debug("Login attempt for {} verified: {}", username, verified);

        if (verified && username != null) {
            sessions.setUsername(ctx.channel(), username.toLowerCase());
            sessions.markAuthenticated(ctx.channel());
        }

        var output = new BncsPacketBuffer()
            .writeByte(verified ? 0x00 : 0x01);
        send(ctx, output);
    }
}
