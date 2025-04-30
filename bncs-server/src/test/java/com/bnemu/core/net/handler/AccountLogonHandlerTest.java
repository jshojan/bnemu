package com.bnemu.core.net.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.bnemu.bncs.handler.AccountLogonHandler;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionContext;
import org.bnemu.core.session.SessionManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class AccountLogonHandlerTest {

    @Test
    public void testAccountLogonResponse() {
        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        Channel channel = Mockito.mock(Channel.class);
        @SuppressWarnings("unchecked")
        Attribute<SessionContext> attr = (Attribute<SessionContext>) Mockito.mock(Attribute.class);

        SessionContext session = new SessionContext();
        Mockito.when(ctx.channel()).thenReturn(channel);
        Mockito.when(channel.attr(ArgumentMatchers.any(AttributeKey.class))).thenReturn(attr);

        byte[] payload = "TestUser�".getBytes(); // <-- Fix null terminator
        BncsPacket packet = new BncsPacket(BncsPacketId.SID_AUTH_ACCOUNTLOGON, Unpooled.wrappedBuffer(payload));

        SessionManager sessionManager = new SessionManager(); // <-- Create SessionManager
        AccountLogonHandler handler = new AccountLogonHandler(sessionManager); // <-- Inject it
        handler.handle(ctx, packet);

        Mockito.verify(attr).set(ArgumentMatchers.any(SessionContext.class));
        Mockito.verify(ctx).writeAndFlush(ArgumentMatchers.any(BncsPacket.class));
    }
}
