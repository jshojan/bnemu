package org.bnemu.core.net.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.bnemu.core.chat.ChatChannelManager;
import org.bnemu.core.dao.AccountDao;
import org.bnemu.core.net.packet.BncsPacket;
import org.bnemu.core.net.packet.BncsPacketHandler;
import org.bnemu.core.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionManager;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.HashMap;

import static org.mockito.Mockito.*;

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
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        BncsPacket packet = new BncsPacket(BncsPacketId.SID_ENTERCHAT, Unpooled.buffer(0));

        BncsPacketHandler handler = mock(BncsPacketHandler.class);
        when(handler.supports(BncsPacketId.SID_ENTERCHAT)).thenReturn(true);

        AccountDao dao = mock(AccountDao.class);
        SessionManager sessions = mock(SessionManager.class);

        TestDispatcher dispatcher = new TestDispatcher(dao, sessions);
        dispatcher.registerMock(handler);

        dispatcher.dispatch(ctx, packet);

        verify(handler, times(1)).handle(ctx, packet);
    }

    @Test
    public void testNoHandlerFound() {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        BncsPacket packet = new BncsPacket((byte) 0x7F, Unpooled.buffer(0)); // Unused

        AccountDao dao = mock(AccountDao.class);
        SessionManager sessions = mock(SessionManager.class);
        ChatChannelManager channelManager = mock(ChatChannelManager.class);

        BncsDispatcher dispatcher = new BncsDispatcher(dao, sessions, channelManager);

        // Should not throw or fail
        dispatcher.dispatch(ctx, packet);

        // No handler to verify; just ensuring no exception is thrown
    }
}
