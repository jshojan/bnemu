package org.bnemu.d2cs.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import org.bnemu.core.dao.D2CharacterDao;
import org.bnemu.core.model.D2Character;
import org.bnemu.d2cs.net.packet.McpPacket;
import org.bnemu.d2cs.net.packet.McpPacketBuffer;
import org.bnemu.d2cs.net.packet.McpPacketId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Handles MCP_CHARLIST2 (0x19) - List characters for account.
 */
public class CharList2Handler extends McpPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(CharList2Handler.class);
    private static final AttributeKey<String> ACCOUNT_NAME_KEY = AttributeKey.valueOf("accountName");

    private final D2CharacterDao characterDao;

    public CharList2Handler(D2CharacterDao characterDao) {
        this.characterDao = characterDao;
    }

    @Override
    public McpPacketId packetId() {
        return McpPacketId.MCP_CHARLIST2;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, McpPacket packet) {
        var input = packet.payload();

        logger.info("MCP_CHARLIST2: Received character list request");

        // C->S format:
        // (DWORD) Number of characters to list (max 8)
        int requestedCount = input.readDword();
        logger.info("MCP_CHARLIST2: Requested count = {}", requestedCount);

        // Get account name from session (set during MCP_STARTUP)
        String accountName = ctx.channel().attr(ACCOUNT_NAME_KEY).get();
        if (accountName == null) {
            logger.warn("MCP_CHARLIST2: No account name in session");
            sendEmptyList(ctx, requestedCount);
            return;
        }

        // Get all characters for this account
        List<D2Character> characters = characterDao.findByAccountName(accountName);
        int totalCount = characters.size();
        int returnCount = Math.min(totalCount, Math.min(requestedCount, 8));

        logger.debug("MCP_CHARLIST2: account='{}', requested={}, total={}, returning={}",
                accountName, requestedCount, totalCount, returnCount);

        // S->C format:
        // (WORD) Number of characters requested
        // (DWORD) Total number of characters on account
        // (WORD) Number of characters returned
        // For each character:
        //   (DWORD) Expires (0 for no expiration)
        //   (STRING) Character name
        //   (WORD[6]) Character stat string (12 bytes)
        var output = new McpPacketBuffer()
            .writeWord((short) requestedCount)
            .writeDword(totalCount)
            .writeWord((short) returnCount);

        for (int i = 0; i < returnCount; i++) {
            D2Character character = characters.get(i);
            writeCharacterEntry(output, character);
        }

        send(ctx, output);
    }

    private void writeCharacterEntry(McpPacketBuffer output, D2Character character) {
        // (DWORD) Expires - use far future timestamp (90 days from now)
        // Note: 0xFFFFFFFF shows as 1969 because it's interpreted as signed -1
        long expiresAt = System.currentTimeMillis() / 1000 + (90L * 24 * 60 * 60);
        output.writeDword((int) expiresAt);

        // (STRING) Character name
        output.writeString(character.getName());

        // 33-byte character statstring per BnetDocs format
        // Note: Values 0x01-0xFF are valid; 0xFF typically means "empty/none"
        int charClass = character.getCharClass() != null ? character.getCharClass().getCode() : 0;
        int level = Math.max(1, Math.min(99, character.getLevel()));

        // Build flags byte (byte 27): 0x04=hardcore, 0x08=dead, 0x20=expansion, 0x40=ladder
        int flags = 0x80; // Base flag (character exists)
        if (character.isHardcore()) flags |= 0x04;
        if (character.isDead()) flags |= 0x08;
        if (character.isExpansion()) flags |= 0x20;
        if (character.isLadder()) flags |= 0x40;

        // Byte 1-2: Header bytes
        output.writeByte(0x84);
        output.writeByte(0x80);

        // Bytes 3-12: Equipment slots (10 bytes) - 0xFF = no item
        for (int i = 0; i < 10; i++) {
            output.writeByte(0xFF);
        }

        // Byte 13: Offhand item - 0xFF = none
        output.writeByte(0xFF);

        // Byte 14: Character class (1-indexed: 1=Amazon, 2=Sorc, 3=Necro, 4=Pally, 5=Barb, 6=Druid, 7=Sin)
        output.writeByte(charClass + 1);

        // Bytes 15-25: Equipment colors (11 bytes) - 0xFF = default/none
        for (int i = 0; i < 11; i++) {
            output.writeByte(0xFF);
        }

        // Byte 26: Level
        output.writeByte(level);

        // Byte 27: Flags
        output.writeByte(flags);

        // Byte 28: Current act - bitfield: bits 0-2 = act (0-4), bit 7 = active
        // 0x80 = Act 1 Normal active (bit 7 set, act = 0)
        output.writeByte(0x80);

        // Bytes 29-30: Unknown
        output.writeByte(0xFF);
        output.writeByte(0xFF);

        // Byte 31: Ladder (0xFF = non-ladder, other values = ladder season)
        output.writeByte(character.isLadder() ? 0x01 : 0xFF);

        // Bytes 32-33: Unknown
        output.writeByte(0xFF);
        output.writeByte(0xFF);

        // Null terminator - statstring is sent as null-terminated string
        output.writeByte(0x00);
    }

    private void sendEmptyList(ChannelHandlerContext ctx, int requestedCount) {
        var output = new McpPacketBuffer()
            .writeWord((short) requestedCount)
            .writeDword(0)  // Total characters
            .writeWord((short) 0);  // Returned characters

        send(ctx, output);
    }
}
