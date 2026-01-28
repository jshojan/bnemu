package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketBuffer;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles SID_CDKEY2 (0x36) for older games like Warcraft 2 BNE.
 * This is the CD key verification packet.
 *
 * This handler accepts any CD key without validation.
 */
public class CdKey2Handler extends BncsPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(CdKey2Handler.class);

    // Result codes per BNetDocs
    private static final int RESULT_OK = 0x01;

    private final SessionManager sessionManager;

    public CdKey2Handler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public BncsPacketId bncsPacketId() {
        return BncsPacketId.SID_CDKEY2;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        var input = packet.payload();

        // Parse C->S format per BNetDocs:
        // (DWORD) Spawn (0 = normal, 1 = spawn)
        // (DWORD) CD Key length
        // (DWORD) CD Key product value
        // (DWORD) CD Key public value
        // (DWORD) Server token
        // (DWORD) Client token
        // (DWORD[5]) Hashed key data (20 bytes)
        // (STRING) Key owner name
        int spawn = input.readDword();
        int keyLength = input.readDword();
        int keyProduct = input.readDword();
        int keyPublic = input.readDword();
        int serverToken = input.readDword();
        int clientToken = input.readDword();
        byte[] hashedKeyData = input.readBytes(20);
        String keyOwner = input.readString();

        // Store client token for login
        sessionManager.set(ctx.channel(), "clientToken", String.valueOf(clientToken));

        logger.debug("SID_CDKEY2: spawn={}, keyLength={}, keyProduct=0x{}, keyOwner='{}'",
            spawn, keyLength, Integer.toHexString(keyProduct), keyOwner);

        // S->C response per BNetDocs:
        // (DWORD) Result (1 = OK, 2 = Invalid, 3 = Bad product, 4 = Banned, 5 = In use)
        // (STRING) Key owner
        var output = new BncsPacketBuffer()
            .writeDword(RESULT_OK)
            .writeString(keyOwner);
        send(ctx, output);
    }
}
