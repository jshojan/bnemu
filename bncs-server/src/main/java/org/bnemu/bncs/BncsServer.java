package org.bnemu.bncs;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.bnemu.bncs.chat.ChatChannelManager;
import org.bnemu.core.config.ConfigLoader;
import org.bnemu.core.config.ConfigLoadException;
import org.bnemu.core.config.CoreConfig;
import org.bnemu.core.dao.AccountDao;
import org.bnemu.bncs.handler.BncsDispatcher;
import org.bnemu.bncs.handler.SessionTimeoutHandler;
import org.bnemu.bncs.net.logging.InboundLoggingHandler;
import org.bnemu.bncs.net.logging.OutboundLoggingHandler;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketDecoder;
import org.bnemu.bncs.net.packet.BncsPacketEncoder;
import org.bnemu.core.session.SessionManager;
import org.bnemu.core.auth.RealmTokenStore;
import org.bnemu.core.auth.SelectedCharacterStore;
import org.bnemu.bnftp.BnftpFileProvider;
import org.bnemu.persistence.dao.MongoAccountDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class BncsServer {
    private static final Logger logger = LoggerFactory.getLogger(BncsServer.class);
    private final int port;
    private final BncsDispatcher dispatcher;
    private final ChatChannelManager channelManager;
    private final BnftpFileProvider bnftpFileProvider;

    public BncsServer(CoreConfig config) {
        this.port = config.getServer().getBncs().getPort();

        // Construct MongoDB connection
        String mongoUri = String.format(
                "mongodb://%s:%s@%s:%d/%s?authSource=admin",
                config.getMongo().getUsername(),
                config.getMongo().getPassword(),
                config.getMongo().getHost(),
                config.getMongo().getPort(),
                config.getMongo().getDatabase()
        );

        MongoClient mongoClient = MongoClients.create(mongoUri);
        MongoDatabase db = mongoClient.getDatabase(config.getMongo().getDatabase());

        // Initialize shared token store for D2 realm auth
        RealmTokenStore.initialize(db);

        // Initialize selected character store for cross-server sharing
        SelectedCharacterStore.initialize(db);

        // Initialize core components
        AccountDao accountDao = new MongoAccountDao(db);
        SessionManager sessions = new SessionManager();
        this.channelManager = new ChatChannelManager(sessions);

        // Initialize BNFTP file provider
        this.bnftpFileProvider = new BnftpFileProvider(Path.of(config.getBnftp().getFilesDir()));

        // Pass all components into the dispatcher (use same channelManager instance)
        this.dispatcher = new BncsDispatcher(accountDao, sessions, this.channelManager, config,
                                             SelectedCharacterStore.getInstance(), bnftpFileProvider);
    }


    public void start() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new BncsPacketDecoder(bnftpFileProvider));
                            pipeline.addLast(new InboundLoggingHandler());
                            pipeline.addLast(new BncsPacketEncoder());
                            pipeline.addLast(new OutboundLoggingHandler());
                            // Send ping every 30s of write idle, disconnect after 120s of read idle
                            pipeline.addLast(new IdleStateHandler(120, 30, 0));
                            pipeline.addLast(new SessionTimeoutHandler(channelManager));
                            pipeline.addLast(new SimpleChannelInboundHandler<BncsPacket>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, BncsPacket msg) {
                                    dispatcher.dispatch(ctx, msg);
                                }
                            });
                        }
                    });

            ChannelFuture future = bootstrap.bind(this.port).sync();
            logger.info("BNCS server started on port {}", this.port);
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        try {
            CoreConfig config = ConfigLoader.load("config.yml");
            new BncsServer(config).start();
        } catch (ConfigLoadException e) {
            logger.error("Failed to load configuration", e);
            System.exit(1);
        } catch (InterruptedException e) {
            logger.error("Server was interrupted", e);
            Thread.currentThread().interrupt();
        }
    }
}
