package com.bnemu.core.net.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.bnemu.bncs.handler.EnterChatHandler;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionContext;
import org.bnemu.core.session.SessionManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class EnterChatHandlerTest {

    @Test
    public void testEnterChatHandler() {
        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        Channel channel = Mockito.mock(Channel.class);
        @SuppressWarnings("unchecked")
        Attribute<SessionContext> attr = (Attribute<SessionContext>) Mockito.mock(Attribute.class);

        SessionContext session = new SessionContext();
        session.setAccountName("TestUser");

        Mockito.when(ctx.channel()).thenReturn(channel);
        Mockito.when(channel.attr(ArgumentMatchers.any(AttributeKey.class))).thenReturn(attr);
        Mockito.when(attr.get()).thenReturn(session);

        SessionManager sessionManager = new SessionManager();
        sessionManager.set(channel, "username", "TestUser");

        EnterChatHandler handler = new EnterChatHandler(sessionManager);
        handler.handle(ctx, new BncsPacket(BncsPacketId.SID_ENTERCHAT, Unpooled.buffer(0)));

        Mockito.verify(ctx).writeAndFlush(ArgumentMatchers.any(BncsPacket.class));
    }
}
