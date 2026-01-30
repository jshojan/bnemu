package org.bnemu.d2cs.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import org.bnemu.core.auth.SelectedCharacterStore;
import org.bnemu.core.dao.D2CharacterDao;
import org.bnemu.core.model.D2Character;
import org.bnemu.d2cs.net.packet.McpPacket;
import org.bnemu.d2cs.net.packet.McpPacketBuffer;
import org.bnemu.d2cs.net.packet.McpPacketId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles MCP_CHARLOGON (0x07) - Select a character to play.
 */
public class CharLogonHandler extends McpPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(CharLogonHandler.class);
    private static final AttributeKey<String> ACCOUNT_NAME_KEY = AttributeKey.valueOf("accountName");
    private static final AttributeKey<String> CHARACTER_NAME_KEY = AttributeKey.valueOf("characterName");

    // Result codes
    private static final int RESULT_SUCCESS = 0x00;
    private static final int RESULT_CHARACTER_NOT_FOUND = 0x46;
    private static final int RESULT_LOGON_FAILED = 0x7A;

    private final D2CharacterDao characterDao;
    private final SelectedCharacterStore selectedCharStore;
    private final String realmName;

    public CharLogonHandler(D2CharacterDao characterDao, SelectedCharacterStore selectedCharStore, String realmName) {
        this.characterDao = characterDao;
        this.selectedCharStore = selectedCharStore;
        this.realmName = realmName;
    }

    @Override
    public McpPacketId packetId() {
        return McpPacketId.MCP_CHARLOGON;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, McpPacket packet) {
        var input = packet.payload();

        // C->S format:
        // (STRING) Character name
        String characterName = input.readString();

        // Get account name from session
        String accountName = ctx.channel().attr(ACCOUNT_NAME_KEY).get();
        if (accountName == null) {
            logger.warn("MCP_CHARLOGON: No account name in session");
            sendResult(ctx, RESULT_LOGON_FAILED);
            return;
        }

        // Find the character
        D2Character character = characterDao.findByAccountAndName(accountName, characterName);
        if (character == null) {
            logger.warn("MCP_CHARLOGON: Character '{}' not found for account '{}'",
                    characterName, accountName);
            sendResult(ctx, RESULT_CHARACTER_NOT_FOUND);
            return;
        }

        // Store character name in session for game creation
        ctx.channel().attr(CHARACTER_NAME_KEY).set(character.getName());

        // Update last played time
        character.setLastPlayedAt(System.currentTimeMillis());
        characterDao.update(character);

        // Store selected character for BNCS to pick up
        selectedCharStore.setSelectedCharacter(
            accountName,
            realmName,
            character.getName(),
            character.getCharClass() != null ? character.getCharClass().name() : "BARBARIAN",
            character.getLevel(),
            character.isExpansion(),
            character.isHardcore(),
            character.isLadder()
        );

        logger.info("MCP_CHARLOGON: '{}' selected character '{}' (Level {} {})",
                accountName, character.getName(), character.getLevel(),
                character.getCharClass() != null ? character.getCharClass().name() : "Unknown");

        sendResult(ctx, RESULT_SUCCESS);
    }

    private void sendResult(ChannelHandlerContext ctx, int result) {
        // S->C format:
        // (DWORD) Result
        var output = new McpPacketBuffer()
            .writeDword(result);
        send(ctx, output);
    }
}
