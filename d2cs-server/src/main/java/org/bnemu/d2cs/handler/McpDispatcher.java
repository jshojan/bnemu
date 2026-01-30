package org.bnemu.d2cs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.core.auth.RealmTokenStore;
import org.bnemu.core.auth.SelectedCharacterStore;
import org.bnemu.core.dao.D2CharacterDao;
import org.bnemu.d2cs.game.GameRegistry;
import org.bnemu.d2cs.net.packet.McpPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Dispatches MCP packets to appropriate handlers.
 */
public class McpDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(McpDispatcher.class);

    private final Map<Byte, McpPacketHandler> handlers = new HashMap<>();
    private final RealmTokenStore tokenStore;

    public McpDispatcher(D2CharacterDao characterDao, SelectedCharacterStore selectedCharStore,
                         String realmName, GameRegistry gameRegistry, String d2gsHost) {
        this.tokenStore = RealmTokenStore.getInstance();
        registerDefaults(characterDao, selectedCharStore, realmName, gameRegistry, d2gsHost);
    }

    private void registerDefaults(D2CharacterDao characterDao, SelectedCharacterStore selectedCharStore,
                                  String realmName, GameRegistry gameRegistry, String d2gsHost) {
        register(new StartupHandler(tokenStore));
        register(new MotdHandler());
        register(new CharList2Handler(characterDao));
        register(new CharCreateHandler(characterDao, selectedCharStore, realmName));
        register(new CharLogonHandler(characterDao, selectedCharStore, realmName));
        register(new CharUpgradeHandler(characterDao));
        register(new CharDeleteHandler(characterDao));

        // Game management handlers
        register(new CreateGameHandler(gameRegistry, characterDao));
        register(new JoinGameHandler(gameRegistry, characterDao, d2gsHost));
        register(new GameListHandler(gameRegistry));
        register(new GameInfoHandler(gameRegistry));
    }

    public void register(McpPacketHandler handler) {
        handlers.put(handler.packetId().getCode(), handler);
    }

    public void dispatch(ChannelHandlerContext ctx, McpPacket packet) {
        logger.info("MCP packet received: 0x{} ({}), payload size={}",
            String.format("%02X", packet.rawPacketId()),
            packet.packetId().name(),
            packet.payload().length());

        McpPacketHandler handler = handlers.get(packet.packetId().getCode());
        if (handler != null) {
            handler.handle(ctx, packet);
        } else {
            logger.warn("No handler found for MCP packet ID: 0x{}",
                String.format("%02X", packet.rawPacketId()));
        }
    }
}
