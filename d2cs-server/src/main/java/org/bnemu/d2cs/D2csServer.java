package org.bnemu.d2cs;

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
import org.bnemu.core.auth.RealmTokenStore;
import org.bnemu.core.auth.SelectedCharacterStore;
import org.bnemu.core.config.ConfigLoadException;
import org.bnemu.core.config.ConfigLoader;
import org.bnemu.core.config.CoreConfig;
import org.bnemu.core.dao.D2CharacterDao;
import org.bnemu.d2cs.game.GameRegistry;
import org.bnemu.d2cs.handler.McpDispatcher;
import org.bnemu.persistence.dao.MongoD2CharacterDao;
import org.bnemu.d2cs.net.packet.McpPacket;
import org.bnemu.d2cs.net.packet.McpPacketDecoder;
import org.bnemu.d2cs.net.packet.McpPacketEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Diablo 2 Character Server (D2CS).
 * Handles character management and game creation for D2 realm.
 */
public class D2csServer {
    private static final Logger logger = LoggerFactory.getLogger(D2csServer.class);

    private final int port;
    private final McpDispatcher dispatcher;

    public D2csServer(CoreConfig config) {
        this.port = config.getServer().getD2cs().getPort();

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

        // Initialize character DAO
        D2CharacterDao characterDao = new MongoD2CharacterDao(db);

        // Get realm name from config
        String realmName = config.getRealm() != null ? config.getRealm().getName() : "bnemu";

        // Initialize game registry and D2GS host for game management
        GameRegistry gameRegistry = new GameRegistry();
        String d2gsHost = config.getServer().getD2gs().getHost();

        this.dispatcher = new McpDispatcher(characterDao, SelectedCharacterStore.getInstance(),
                realmName, gameRegistry, d2gsHost);
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
                        pipeline.addLast(new McpPacketDecoder());
                        pipeline.addLast(new McpPacketEncoder());
                        pipeline.addLast(new SimpleChannelInboundHandler<McpPacket>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, McpPacket msg) {
                                dispatcher.dispatch(ctx, msg);
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                logger.error("Channel error", cause);
                                ctx.close();
                            }
                        });
                    }
                });

            ChannelFuture future = bootstrap.bind(port).sync();
            logger.info("D2CS server started on port {}", port);
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        try {
            CoreConfig config = ConfigLoader.load("config.yml");
            new D2csServer(config).start();
        } catch (ConfigLoadException e) {
            logger.error("Failed to load configuration", e);
            System.exit(1);
        } catch (InterruptedException e) {
            logger.error("Server was interrupted", e);
            Thread.currentThread().interrupt();
        }
    }
}
