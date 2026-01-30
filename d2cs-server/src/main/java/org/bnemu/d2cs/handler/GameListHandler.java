package org.bnemu.d2cs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.core.model.GameInfo;
import org.bnemu.d2cs.game.GameRegistry;
import org.bnemu.d2cs.net.packet.McpPacket;
import org.bnemu.d2cs.net.packet.McpPacketBuffer;
import org.bnemu.d2cs.net.packet.McpPacketId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Handles MCP_GAMELIST (0x05) - List available games.
 */
public class GameListHandler extends McpPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(GameListHandler.class);

    private final GameRegistry gameRegistry;

    public GameListHandler(GameRegistry gameRegistry) {
        this.gameRegistry = gameRegistry;
    }

    @Override
    public McpPacketId packetId() {
        return McpPacketId.MCP_GAMELIST;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, McpPacket packet) {
        var input = packet.payload();

        // C->S format:
        // (WORD) Request ID
        // (DWORD) Unknown
        // (STRING) Search filter
        short requestId = input.readWord();
        input.readDword(); // unknown
        String searchFilter = input.readString();

        logger.info("MCP_GAMELIST: filter='{}'", searchFilter);

        Collection<GameInfo> games = gameRegistry.listGames(searchFilter);

        for (GameInfo game : games) {
            var output = new McpPacketBuffer()
                    .writeWord(requestId)
                    .writeDword((int) game.getId())
                    .writeByte(game.getCurrentPlayers())
                    .writeDword(game.getDifficulty() & 0x0F)
                    .writeString(game.getName())
                    .writeString(game.getDescription() != null ? game.getDescription() : "");
            send(ctx, output);
        }

        // Send terminator (empty game name = end of list)
        var end = new McpPacketBuffer()
                .writeWord(requestId)
                .writeDword(0)
                .writeByte(0)
                .writeDword(0)
                .writeString("")
                .writeString("");
        send(ctx, end);

        logger.info("MCP_GAMELIST: Sent {} games", games.size());
    }
}
