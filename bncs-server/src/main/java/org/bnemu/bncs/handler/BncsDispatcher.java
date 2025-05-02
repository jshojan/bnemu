package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.chat.ChatChannelManager;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.core.dao.AccountDao;
import org.bnemu.core.session.SessionManager;
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

    // production
    public BncsDispatcher(AccountDao accountDao, SessionManager sessions, ChatChannelManager channelManager) {
        this.accountDao = accountDao;
        this.sessions = sessions;
        this.channelManager = channelManager;
        registerDefaults();
    }

    // legacy/test mode
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
        register(new EnterChatHandler());
        register(new ChatCommandHandler(sessions, channelManager));
        register(new PingHandler());
        register(new LogonResponse2Handler(accountDao, sessions));
        register(new CreateAccount2Handler(accountDao));
        register(new GetChannelListHandler());

        // Only register JoinChannelHandler if channelManager is available
        if (channelManager != null) {
            register(new JoinChannelHandler(sessions, channelManager));
        }
    }

    public void register(BncsPacketHandler handler) {
        handlers.put(handler.bncsPacketId().getCode(), handler);
    }

    public void dispatch(ChannelHandlerContext ctx, BncsPacket packet) {
        BncsPacketHandler handler = handlers.get(packet.packetId().getCode());
        if (handler != null) {
            handler.handle(ctx, packet);
        } else {
            logger.debug("No handler found for packet ID: 0x{}", String.format("%02X", packet.packetId().getCode()));
        }
    }
}
