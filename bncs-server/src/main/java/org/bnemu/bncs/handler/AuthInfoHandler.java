package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketBuffer;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionManager;

import org.bnemu.bnftp.BnftpFileProvider;
import java.util.Random;

public class AuthInfoHandler extends BncsPacketHandler {
    private static final int LOGON_TYPE = 0; // 0 = Broken SHA-1 (default for StarCraft/D2)
    private static final String MPQ_FILENAME = "ver-IX86-1.mpq";
    private static final String CHECKREVISION_FORMULA =
        "A=3845581634 B=880823580 C=1363937103 4 A=A-S B=B-C C=C-A A=A-B";
    private static final Random RANDOM = new Random();
    private final SessionManager sessionManager;
    private final BnftpFileProvider bnftpFileProvider;

    public AuthInfoHandler(SessionManager sessionManager, BnftpFileProvider bnftpFileProvider) {
        this.sessionManager = sessionManager;
        this.bnftpFileProvider = bnftpFileProvider;
    }

    @Override
    public BncsPacketId bncsPacketId() {
        return BncsPacketId.SID_AUTH_INFO;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        var input = packet.payload();

        // Parse client info per BNetDocs SID_AUTH_INFO C->S format
        int protocolId = input.readDword();      // Protocol ID (0)
        int platformId = input.readDword();      // Platform ID ("68XI" = IX86)
        int productId = input.readDword();       // Product ID ("RATS" = STAR, "PXES" = SEXP)

        // Store product code as a 4-character string (bytes are reversed)
        String productCode = new String(new byte[] {
            (byte) (productId & 0xFF),
            (byte) ((productId >> 8) & 0xFF),
            (byte) ((productId >> 16) & 0xFF),
            (byte) ((productId >> 24) & 0xFF)
        });
        sessionManager.set(ctx.channel(), "product", productCode);

        // TODO: this should increment from 1 for every login to the server
        int serverToken = RANDOM.nextInt() & 0x7FFFFFFF;
        sessionManager.set(ctx.channel(), "serverToken", String.valueOf(serverToken));

        // Get real FILETIME from the MPQ file on disk (client hangs/crashes if this is zero)
        long filetime = bnftpFileProvider.getFiletime(MPQ_FILENAME);
        int filetimeLow = (int) (filetime & 0xFFFFFFFFL);
        int filetimeHigh = (int) (filetime >>> 32);

        var output = new BncsPacketBuffer()
            .writeDword(LOGON_TYPE)
            .writeDword(serverToken)
            .writeDword(0x02C9)
            .writeDword(filetimeLow)
            .writeDword(filetimeHigh)
            .writeString(MPQ_FILENAME)
            .writeString(CHECKREVISION_FORMULA);
        send(ctx, output);

        // Send SID_PING for login-time latency measurement
        // Client echoes the cookie back; PingHandler records RTT on first response
        int pingCookie = (int) System.currentTimeMillis();
        var pingPayload = new BncsPacketBuffer().writeDword(pingCookie);
        ctx.writeAndFlush(new BncsPacket(BncsPacketId.SID_PING, pingPayload));
    }
}
