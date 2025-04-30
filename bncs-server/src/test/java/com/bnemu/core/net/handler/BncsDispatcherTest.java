package com.bnemu.core.net.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.chat.ChatChannelManager;
import org.bnemu.bncs.handler.BncsDispatcher;
import org.bnemu.bncs.handler.BncsPacketHandler;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.dao.AccountDao;
import org.bnemu.core.session.SessionManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.HashMap;

public class BncsDispatcherTest {

    static class TestDispatcher extends BncsDispatcher {
        private final Map<Byte, BncsPacketHandler> mockHandlers = new HashMap<>();

        public TestDispatcher(AccountDao dao, SessionManager sessions) {
            super(dao, sessions, true); // skipDefaults = true
        }

        public void registerMock(BncsPacketHandler handler) {
            for (byte i = Byte.MIN_VALUE; i < Byte.MAX_VALUE; i++) {
                if (handler.supports(i)) {
                    mockHandlers.put(i, handler);
                }
            }
        }

        @Override
        public void dispatch(ChannelHandlerContext ctx, BncsPacket packet) {
            BncsPacketHandler handler = mockHandlers.get(packet.getCommand());
            if (handler != null) {
                handler.handle(ctx, packet);
            }
        }
    }

    @Test
    public void testDispatchesToCorrectHandler() {
        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        BncsPacket packet = new BncsPacket(BncsPacketId.SID_ENTERCHAT, Unpooled.buffer(0));

        BncsPacketHandler handler = Mockito.mock(BncsPacketHandler.class);
        Mockito.when(handler.supports(BncsPacketId.SID_ENTERCHAT)).thenReturn(true);

        AccountDao dao = Mockito.mock(AccountDao.class);
        SessionManager sessions = Mockito.mock(SessionManager.class);

        TestDispatcher dispatcher = new TestDispatcher(dao, sessions);
        dispatcher.registerMock(handler);

        dispatcher.dispatch(ctx, packet);

        Mockito.verify(handler, Mockito.times(1)).handle(ctx, packet);
    }

    @Test
    public void testNoHandlerFound() {
        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        BncsPacket packet = new BncsPacket((byte) 0x7F, Unpooled.buffer(0)); // Unused

        AccountDao dao = Mockito.mock(AccountDao.class);
        SessionManager sessions = Mockito.mock(SessionManager.class);
        ChatChannelManager channelManager = Mockito.mock(ChatChannelManager.class);

        BncsDispatcher dispatcher = new BncsDispatcher(dao, sessions, channelManager);

        // Should not throw or fail
        dispatcher.dispatch(ctx, packet);

        // No handler to verify; just ensuring no exception is thrown
    }
}
