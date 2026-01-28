package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles SID_LOCALEINFO (0x12) for older games like Warcraft 2 BNE.
 * This packet provides locale information from the client.
 * No server response is required.
 */
public class LocaleInfoHandler extends BncsPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(LocaleInfoHandler.class);

    @Override
    public BncsPacketId bncsPacketId() {
        return BncsPacketId.SID_LOCALEINFO;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        var input = packet.payload();

        // Parse C->S format per BNetDocs:
        // (FILETIME) System time - 8 bytes
        // (FILETIME) Local time - 8 bytes
        // (DWORD) Timezone bias
        // (DWORD) System LCID
        // (DWORD) User LCID
        // (DWORD) User LangID
        // (STRING) Abbreviated language name
        // (STRING) Country name
        // (STRING) Abbreviated country
        // (STRING) Country (English)
        input.skipBytes(8); // System time (FILETIME)
        input.skipBytes(8); // Local time (FILETIME)
        int timezoneBias = input.readDword();
        int systemLcid = input.readDword();
        int userLcid = input.readDword();
        int userLangId = input.readDword();
        String langAbbrev = input.readString();
        String countryName = input.readString();
        String countryAbbrev = input.readString();
        String countryEnglish = input.readString();

        logger.debug("SID_LOCALEINFO: timezoneBias={}, systemLcid=0x{}, userLcid=0x{}, lang='{}', country='{}'",
            timezoneBias, Integer.toHexString(systemLcid), Integer.toHexString(userLcid), langAbbrev, countryName);

        // No response required for this packet
    }
}
