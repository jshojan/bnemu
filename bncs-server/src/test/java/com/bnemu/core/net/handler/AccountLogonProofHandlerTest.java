package com.bnemu.core.net.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.bnemu.bncs.handler.AccountLogonProofHandler;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.dao.AccountDao;
import org.bnemu.core.session.SessionContext;
import org.bnemu.core.session.SessionManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class AccountLogonProofHandlerTest {

    @Test
    public void testLoginProofResponse() {
        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        Channel channel = Mockito.mock(Channel.class);
        @SuppressWarnings("unchecked")
        Attribute<SessionContext> attr = (Attribute<SessionContext>) Mockito.mock(Attribute.class);

        SessionContext session = new SessionContext();
        session.setAccountName("TestUser");

        Mockito.when(ctx.channel()).thenReturn(channel);
        Mockito.when(channel.attr(ArgumentMatchers.any(AttributeKey.class))).thenReturn(attr);
        Mockito.when(attr.get()).thenReturn(session);

        // Build test packet payload
        ByteBuf buf = Unpooled.buffer();
        buf.writeIntLE(123456789); // client token
        buf.writeIntLE(987654321); // server token
        buf.writeBytes(new byte[20]); // fake client hash
        buf.writeBytes("TestUser".getBytes());
        buf.writeByte(0x00); // null terminator
        BncsPacket packet = new BncsPacket(BncsPacketId.SID_AUTH_ACCOUNTLOGONPROOF, buf);

        AccountDao mockDao = Mockito.mock(AccountDao.class);
        SessionManager sessionManager = new SessionManager();
        sessionManager.set(channel, "username", "TestUser");

        // Mock that validation succeeds
        Mockito.when(mockDao.validatePassword(ArgumentMatchers.eq("TestUser"), ArgumentMatchers.any(byte[].class), ArgumentMatchers.eq(123456789), ArgumentMatchers.eq(987654321)))
                .thenReturn(true);

        AccountLogonProofHandler handler = new AccountLogonProofHandler(mockDao, sessionManager);
        handler.handle(ctx, packet);

        Mockito.verify(ctx).writeAndFlush(ArgumentMatchers.any(BncsPacket.class));
    }
}
