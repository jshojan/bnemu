package org.bnemu.bncs.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketHandler;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountLogonHandler implements BncsPacketHandler {

    private static final Logger logger = LoggerFactory.getLogger(AccountLogonHandler.class);
    private final SessionManager sessions;

    public AccountLogonHandler(SessionManager sessions) {
        this.sessions = sessions;
    }

    @Override
    public boolean supports(byte packetId) {
        return packetId == BncsPacketId.SID_AUTH_ACCOUNTLOGON;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        ByteBuf buf = packet.getPayload();

        String username = readNullTerminatedString(buf);
        sessions.set(ctx.channel(), "username", username);

        logger.debug("Session created for account '{}', channel: {}", username, ctx.channel().id());

        // Stubbed success response (normally includes hashed proof)
        ctx.writeAndFlush(new BncsPacket(BncsPacketId.SID_AUTH_ACCOUNTLOGONPROOF, ctx.alloc().buffer(1).writeByte(0x00)));
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