package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketBuffer;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles SID_STARTVERSIONING (0x06) for older games like Warcraft 2 BNE.
 * This is the old version check request packet.
 */
public class StartVersioningHandler extends BncsPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(StartVersioningHandler.class);

    // Same values used by AuthInfoHandler for newer clients
    private static final String MPQ_FILENAME = "ver-IX86-0.mpq";
    private static final String CHECK_REVISION_FORMULA = "A=125933019 B=665814511 C=736475113 4 A=A+S B=B^C C=C^A A=A^B";

    private final SessionManager sessionManager;

    public StartVersioningHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public BncsPacketId bncsPacketId() {
        return BncsPacketId.SID_STARTVERSIONING;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        var input = packet.payload();

        // Parse C->S format per BNetDocs:
        // (DWORD) Platform ID ("68XI" = IX86)
        // (DWORD) Product ID ("NB2W" = W2BN)
        // (DWORD) Version Byte
        // (DWORD) Unknown (0)
        int platformId = input.readDword();
        int productId = input.readDword();
        int versionByte = input.readDword();
        int unknown = input.readDword();

        // Store product code (bytes are in reverse order)
        String productCode = new String(new byte[] {
            (byte) (productId & 0xFF),
            (byte) ((productId >> 8) & 0xFF),
            (byte) ((productId >> 16) & 0xFF),
            (byte) ((productId >> 24) & 0xFF)
        });
        sessionManager.set(ctx.channel(), "product", productCode);

        logger.debug("SID_STARTVERSIONING: platform=0x{}, product='{}', versionByte={}, unknown={}",
            Integer.toHexString(platformId), productCode, versionByte, unknown);

        // S->C response per BNetDocs:
        // (FILETIME) MPQ Filetime - 8 bytes, can be 0
        // (STRING) MPQ Filename
        // (STRING) ValueString (formula)
        var output = new BncsPacketBuffer()
            .writeDword(0) // FILETIME low
            .writeDword(0) // FILETIME high
            .writeString(MPQ_FILENAME)
            .writeString(CHECK_REVISION_FORMULA);
        send(ctx, output);
    }
}
