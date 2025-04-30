package org.bnemu.core.net.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.bnemu.core.dao.AccountDao;
import org.bnemu.core.net.packet.BncsPacket;
import org.bnemu.core.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionContext;
import org.bnemu.core.session.SessionManager;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class AccountLogonProofHandlerTest {

    @Test
    public void testLoginProofResponse() {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        @SuppressWarnings("unchecked")
        Attribute<SessionContext> attr = (Attribute<SessionContext>) mock(Attribute.class);

        SessionContext session = new SessionContext();
        session.setAccountName("TestUser");

        when(ctx.channel()).thenReturn(channel);
        when(channel.attr(any(AttributeKey.class))).thenReturn(attr);
        when(attr.get()).thenReturn(session);

        // Build test packet payload
        ByteBuf buf = Unpooled.buffer();
        buf.writeIntLE(123456789); // client token
        buf.writeIntLE(987654321); // server token
        buf.writeBytes(new byte[20]); // fake client hash
        buf.writeBytes("TestUser".getBytes());
        buf.writeByte(0x00); // null terminator
        BncsPacket packet = new BncsPacket(BncsPacketId.SID_AUTH_ACCOUNTLOGONPROOF, buf);

        AccountDao mockDao = mock(AccountDao.class);
        SessionManager sessionManager = new SessionManager();
        sessionManager.set(channel, "username", "TestUser");

        // Mock that validation succeeds
        when(mockDao.validatePassword(eq("TestUser"), any(byte[].class), eq(123456789), eq(987654321)))
                .thenReturn(true);

        AccountLogonProofHandler handler = new AccountLogonProofHandler(mockDao, sessionManager);
        handler.handle(ctx, packet);

        verify(ctx).writeAndFlush(any(BncsPacket.class));
    }
}
