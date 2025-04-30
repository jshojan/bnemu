package com.bnemu.core.net.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.bnemu.bncs.chat.ChatChannelManager;
import org.bnemu.bncs.handler.ChatCommandHandler;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionContext;
import org.bnemu.core.session.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;

public class ChatCommandHandlerTest {
    private ChannelHandlerContext ctx;
    private Channel channel;
    private Attribute<SessionContext> attr;
    private SessionContext session;

    private SessionManager sessionManager;
    private ChatChannelManager channelManager;

    @BeforeEach
    public void setup() {
        ctx = Mockito.mock(ChannelHandlerContext.class);
        channel = Mockito.mock(Channel.class);
        attr = Mockito.mock(Attribute.class);

        session = new SessionContext();
        session.setAccountName("TestUser");
        session.setCurrentChannel("The Void");

        Mockito.when(ctx.channel()).thenReturn(channel);
        Mockito.when(channel.attr(ArgumentMatchers.any(AttributeKey.class))).thenReturn(attr);
        Mockito.when(attr.get()).thenReturn(session);

        sessionManager = new SessionManager();
        sessionManager.set(channel, "username", "TestUser");
        sessionManager.set(channel, "channel", "The Void");

        channelManager = new ChatChannelManager(sessionManager);
    }

    @Test
    public void testJoinCommand() {
        BncsPacket packet = new BncsPacket(BncsPacketId.SID_CHATCOMMAND,
                Unpooled.wrappedBuffer("/join Lobby".getBytes(StandardCharsets.US_ASCII)));
        new ChatCommandHandler(sessionManager, channelManager).handle(ctx, packet);
        // Can't directly verify join broadcast; just ensure no crash
    }

    @Test
    public void testWhisperCommand() {
        BncsPacket packet = new BncsPacket(BncsPacketId.SID_CHATCOMMAND,
                Unpooled.wrappedBuffer("/whisper User2 Hello".getBytes(StandardCharsets.US_ASCII)));
        new ChatCommandHandler(sessionManager, channelManager).handle(ctx, packet);
        Mockito.verify(ctx).writeAndFlush(ArgumentMatchers.any(BncsPacket.class));
    }

    @Test
    public void testEmoteCommand() {
        BncsPacket packet = new BncsPacket(BncsPacketId.SID_CHATCOMMAND,
                Unpooled.wrappedBuffer("/emote waves".getBytes(StandardCharsets.US_ASCII)));
        new ChatCommandHandler(sessionManager, channelManager).handle(ctx, packet);
        Mockito.verify(ctx).writeAndFlush(ArgumentMatchers.any(BncsPacket.class));
    }

    @Test
    public void testWhoisCommand() {
        BncsPacket packet = new BncsPacket(BncsPacketId.SID_CHATCOMMAND,
                Unpooled.wrappedBuffer("/whois User2".getBytes(StandardCharsets.US_ASCII)));
        new ChatCommandHandler(sessionManager, channelManager).handle(ctx, packet);
        Mockito.verify(ctx).writeAndFlush(ArgumentMatchers.any(BncsPacket.class));
    }

    @Test
    public void testUsersCommand() {
        channelManager.joinChannel("The Void", ctx, "TestUser");

        BncsPacket packet = new BncsPacket(BncsPacketId.SID_CHATCOMMAND,
                Unpooled.wrappedBuffer("/users".getBytes(StandardCharsets.US_ASCII)));
        new ChatCommandHandler(sessionManager, channelManager).handle(ctx, packet);
        Mockito.verify(ctx).writeAndFlush(ArgumentMatchers.argThat(response -> {
            if (!(response instanceof BncsPacket)) return false;
            BncsPacket bncsPacket = (BncsPacket) response;
            String msg = bncsPacket.getPayload().toString(0, bncsPacket.getPayload().readableBytes(), StandardCharsets.US_ASCII);
            return msg.startsWith("Users in channel:");
        }));
    }
}
