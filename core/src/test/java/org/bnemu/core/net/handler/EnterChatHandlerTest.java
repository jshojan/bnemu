package org.bnemu.core.net.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.bnemu.core.net.packet.BncsPacket;
import org.bnemu.core.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionContext;
import org.bnemu.core.session.SessionManager;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class EnterChatHandlerTest {

    @Test
    public void testEnterChatHandler() {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        @SuppressWarnings("unchecked")
        Attribute<SessionContext> attr = (Attribute<SessionContext>) mock(Attribute.class);

        SessionContext session = new SessionContext();
        session.setAccountName("TestUser");

        when(ctx.channel()).thenReturn(channel);
        when(channel.attr(any(AttributeKey.class))).thenReturn(attr);
        when(attr.get()).thenReturn(session);

        SessionManager sessionManager = new SessionManager();
        sessionManager.set(channel, "username", "TestUser");

        EnterChatHandler handler = new EnterChatHandler(sessionManager);
        handler.handle(ctx, new BncsPacket(BncsPacketId.SID_ENTERCHAT, Unpooled.buffer(0)));

        verify(ctx).writeAndFlush(any(BncsPacket.class));
    }
}
