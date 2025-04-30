package org.bnemu.core.net.handler;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.timeout.IdleStateEvent;
import org.bnemu.core.chat.ChatChannelManager;
import org.bnemu.core.session.SessionManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

public class SessionTimeoutHandlerTest {

    @Test
    public void testIdleEventTriggersCleanup() {
        // Mocks
        ChatChannelManager mockChannelManager = mock(ChatChannelManager.class);
        SessionManager mockSessionManager = mock(SessionManager.class);

        when(mockChannelManager.getSessionManager()).thenReturn(mockSessionManager);
        when(mockSessionManager.get(any(), eq("username"))).thenReturn("IdleUser");
        when(mockSessionManager.get(any(), eq("channel"))).thenReturn("TestChannel");

        // Handler under test
        SessionTimeoutHandler handler = new SessionTimeoutHandler(mockChannelManager);
        EmbeddedChannel channel = new EmbeddedChannel(handler);

        // Simulate READER_IDLE event using Netty's predefined constant
        channel.pipeline().fireUserEventTriggered(IdleStateEvent.READER_IDLE_STATE_EVENT);

        // Verify proper channel cleanup
        verify(mockChannelManager).leaveChannel(any());
        assertFalse(channel.isActive(), "Channel should be closed due to idle timeout");
    }
}
