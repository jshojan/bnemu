package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketBuffer;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.config.RealmConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles SID_QUERYREALMS2 (0x40) for Diablo 2.
 * Returns the list of available realms.
 */
public class QueryRealms2Handler extends BncsPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(QueryRealms2Handler.class);

    private final RealmConfig realmConfig;

    public QueryRealms2Handler(RealmConfig realmConfig) {
        this.realmConfig = realmConfig;
    }

    @Override
    public BncsPacketId bncsPacketId() {
        return BncsPacketId.SID_QUERYREALMS2;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        logger.debug("SID_QUERYREALMS2: Client requesting realm list");

        // S->C format per BNetDocs:
        // (DWORD) Unknown (0)
        // (DWORD) Count
        // For each realm:
        //   (DWORD) Unknown (1)
        //   (STRING) Realm title
        //   (STRING) Realm description
        var output = new BncsPacketBuffer()
            .writeDword(0)              // Unknown
            .writeDword(1)              // Count (1 realm)
            .writeDword(1)              // Unknown (1)
            .writeString(realmConfig.getName())
            .writeString(realmConfig.getDescription());

        logger.debug("Sending realm list: name='{}', description='{}'",
            realmConfig.getName(), realmConfig.getDescription());

        send(ctx, output);
    }
}
