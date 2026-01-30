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

/**
 * Handles MCP_CHARUPGRADE (0x18) - Upgrade a classic D2 character to expansion (LoD).
 */
public class CharUpgradeHandler extends McpPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(CharUpgradeHandler.class);
    private static final AttributeKey<String> ACCOUNT_NAME_KEY = AttributeKey.valueOf("accountName");

    private static final int RESULT_SUCCESS = 0x00;
    private static final int RESULT_FAILED = 0x01;

    private final D2CharacterDao characterDao;

    public CharUpgradeHandler(D2CharacterDao characterDao) {
        this.characterDao = characterDao;
    }

    @Override
    public McpPacketId packetId() {
        return McpPacketId.MCP_CHARUPGRADE;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, McpPacket packet) {
        var input = packet.payload();

        // C->S format:
        // (STRING) Character name
        String characterName = input.readString();

        String accountName = ctx.channel().attr(ACCOUNT_NAME_KEY).get();
        if (accountName == null) {
            logger.warn("MCP_CHARUPGRADE: No account name in session");
            sendResult(ctx, RESULT_FAILED);
            return;
        }

        D2Character character = characterDao.findByAccountAndName(accountName, characterName);
        if (character == null) {
            logger.warn("MCP_CHARUPGRADE: Character '{}' not found for account '{}'",
                    characterName, accountName);
            sendResult(ctx, RESULT_FAILED);
            return;
        }

        if (character.isExpansion()) {
            logger.debug("MCP_CHARUPGRADE: Character '{}' is already expansion", characterName);
            sendResult(ctx, RESULT_SUCCESS);
            return;
        }

        character.setExpansion(true);
        characterDao.update(character);

        logger.info("MCP_CHARUPGRADE: '{}' upgraded character '{}' to expansion",
                accountName, characterName);

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
