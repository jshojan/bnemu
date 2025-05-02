package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketBuffer;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountLogonHandler extends BncsPacketHandler {

    private static final Logger logger = LoggerFactory.getLogger(AccountLogonHandler.class);
    private final SessionManager sessions;

    public AccountLogonHandler(SessionManager sessions) {
        this.sessions = sessions;
    }

    @Override
    public BncsPacketId bncsPacketId() {
        return BncsPacketId.SID_AUTH_ACCOUNTLOGON;
    }

    // TODO: this is wrong
    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        var username = packet.payload().readString();
        sessions.set(ctx.channel(), "username", username);
        logger.debug("Session created for account '{}', channel: {}", username, ctx.channel().id());
        
        var output = new BncsPacketBuffer()
                .writeByte(0x00);
        send(ctx, output);
    }
}