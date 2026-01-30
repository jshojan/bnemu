package org.bnemu.d2cs.net.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bnemu.core.net.packet.PacketBuffer;

/**
 * MCP packet buffer with 3-byte header format: [SIZE:2][ID:1]
 * Size includes the header (3 bytes).
 */
public class McpPacketBuffer extends PacketBuffer<McpPacketId, McpPacketBuffer> {

    public McpPacketBuffer() {
        super();
    }

    public McpPacketBuffer(ByteBuf buf) {
        super(buf);
    }

    @Override
    protected McpPacketBuffer self() {
        return this;
    }

    @Override
    public ByteBuf withHeader(McpPacketId id) {
        final var output = Unpooled.buffer();
        final var headerSize = 3;
        final var length = this.buf.readableBytes() + headerSize;

        // MCP header: [SIZE:2 little-endian][ID:1]
        output.writeShortLE(length);
        output.writeByte(id.getCode());
        output.writeBytes(this.buf);

        return output;
    }
}
