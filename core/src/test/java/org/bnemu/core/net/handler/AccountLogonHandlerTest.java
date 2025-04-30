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
        import static org.junit.jupiter.api.Assertions.*;

public class AccountLogonHandlerTest {

    @Test
    public void testAccountLogonResponse() {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        @SuppressWarnings("unchecked")
        Attribute<SessionContext> attr = (Attribute<SessionContext>) mock(Attribute.class);

        SessionContext session = new SessionContext();
        when(ctx.channel()).thenReturn(channel);
        when(channel.attr(any(AttributeKey.class))).thenReturn(attr);

        byte[] payload = "TestUser�".getBytes(); // <-- Fix null terminator
        BncsPacket packet = new BncsPacket(BncsPacketId.SID_AUTH_ACCOUNTLOGON, Unpooled.wrappedBuffer(payload));

        SessionManager sessionManager = new SessionManager(); // <-- Create SessionManager
        AccountLogonHandler handler = new AccountLogonHandler(sessionManager); // <-- Inject it
        handler.handle(ctx, packet);

        verify(attr).set(any(SessionContext.class));
        verify(ctx).writeAndFlush(any(BncsPacket.class));
    }
}
