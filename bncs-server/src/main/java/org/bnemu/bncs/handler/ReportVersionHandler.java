package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketBuffer;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles SID_REPORTVERSION (0x07) for older games like Warcraft 2 BNE.
 * This is the old version check result packet from the client.
 */
public class ReportVersionHandler extends BncsPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReportVersionHandler.class);

    // Result codes per BNetDocs
    private static final int RESULT_SUCCESS = 2;

    @Override
    public BncsPacketId bncsPacketId() {
        return BncsPacketId.SID_REPORTVERSION;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        var input = packet.payload();

        // Parse C->S format per BNetDocs:
        // (DWORD) Platform ID
        // (DWORD) Product ID
        // (DWORD) Version Byte
        // (DWORD) EXE Version
        // (DWORD) EXE Hash
        // (STRING) EXE Info
        int platformId = input.readDword();
        int productId = input.readDword();
        int versionByte = input.readDword();
        int exeVersion = input.readDword();
        int exeHash = input.readDword();
        String exeInfo = input.readString();

        String productCode = new String(new byte[] {
            (byte) (productId & 0xFF),
            (byte) ((productId >> 8) & 0xFF),
            (byte) ((productId >> 16) & 0xFF),
            (byte) ((productId >> 24) & 0xFF)
        });

        logger.debug("SID_REPORTVERSION: product='{}', versionByte={}, exeVersion=0x{}, exeHash=0x{}, exeInfo='{}'",
            productCode, versionByte, Integer.toHexString(exeVersion), Integer.toHexString(exeHash), exeInfo);

        // S->C response per BNetDocs:
        // (DWORD) Result
        // Result codes: 0 = Old game version not allowed, 1 = Invalid version,
        //               2 = Success, 3 = Reinstall required
        var output = new BncsPacketBuffer()
            .writeDword(RESULT_SUCCESS);
        send(ctx, output);
    }
}
