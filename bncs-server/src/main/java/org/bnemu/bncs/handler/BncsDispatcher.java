package org.bnemu.bncs.handler;

import org.bnemu.bncs.chat.ChatChannelManager;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.core.dao.AccountDao;
import org.bnemu.bncs.net.packet.BncsPacketHandler;
import org.bnemu.core.session.SessionManager;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class BncsDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(BncsDispatcher.class);

    private final Map<Byte, BncsPacketHandler> handlers = new HashMap<>();

    private final AccountDao accountDao;
    private final SessionManager sessions;
    private final ChatChannelManager channelManager;

    // ✅ Full constructor (used by production)
    public BncsDispatcher(AccountDao accountDao, SessionManager sessions, ChatChannelManager channelManager) {
        this.accountDao = accountDao;
        this.sessions = sessions;
        this.channelManager = channelManager;
        registerDefaults();
    }

    // ✅ Safe fallback constructor (legacy/test mode)
    public BncsDispatcher(AccountDao accountDao, SessionManager sessions, boolean skipDefaults) {
        this.accountDao = accountDao;
        this.sessions = sessions;
        this.channelManager = null; // prevents use of JoinChannelHandler
        if (!skipDefaults) {
            registerDefaults();
        }
    }

    private void registerDefaults() {
        register(new AccountLogonHandler(sessions));
        register(new AccountLogonProofHandler(accountDao, sessions));
        register(new AuthInfoHandler(sessions));
        register(new AuthCheckHandler(sessions));
        register(new EnterChatHandler(sessions));
        register(new ChatCommandHandler(sessions, channelManager));
        register(new PingHandler());
        register(new LogonResponse2Handler(accountDao, sessions));
        register(new CreateAccount2Handler(accountDao));
        register(new GetChannelListHandler(sessions));

        // Only register JoinChannelHandler if channelManager is available
        if (channelManager != null) {
            register(new JoinChannelHandler(sessions, channelManager));
        }
    }

    public void register(BncsPacketHandler handler) {
        for (int id = 0; id <= 0xFF; id++) {
            if (handler.supports((byte) id)) {
                handlers.put((byte) id, handler);
            }
        }
    }

    public void dispatch(ChannelHandlerContext ctx, BncsPacket packet) {
        BncsPacketHandler handler = handlers.get(packet.getCommand());
        if (handler != null) {
            handler.handle(ctx, packet);
        } else {
            logger.debug("No handler found for packet ID: 0x{}", String.format("%02X", packet.getCommand()));
        }
    }
}
