package org.bnemu.bncs.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketHandler;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionManager;

public class AuthCheckHandler implements BncsPacketHandler {
    private final SessionManager sessionManager;

    public AuthCheckHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public boolean supports(byte packetId) {
        return packetId == BncsPacketId.SID_AUTH_CHECK;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        ByteBuf buf = packet.getPayload();

        buf.skipBytes(4); // <--- SKIP the first 4 bytes, they're not the client token!

        int clientToken = buf.readIntLE();
        sessionManager.set(ctx.channel(), "clientToken", String.valueOf(clientToken));
        //System.out.println("[AUTH_CHECK] Client Token: " + clientToken);

        // Skip the rest for now
        buf.skipBytes(buf.readableBytes());

        ByteBuf out = ctx.alloc().buffer();
        out.writeIntLE(0); // success
        out.writeByte(0x00);
        ctx.writeAndFlush(new BncsPacket(BncsPacketId.SID_AUTH_CHECK, out));
    }
}
