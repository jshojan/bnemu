package org.bnemu.core.net.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.bnemu.core.dao.AccountDao;
import org.bnemu.core.net.packet.BncsPacket;
import org.bnemu.core.net.packet.BncsPacketId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class CreateAccountHandlerTest {

    @Test
    public void testCreateAccountSuccess() {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        AccountDao dao = mock(AccountDao.class);

        byte[] payloadBytes = "newuser\u0000testhash\u0000".getBytes(StandardCharsets.US_ASCII);
        BncsPacket packet = new BncsPacket(BncsPacketId.SID_CREATEACCOUNT, Unpooled.wrappedBuffer(payloadBytes));

        when(dao.createAccount(eq("newuser"), any())).thenReturn(true);

        CreateAccountHandler handler = new CreateAccountHandler(dao);
        handler.handle(ctx, packet);

        ArgumentCaptor<BncsPacket> captor = ArgumentCaptor.forClass(BncsPacket.class);
        verify(ctx).writeAndFlush(captor.capture());

        BncsPacket response = captor.getValue();
        assertEquals(BncsPacketId.SID_CREATEACCOUNT, response.getCommand());

        byte[] expected = {0x00};
        byte[] actual = new byte[response.getPayload().readableBytes()];
        response.getPayload().readBytes(actual);
        assertArrayEquals(expected, actual); // success = 0x00
    }

    @Test
    public void testCreateAccountFail() {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        AccountDao dao = mock(AccountDao.class);

        byte[] payloadBytes = "existinguser\u0000testhash\u0000".getBytes(StandardCharsets.US_ASCII);
        BncsPacket packet = new BncsPacket(BncsPacketId.SID_CREATEACCOUNT, Unpooled.wrappedBuffer(payloadBytes));

        when(dao.createAccount(eq("existinguser"), any())).thenReturn(false);

        CreateAccountHandler handler = new CreateAccountHandler(dao);
        handler.handle(ctx, packet);

        ArgumentCaptor<BncsPacket> captor = ArgumentCaptor.forClass(BncsPacket.class);
        verify(ctx).writeAndFlush(captor.capture());

        BncsPacket response = captor.getValue();
        assertEquals(BncsPacketId.SID_CREATEACCOUNT, response.getCommand());

        byte[] expected = {0x01};
        byte[] actual = new byte[response.getPayload().readableBytes()];
        response.getPayload().readBytes(actual);
        assertArrayEquals(expected, actual); // fail = 0x01
    }
}
