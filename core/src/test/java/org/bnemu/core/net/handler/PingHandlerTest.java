package org.bnemu.core.net.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.bnemu.core.net.packet.BncsPacket;
import org.bnemu.core.net.packet.BncsPacketId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PingHandlerTest {

    @Test
    public void testPingEcho() {
        EmbeddedChannel channel = new EmbeddedChannel(new PingHandlerWrapper());

        byte[] payload = new byte[] { 0x00, 0x01, 0x02 };
        BncsPacket ping = new BncsPacket(BncsPacketId.SID_PING, Unpooled.wrappedBuffer(payload));

        channel.writeInbound(ping); // Write inbound to trigger handler

        BncsPacket echoed = channel.readOutbound();
        assertNotNull(echoed);
        assertEquals(BncsPacketId.SID_PING, echoed.getCommand());

        byte[] actual = new byte[echoed.getPayload().readableBytes()];
        echoed.getPayload().readBytes(actual);
        assertArrayEquals(payload, actual);
    }

    // Inner wrapper to adapt BncsPacketHandler for EmbeddedChannel use
    private static class PingHandlerWrapper extends io.netty.channel.ChannelInboundHandlerAdapter {
        private final PingHandler handler = new PingHandler();

        @Override
        public void channelRead(io.netty.channel.ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof BncsPacket packet && handler.supports(packet.getCommand())) {
                handler.handle(ctx, packet);
            }
        }
    }
}
