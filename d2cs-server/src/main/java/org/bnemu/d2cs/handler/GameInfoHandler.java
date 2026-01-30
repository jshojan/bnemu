package org.bnemu.d2cs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.core.model.GameInfo;
import org.bnemu.core.model.GameInfo.GameCharacter;
import org.bnemu.d2cs.game.GameRegistry;
import org.bnemu.d2cs.net.packet.McpPacket;
import org.bnemu.d2cs.net.packet.McpPacketBuffer;
import org.bnemu.d2cs.net.packet.McpPacketId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Handles MCP_GAMEINFO (0x06) - Get game details.
 */
public class GameInfoHandler extends McpPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(GameInfoHandler.class);

    private final GameRegistry gameRegistry;

    public GameInfoHandler(GameRegistry gameRegistry) {
        this.gameRegistry = gameRegistry;
    }

    @Override
    public McpPacketId packetId() {
        return McpPacketId.MCP_GAMEINFO;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, McpPacket packet) {
        var input = packet.payload();

        // C->S format:
        // (WORD) Request ID
        // (STRING) Game name
        short requestId = input.readWord();
        String gameName = input.readString();

        logger.info("MCP_GAMEINFO: name='{}'", gameName);

        GameInfo game = gameRegistry.findByName(gameName);
        if (game == null) {
            logger.warn("MCP_GAMEINFO: Game '{}' not found", gameName);
            sendNotFound(ctx, requestId);
            return;
        }

        List<GameCharacter> characters;
        synchronized (game) {
            characters = List.copyOf(game.getCharacters());
        }

        int uptimeSeconds = (int) ((System.currentTimeMillis() - game.getCreatedAt()) / 1000);

        // S->C format:
        // (WORD) Request ID
        // (DWORD) Game status/flags
        // (DWORD) Uptime in seconds
        // (WORD) Unknown
        // (BYTE) Max players
        // (BYTE) Number of characters in game
        // For each character: (BYTE) class, (BYTE) level
        // (STRING) Game description
        // For each character: (STRING) name
        var output = new McpPacketBuffer()
                .writeWord(requestId)
                .writeDword(game.getDifficulty() & 0x0F)
                .writeDword(uptimeSeconds)
                .writeWord((short) 0)
                .writeByte(game.getMaxPlayers())
                .writeByte(characters.size());

        for (GameCharacter ch : characters) {
            output.writeByte(ch.charClass());
            output.writeByte(ch.level());
        }

        output.writeString(game.getDescription() != null ? game.getDescription() : "");

        for (GameCharacter ch : characters) {
            output.writeString(ch.name());
        }

        send(ctx, output);
        logger.info("MCP_GAMEINFO: Sent info for game '{}' ({} players, uptime={}s)",
                gameName, characters.size(), uptimeSeconds);
    }

    private void sendNotFound(ChannelHandlerContext ctx, short requestId) {
        var output = new McpPacketBuffer()
                .writeWord(requestId)
                .writeDword(0xFFFFFFFF)
                .writeDword(0)
                .writeWord((short) 0)
                .writeByte(0)
                .writeByte(0)
                .writeString("");
        send(ctx, output);
    }
}
