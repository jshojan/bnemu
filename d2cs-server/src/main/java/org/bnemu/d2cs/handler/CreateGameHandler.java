package org.bnemu.d2cs.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import org.bnemu.core.dao.D2CharacterDao;
import org.bnemu.core.model.D2Character;
import org.bnemu.d2cs.game.GameRegistry;
import org.bnemu.d2cs.net.packet.McpPacket;
import org.bnemu.d2cs.net.packet.McpPacketBuffer;
import org.bnemu.d2cs.net.packet.McpPacketId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles MCP_CREATEGAME (0x03) - Create a new game.
 */
public class CreateGameHandler extends McpPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(CreateGameHandler.class);
    private static final AttributeKey<String> ACCOUNT_NAME_KEY = AttributeKey.valueOf("accountName");
    private static final AttributeKey<String> CHARACTER_NAME_KEY = AttributeKey.valueOf("characterName");

    private static final int RESULT_SUCCESS = 0x00;
    private static final int RESULT_ALREADY_EXISTS = 0x1E;
    private static final int RESULT_SERVER_DOWN = 0x1F;
    private static final int RESULT_DEAD_HARDCORE = 0x6E;

    private final GameRegistry gameRegistry;
    private final D2CharacterDao characterDao;

    public CreateGameHandler(GameRegistry gameRegistry, D2CharacterDao characterDao) {
        this.gameRegistry = gameRegistry;
        this.characterDao = characterDao;
    }

    @Override
    public McpPacketId packetId() {
        return McpPacketId.MCP_CREATEGAME;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, McpPacket packet) {
        var input = packet.payload();

        // C->S format:
        // (WORD) Request ID
        // (DWORD) Difficulty (0=Normal, 1=Nightmare, 2=Hell)
        // (BYTE) Unknown
        // (BYTE) Player difference
        // (BYTE) Max players
        // (STRING) Game name
        // (STRING) Game password
        // (STRING) Game description
        short requestId = input.readWord();
        int difficulty = input.readDword();
        input.readByte(); // unknown
        input.readByte(); // player difference
        byte maxPlayers = input.readByte();
        String gameName = input.readString();
        String gamePassword = input.readString();
        String gameDescription = input.readString();

        logger.info("MCP_CREATEGAME: name='{}', difficulty={}, maxPlayers={}", gameName, difficulty, maxPlayers);

        String accountName = ctx.channel().attr(ACCOUNT_NAME_KEY).get();
        String characterName = ctx.channel().attr(CHARACTER_NAME_KEY).get();
        if (accountName == null || characterName == null) {
            logger.warn("MCP_CREATEGAME: No account/character in session");
            sendResponse(ctx, requestId, 0, RESULT_SERVER_DOWN);
            return;
        }

        if (gameRegistry.exists(gameName)) {
            logger.info("MCP_CREATEGAME: Game '{}' already exists", gameName);
            sendResponse(ctx, requestId, 0, RESULT_ALREADY_EXISTS);
            return;
        }

        D2Character character = characterDao.findByAccountAndName(accountName, characterName);
        if (character != null && character.isHardcore() && character.isDead()) {
            logger.info("MCP_CREATEGAME: Dead hardcore character '{}'", characterName);
            sendResponse(ctx, requestId, 0, RESULT_DEAD_HARDCORE);
            return;
        }

        var game = gameRegistry.createGame(gameName, gamePassword, gameDescription,
                difficulty, maxPlayers, accountName);

        // Add creator as first character in the game
        if (character != null) {
            gameRegistry.addCharacter(gameName, characterName,
                    character.getCharClass() != null ? character.getCharClass().getCode() : 0,
                    character.getLevel());
        }

        logger.info("MCP_CREATEGAME: Game '{}' created by '{}' ({})", gameName, characterName, accountName);
        sendResponse(ctx, requestId, game.getGameToken(), RESULT_SUCCESS);
    }

    private void sendResponse(ChannelHandlerContext ctx, short requestId, int gameToken, int result) {
        // S->C format:
        // (WORD) Request ID
        // (DWORD) Game token
        // (WORD) Unknown
        // (DWORD) Result
        var output = new McpPacketBuffer()
                .writeWord(requestId)
                .writeDword(gameToken)
                .writeWord((short) 0)
                .writeDword(result);
        send(ctx, output);
    }
}
