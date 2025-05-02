package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketBuffer;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionManager;

import java.util.Random;

public class AuthInfoHandler extends BncsPacketHandler {
    private static final int LOGON_TYPE = 0; // 0 = Broken SHA-1 (default for StarCraft)
    private static final String MPQ_FILENAME = "ver-IX86-0.mpq";
    private static final String CHECK_REVISION_FORMULA = "A=125933019 B=665814511 C=736475113 4 A=A+S B=B^C C=C^A A=A^B";
    private static final Random RANDOM = new Random();
    private final SessionManager sessionManager;

    public AuthInfoHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public BncsPacketId bncsPacketId() {
        return BncsPacketId.SID_AUTH_INFO;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        // TODO: this should increment from 1 for every login to the server
        int serverToken = RANDOM.nextInt() & 0x7FFFFFFF;
        sessionManager.set(ctx.channel(), "serverToken", String.valueOf(serverToken));

        var output = new BncsPacketBuffer()
            .writeDword(LOGON_TYPE)
            .writeDword(serverToken)
            .writeDword(0x02C9)
            .writeDword(0x00)
            .writeDword(0x00)
            .writeString(MPQ_FILENAME)
            .writeString(CHECK_REVISION_FORMULA);
        send(ctx, output);
    }
}
