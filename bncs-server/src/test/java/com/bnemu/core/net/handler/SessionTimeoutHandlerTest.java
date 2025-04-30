package com.bnemu.core.net.handler;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.timeout.IdleStateEvent;
import org.bnemu.bncs.chat.ChatChannelManager;
import org.bnemu.bncs.handler.SessionTimeoutHandler;
import org.bnemu.core.session.SessionManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class SessionTimeoutHandlerTest {

    @Test
    public void testIdleEventTriggersCleanup() {
        // Mocks
        ChatChannelManager mockChannelManager = Mockito.mock(ChatChannelManager.class);
        SessionManager mockSessionManager = Mockito.mock(SessionManager.class);

        Mockito.when(mockChannelManager.getSessionManager()).thenReturn(mockSessionManager);
        Mockito.when(mockSessionManager.get(ArgumentMatchers.any(), ArgumentMatchers.eq("username"))).thenReturn("IdleUser");
        Mockito.when(mockSessionManager.get(ArgumentMatchers.any(), ArgumentMatchers.eq("channel"))).thenReturn("TestChannel");

        // Handler under test
        SessionTimeoutHandler handler = new SessionTimeoutHandler(mockChannelManager);
        EmbeddedChannel channel = new EmbeddedChannel(handler);

        // Simulate READER_IDLE event using Netty's predefined constant
        channel.pipeline().fireUserEventTriggered(IdleStateEvent.READER_IDLE_STATE_EVENT);

        // Verify proper channel cleanup
        Mockito.verify(mockChannelManager).leaveChannel(ArgumentMatchers.any());
        Assertions.assertFalse(channel.isActive(), "Channel should be closed due to idle timeout");
    }
}
