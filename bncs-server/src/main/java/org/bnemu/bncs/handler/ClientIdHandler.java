package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketBuffer;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles SID_CLIENTID (0x1E) for older games like Warcraft 2 BNE.
 * This packet identifies the client and provides registration information.
 */
public class ClientIdHandler extends BncsPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(ClientIdHandler.class);

    @Override
    public BncsPacketId bncsPacketId() {
        return BncsPacketId.SID_CLIENTID;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        var input = packet.payload();

        // Parse C->S format per BNetDocs:
        // (DWORD) Registration Version
        // (DWORD) Registration Authority
        // (DWORD) Account Number
        // (DWORD) Registration Token
        // (STRING) LAN Computer Name
        // (STRING) LAN Username
        int regVersion = input.readDword();
        int regAuthority = input.readDword();
        int accountNumber = input.readDword();
        int regToken = input.readDword();
        String lanComputerName = input.readString();
        String lanUsername = input.readString();

        logger.debug("SID_CLIENTID: regVersion={}, regAuthority={}, accountNumber={}, regToken={}, lanComputer='{}', lanUser='{}'",
            regVersion, regAuthority, accountNumber, regToken, lanComputerName, lanUsername);

        // S->C response echoes the registration values
        var output = new BncsPacketBuffer()
            .writeDword(regVersion)
            .writeDword(regAuthority)
            .writeDword(accountNumber)
            .writeDword(regToken);
        send(ctx, output);
    }
}
