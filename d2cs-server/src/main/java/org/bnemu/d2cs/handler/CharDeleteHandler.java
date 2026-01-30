package org.bnemu.d2cs.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import org.bnemu.core.dao.D2CharacterDao;
import org.bnemu.d2cs.net.packet.McpPacket;
import org.bnemu.d2cs.net.packet.McpPacketBuffer;
import org.bnemu.d2cs.net.packet.McpPacketId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles MCP_CHARDELETE (0x0A) - Delete a character.
 */
public class CharDeleteHandler extends McpPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(CharDeleteHandler.class);
    private static final AttributeKey<String> ACCOUNT_NAME_KEY = AttributeKey.valueOf("accountName");

    private final D2CharacterDao characterDao;

    public CharDeleteHandler(D2CharacterDao characterDao) {
        this.characterDao = characterDao;
    }

    @Override
    public McpPacketId packetId() {
        return McpPacketId.MCP_CHARDELETE;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, McpPacket packet) {
        var input = packet.payload();

        // C->S: (WORD) Unknown + (STRING) Character Name
        input.readWord(); // skip unknown
        String characterName = input.readString();

        String accountName = ctx.channel().attr(ACCOUNT_NAME_KEY).get();
        if (accountName == null) {
            logger.warn("MCP_CHARDELETE: No account in session");
            sendResult(ctx, 0x01, characterName);
            return;
        }

        // Verify the character belongs to this account
        var character = characterDao.findByAccountAndName(accountName, characterName);
        if (character == null) {
            logger.warn("MCP_CHARDELETE: Character '{}' not found for account '{}'", characterName, accountName);
            sendResult(ctx, 0x01, characterName);
            return;
        }

        characterDao.delete(accountName, characterName);
        logger.info("MCP_CHARDELETE: Deleted '{}' from account '{}'", characterName, accountName);
        sendResult(ctx, 0x00, characterName);
    }

    private void sendResult(ChannelHandlerContext ctx, int result, String characterName) {
        // S->C: (WORD) Result + (STRING) Character Name
        var output = new McpPacketBuffer()
            .writeWord((short) result)
            .writeString(characterName != null ? characterName : "");
        send(ctx, output);
    }
}
