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
 * Handles MCP_JOINGAME (0x04) - Join an existing game.
 */
public class JoinGameHandler extends McpPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(JoinGameHandler.class);
    private static final AttributeKey<String> ACCOUNT_NAME_KEY = AttributeKey.valueOf("accountName");
    private static final AttributeKey<String> CHARACTER_NAME_KEY = AttributeKey.valueOf("characterName");

    private static final int RESULT_SUCCESS = 0x00;
    private static final int RESULT_WRONG_PASSWORD = 0x29;
    private static final int RESULT_NOT_FOUND = 0x2A;
    private static final int RESULT_GAME_FULL = 0x2B;
    private static final int RESULT_DEAD_HARDCORE = 0x6E;

    private final GameRegistry gameRegistry;
    private final D2CharacterDao characterDao;
    private final int d2gsIp;

    public JoinGameHandler(GameRegistry gameRegistry, D2CharacterDao characterDao, String d2gsHost) {
        this.gameRegistry = gameRegistry;
        this.characterDao = characterDao;
        this.d2gsIp = ipToInt(d2gsHost);
    }

    @Override
    public McpPacketId packetId() {
        return McpPacketId.MCP_JOINGAME;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, McpPacket packet) {
        var input = packet.payload();

        // C->S format:
        // (WORD) Request ID
        // (STRING) Game name
        // (STRING) Game password
        short requestId = input.readWord();
        String gameName = input.readString();
        String gamePassword = input.readString();

        logger.info("MCP_JOINGAME: name='{}'", gameName);

        String accountName = ctx.channel().attr(ACCOUNT_NAME_KEY).get();
        String characterName = ctx.channel().attr(CHARACTER_NAME_KEY).get();
        if (accountName == null || characterName == null) {
            logger.warn("MCP_JOINGAME: No account/character in session");
            sendResponse(ctx, requestId, 0, 0, 0, RESULT_NOT_FOUND);
            return;
        }

        var game = gameRegistry.findByName(gameName);
        if (game == null) {
            logger.info("MCP_JOINGAME: Game '{}' not found", gameName);
            sendResponse(ctx, requestId, 0, 0, 0, RESULT_NOT_FOUND);
            return;
        }

        // Check password
        String requiredPassword = game.getPassword();
        if (requiredPassword != null && !requiredPassword.isEmpty()
                && !requiredPassword.equals(gamePassword)) {
            logger.info("MCP_JOINGAME: Wrong password for game '{}'", gameName);
            sendResponse(ctx, requestId, 0, 0, 0, RESULT_WRONG_PASSWORD);
            return;
        }

        // Check if full
        if (game.getCurrentPlayers() >= game.getMaxPlayers()) {
            logger.info("MCP_JOINGAME: Game '{}' is full ({}/{})",
                    gameName, game.getCurrentPlayers(), game.getMaxPlayers());
            sendResponse(ctx, requestId, 0, 0, 0, RESULT_GAME_FULL);
            return;
        }

        // Check dead hardcore
        D2Character character = characterDao.findByAccountAndName(accountName, characterName);
        if (character != null && character.isHardcore() && character.isDead()) {
            logger.info("MCP_JOINGAME: Dead hardcore character '{}'", characterName);
            sendResponse(ctx, requestId, 0, 0, 0, RESULT_DEAD_HARDCORE);
            return;
        }

        // Add character to game
        int charClass = (character != null && character.getCharClass() != null)
                ? character.getCharClass().getCode() : 0;
        int level = character != null ? character.getLevel() : 1;
        gameRegistry.addCharacter(gameName, characterName, charClass, level);

        logger.info("MCP_JOINGAME: '{}' joined game '{}'", characterName, gameName);
        sendResponse(ctx, requestId, game.getGameToken(), d2gsIp, game.getGameHash(), RESULT_SUCCESS);
    }

    private void sendResponse(ChannelHandlerContext ctx, short requestId,
                              int gameToken, int d2gsAddr, int gameHash, int result) {
        // S->C format:
        // (WORD) Request ID
        // (DWORD) Game token
        // (WORD) Unknown
        // (DWORD) D2GS IP address
        // (DWORD) Game hash
        // (DWORD) Result
        var output = new McpPacketBuffer()
                .writeWord(requestId)
                .writeDword(gameToken)
                .writeWord((short) 0)
                .writeDword(d2gsAddr)
                .writeDword(gameHash)
                .writeDword(result);
        send(ctx, output);
    }

    private static int ipToInt(String ip) {
        String[] parts = ip.split("\\.");
        return ((Integer.parseInt(parts[0]) & 0xFF))
                | ((Integer.parseInt(parts[1]) & 0xFF) << 8)
                | ((Integer.parseInt(parts[2]) & 0xFF) << 16)
                | ((Integer.parseInt(parts[3]) & 0xFF) << 24);
    }
}
