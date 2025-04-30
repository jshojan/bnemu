package com.bnemu.core.net.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.handler.CreateAccountHandler;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.dao.AccountDao;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CreateAccountHandlerTest {

    @Test
    public void testCreateAccountSuccess() {
        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        AccountDao dao = Mockito.mock(AccountDao.class);

        byte[] payloadBytes = "newuser\u0000testhash\u0000".getBytes(StandardCharsets.US_ASCII);
        BncsPacket packet = new BncsPacket(BncsPacketId.SID_CREATEACCOUNT, Unpooled.wrappedBuffer(payloadBytes));

        Mockito.when(dao.createAccount(ArgumentMatchers.eq("newuser"), ArgumentMatchers.any())).thenReturn(true);

        CreateAccountHandler handler = new CreateAccountHandler(dao);
        handler.handle(ctx, packet);

        ArgumentCaptor<BncsPacket> captor = ArgumentCaptor.forClass(BncsPacket.class);
        Mockito.verify(ctx).writeAndFlush(captor.capture());

        BncsPacket response = captor.getValue();
        assertEquals(BncsPacketId.SID_CREATEACCOUNT, response.getCommand());

        byte[] expected = {0x00};
        byte[] actual = new byte[response.getPayload().readableBytes()];
        response.getPayload().readBytes(actual);
        Assertions.assertArrayEquals(expected, actual); // success = 0x00
    }

    @Test
    public void testCreateAccountFail() {
        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        AccountDao dao = Mockito.mock(AccountDao.class);

        byte[] payloadBytes = "existinguser\u0000testhash\u0000".getBytes(StandardCharsets.US_ASCII);
        BncsPacket packet = new BncsPacket(BncsPacketId.SID_CREATEACCOUNT, Unpooled.wrappedBuffer(payloadBytes));

        Mockito.when(dao.createAccount(ArgumentMatchers.eq("existinguser"), ArgumentMatchers.any())).thenReturn(false);

        CreateAccountHandler handler = new CreateAccountHandler(dao);
        handler.handle(ctx, packet);

        ArgumentCaptor<BncsPacket> captor = ArgumentCaptor.forClass(BncsPacket.class);
        Mockito.verify(ctx).writeAndFlush(captor.capture());

        BncsPacket response = captor.getValue();
        assertEquals(BncsPacketId.SID_CREATEACCOUNT, response.getCommand());

        byte[] expected = {0x01};
        byte[] actual = new byte[response.getPayload().readableBytes()];
        response.getPayload().readBytes(actual);
        Assertions.assertArrayEquals(expected, actual); // fail = 0x01
    }
}
