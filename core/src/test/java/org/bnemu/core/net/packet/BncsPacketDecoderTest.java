package org.bnemu.core.net.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BncsPacketDecoderTest {

    @Test
    public void testDecodeValidPacket() {
        // Construct a valid BNCS packet:
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(0x01); // Protocol byte
        buffer.writeByte(0xFF); // Header
        buffer.writeByte(0x0A); // Packet ID
        buffer.writeShortLE(7); // Length
        buffer.writeBytes(new byte[]{0x01, 0x02, 0x03}); // Payload

        // Initialize EmbeddedChannel with the decoder
        EmbeddedChannel channel = new EmbeddedChannel(new BncsPacketDecoder());

        // Write the buffer to the channel's inbound
        assertTrue(channel.writeInbound(buffer), "Failed to write inbound buffer");

        // Read the decoded packet
        BncsPacket decoded = channel.readInbound();
        assertNotNull(decoded, "Decoded packet is null");

        // Validate the decoded packet
        assertEquals(0x0A, decoded.getCommand(), "Packet ID mismatch");

        byte[] expectedPayload = new byte[]{0x01, 0x02, 0x03};
        byte[] actualPayload = new byte[decoded.getPayload().readableBytes()];
        decoded.getPayload().readBytes(actualPayload);
        assertArrayEquals(expectedPayload, actualPayload, "Payload mismatch");

        assertEquals(7, decoded.getLength(), "Length mismatch");
    }
}
