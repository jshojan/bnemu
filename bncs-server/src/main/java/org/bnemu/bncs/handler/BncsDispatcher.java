package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.chat.ChatChannelManager;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bnftp.BnftpFileProvider;
import org.bnemu.core.auth.SelectedCharacterStore;
import org.bnemu.core.config.CoreConfig;
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
    private final CoreConfig config;
    private final SelectedCharacterStore selectedCharStore;
    private final BnftpFileProvider bnftpFileProvider;

    // production
    public BncsDispatcher(AccountDao accountDao, SessionManager sessions, ChatChannelManager channelManager,
                          CoreConfig config, SelectedCharacterStore selectedCharStore,
                          BnftpFileProvider bnftpFileProvider) {
        this.accountDao = accountDao;
        this.sessions = sessions;
        this.channelManager = channelManager;
        this.config = config;
        this.selectedCharStore = selectedCharStore;
        this.bnftpFileProvider = bnftpFileProvider;
        registerDefaults();
    }

    // legacy/test mode
    public BncsDispatcher(AccountDao accountDao, SessionManager sessions, boolean skipDefaults) {
        this.accountDao = accountDao;
        this.sessions = sessions;
        this.channelManager = null; // prevents use of JoinChannelHandler
        this.config = null;
        this.selectedCharStore = null;
        this.bnftpFileProvider = null;
        if (!skipDefaults) {
            registerDefaults();
        }
    }

    private void registerDefaults() {
        register(new AccountLogonHandler(sessions));
        register(new AccountLogonProofHandler(accountDao, sessions));
        register(new AuthInfoHandler(sessions, bnftpFileProvider));
        register(new AuthCheckHandler(sessions));
        register(new EnterChatHandler(sessions, selectedCharStore));
        String realmName = config != null && config.getRealm() != null ? config.getRealm().getName() : null;
        register(new ChatCommandHandler(sessions, channelManager, realmName));
        register(new PingHandler(sessions));
        register(new NullHandler());
        register(new LogonResponse2Handler(accountDao, sessions));
        register(new CreateAccount2Handler(accountDao));
        register(new GetChannelListHandler());
        register(new CheckAdHandler());

        // W2BN/older games handlers
        register(new ClientIdHandler());
        register(new LocaleInfoHandler());
        register(new StartVersioningHandler(sessions));
        register(new ReportVersionHandler());
        register(new CdKey2Handler(sessions));
        register(new LogonResponseHandler(accountDao, sessions));

        // Only register channel-related handlers if channelManager is available
        if (channelManager != null) {
            register(new JoinChannelHandler(sessions, channelManager));
            register(new LeaveChatHandler(sessions, channelManager));
        }

        // D2 realm handlers (only if config is available)
        if (config != null) {
            register(new QueryRealms2Handler(config.getRealm()));
            register(new LogonRealmExHandler(sessions, config.getServer().getD2cs()));
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
            logger.debug("No handler found for packet ID: 0x{}", String.format("%02X", packet.rawPacketId()));
        }
    }
}
