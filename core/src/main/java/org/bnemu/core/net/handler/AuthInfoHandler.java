package org.bnemu.core.net.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.bnemu.core.net.packet.BncsPacket;
import org.bnemu.core.net.packet.BncsPacketHandler;
import org.bnemu.core.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionManager;

import java.nio.charset.StandardCharsets;
import java.util.Random;

public class AuthInfoHandler implements BncsPacketHandler {
    private static final int LOGON_TYPE = 0; // 0 = Broken SHA-1 (default for StarCraft)
    private static final String MPQ_FILENAME = "ver-IX86-0.mpq";
    private static final String CHECK_REVISION_FORMULA = "A=125933019 B=665814511 C=736475113 4 A=A+S B=B^C C=C^A A=A^B";
    private final SessionManager sessionManager;
    private static final Random RANDOM = new Random();

    public AuthInfoHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public boolean supports(byte packetId) {
        return packetId == BncsPacketId.SID_AUTH_INFO;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        int serverToken = RANDOM.nextInt() & 0x7FFFFFFF; // Generate a positive 32-bit server token
        sessionManager.set(ctx.channel(), "serverToken", String.valueOf(serverToken)); // Store it

        ByteBuf out = Unpooled.buffer();
        out.writeIntLE(LOGON_TYPE);
        out.writeIntLE(serverToken);
        out.writeIntLE(0x02C9); // UDP value (not important, nonzero)
        out.writeIntLE(0); // FILETIME low
        out.writeIntLE(0); // FILETIME high

        out.writeBytes(MPQ_FILENAME.getBytes(StandardCharsets.US_ASCII));
        out.writeByte(0x00);
        out.writeBytes(CHECK_REVISION_FORMULA.getBytes(StandardCharsets.US_ASCII));
        out.writeByte(0x00);

        ctx.writeAndFlush(new BncsPacket(BncsPacketId.SID_AUTH_INFO, out));
    }
}
