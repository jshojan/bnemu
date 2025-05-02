package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketBuffer;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionManager;

public class AuthCheckHandler implements BncsPacketHandler {
    private final SessionManager sessionManager;

    public AuthCheckHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public BncsPacketId bncsPacketId() {
        return BncsPacketId.SID_AUTH_CHECK;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        var input = packet.payload().skipBytes(4);
        var clientToken = input.readDword();

        sessionManager.set(ctx.channel(), "clientToken", String.valueOf(clientToken));

        var output = new BncsPacketBuffer()
                .writeDword(0x00)
                .writeByte(0x00);

        ctx.writeAndFlush(new BncsPacket(BncsPacketId.SID_AUTH_CHECK, output));
    }
}
