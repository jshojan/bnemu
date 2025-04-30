package org.bnemu.bncs.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.core.dao.AccountDao;
import org.bnemu.bncs.net.packet.BncsPacketHandler;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountLogonProofHandler implements BncsPacketHandler {

    private static final Logger logger = LoggerFactory.getLogger(AccountLogonProofHandler.class);
    private final AccountDao accountDao;
    private final SessionManager sessions;

    public AccountLogonProofHandler(AccountDao accountDao, SessionManager sessions) {
        this.accountDao = accountDao;
        this.sessions = sessions;
    }

    @Override
    public boolean supports(byte command) {
        return command == BncsPacketId.SID_AUTH_ACCOUNTLOGONPROOF;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        ByteBuf buf = packet.getPayload();

        int clientToken = buf.readIntLE();
        int serverToken = buf.readIntLE();
        byte[] clientHash = new byte[20];
        buf.readBytes(clientHash);

        String username = sessions.get(ctx.channel(), "username");

        boolean verified = false;
        if (username != null) {
            verified = accountDao.validatePassword(username, clientHash, clientToken, serverToken);
        }
        logger.debug("Login attempt for {} verified: {}", username, verified);

        ByteBuf response = ctx.alloc().buffer(1);
        response.writeByte(verified ? 0x00 : 0x01);
        ctx.writeAndFlush(new BncsPacket(BncsPacketId.SID_AUTH_ACCOUNTLOGONPROOF, response));
    }

    private String readNullTerminatedString(ByteBuf buf) {
        StringBuilder sb = new StringBuilder();
        byte b;
        while (buf.isReadable() && (b = buf.readByte()) != 0x00) {
            sb.append((char) b);
        }
        return sb.toString();
    }
}
