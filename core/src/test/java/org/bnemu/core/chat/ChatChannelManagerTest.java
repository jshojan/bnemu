package org.bnemu.core.chat;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.bnemu.core.net.packet.BncsPacket;
import org.bnemu.core.session.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ChatChannelManagerTest {

    private SessionManager sessionManager;
    private ChatChannelManager channelManager;
    private ChannelHandlerContext ctx;
    private Channel channel;

    @BeforeEach
    public void setup() {
        sessionManager = mock(SessionManager.class);
        channelManager = new ChatChannelManager(sessionManager);
        ctx = mock(ChannelHandlerContext.class);
        channel = mock(Channel.class);

        when(ctx.channel()).thenReturn(channel);
        when(channel.isActive()).thenReturn(true);
    }

    @Test
    public void testJoinAndLeaveChannel() {
        when(sessionManager.get(channel, "channel")).thenReturn(null);
        String channelName = "TestRoom";

        channelManager.joinChannel(channelName, ctx, "tester");

        ChatChannel chatChannel = channelManager.getChannel(channelName);
        assertNotNull(chatChannel);
        assertEquals(1, chatChannel.getUserCount());

        channelManager.leaveChannel(ctx);
        assertNull(channelManager.getChannel(channelName));
    }

    @Test
    public void testBroadcastMessage() {
        when(sessionManager.get(channel, "channel")).thenReturn(null);
        String channelName = "General";

        channelManager.joinChannel(channelName, ctx, "tester");

        ChatChannel chatChannel = channelManager.getChannel(channelName);
        BncsPacket packet = new BncsPacket((byte) 0x0F, Unpooled.buffer().writeByte(0x01));

        chatChannel.broadcastChatEvent(0x06, "tester", "Test broadcast");

        verify(channel, atLeastOnce()).writeAndFlush(any());
    }
}
