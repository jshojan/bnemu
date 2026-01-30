package org.bnemu.d2cs.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import org.bnemu.core.auth.SelectedCharacterStore;
import org.bnemu.core.dao.D2CharacterDao;
import org.bnemu.core.model.D2Character;
import org.bnemu.core.model.DiabloClass;
import org.bnemu.d2cs.net.packet.McpPacket;
import org.bnemu.d2cs.net.packet.McpPacketBuffer;
import org.bnemu.d2cs.net.packet.McpPacketId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles MCP_CHARCREATE (0x02) - Create a new character.
 */
public class CharCreateHandler extends McpPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(CharCreateHandler.class);
    private static final AttributeKey<String> ACCOUNT_NAME_KEY = AttributeKey.valueOf("accountName");

    // Result codes
    private static final int RESULT_SUCCESS = 0x00;
    private static final int RESULT_NAME_ALREADY_EXISTS = 0x14;
    private static final int RESULT_INVALID_NAME = 0x15;

    private final D2CharacterDao characterDao;
    private final SelectedCharacterStore selectedCharStore;
    private final String realmName;

    public CharCreateHandler(D2CharacterDao characterDao, SelectedCharacterStore selectedCharStore,
                             String realmName) {
        this.characterDao = characterDao;
        this.selectedCharStore = selectedCharStore;
        this.realmName = realmName;
    }

    @Override
    public McpPacketId packetId() {
        return McpPacketId.MCP_CHARCREATE;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, McpPacket packet) {
        var input = packet.payload();

        logger.info("MCP_CHARCREATE: Received character creation request, payload size={}",
                packet.payload().length());

        // C->S format:
        // (DWORD) Character class
        // (WORD) Character flags (bits: 2=hardcore, 5=expansion, 6=ladder)
        // (STRING) Character name
        int charClassCode = input.readDword();
        short flags = input.readWord();
        String name = input.readString();

        logger.info("MCP_CHARCREATE: class={}, flags=0x{}, name='{}'",
                charClassCode, String.format("%04X", flags & 0xFFFF), name);

        // Get account name from session
        String accountName = ctx.channel().attr(ACCOUNT_NAME_KEY).get();
        if (accountName == null) {
            logger.warn("MCP_CHARCREATE: No account name in session");
            sendResult(ctx, RESULT_INVALID_NAME);
            return;
        }

        // Validate character class
        DiabloClass charClass = DiabloClass.fromCode(charClassCode);
        if (charClass == null) {
            logger.warn("MCP_CHARCREATE: Invalid character class: {}", charClassCode);
            sendResult(ctx, RESULT_INVALID_NAME);
            return;
        }

        // Validate name
        if (!isValidCharacterName(name)) {
            logger.warn("MCP_CHARCREATE: Invalid character name: '{}'", name);
            sendResult(ctx, RESULT_INVALID_NAME);
            return;
        }

        // Check if name is already taken
        if (!characterDao.isNameAvailable(name)) {
            logger.info("MCP_CHARCREATE: Name '{}' already exists", name);
            sendResult(ctx, RESULT_NAME_ALREADY_EXISTS);
            return;
        }

        // Create the character
        D2Character character = new D2Character();
        character.setAccountName(accountName);
        character.setName(name);
        character.setCharClass(charClass);
        character.setFlags(flags);
        character.setLevel(1);

        try {
            characterDao.save(character);

            // Store as selected — D2 client may enter chat directly after creation
            // without sending MCP_CHARLOGON first.
            selectedCharStore.setSelectedCharacter(
                accountName, realmName, name, charClass.name(),
                1, character.isExpansion(), character.isHardcore(), character.isLadder());

            logger.info("MCP_CHARCREATE: Created '{}' ({}) for account '{}', sending success",
                    name, charClass.name(), accountName);
            sendResult(ctx, RESULT_SUCCESS);
        } catch (Exception e) {
            logger.error("MCP_CHARCREATE: Failed to save character '{}': {}", name, e.getMessage(), e);
            sendResult(ctx, RESULT_NAME_ALREADY_EXISTS);
        }
    }

    private boolean isValidCharacterName(String name) {
        // Name must be 2-15 characters
        if (name == null || name.length() < 2 || name.length() > 15) {
            return false;
        }

        // Must start with a letter
        if (!Character.isLetter(name.charAt(0))) {
            return false;
        }

        // Can only contain letters, numbers, and underscores
        // Also can contain one hyphen, but not at start or end
        boolean hasHyphen = false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') {
                continue;
            }
            if (c == '-') {
                if (hasHyphen || i == 0 || i == name.length() - 1) {
                    return false;
                }
                hasHyphen = true;
                continue;
            }
            return false;
        }

        return true;
    }

    private void sendResult(ChannelHandlerContext ctx, int result) {
        // S->C format:
        // (DWORD) Result
        var output = new McpPacketBuffer()
            .writeDword(result);
        send(ctx, output);
    }
}
