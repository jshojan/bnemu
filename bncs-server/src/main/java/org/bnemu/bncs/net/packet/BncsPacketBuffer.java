package org.bnemu.bncs.net.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bnemu.core.net.packet.PacketBuffer;

public class BncsPacketBuffer extends PacketBuffer<BncsPacketBuffer, Byte> {

    public BncsPacketBuffer(ByteBuf buf) {
        super(buf);
    }

    @Override
    protected BncsPacketBuffer self() {
        return this;
    }

    @Override
    public ByteBuf withHeader(Byte id) {
        final var output = Unpooled.buffer();
        final var headerSize = 4;
        final var length = this.buf.readableBytes() + headerSize;

        output.writeByte(0xFF);
        output.writeByte(id);
        output.writeShortLE(length);
        output.writeBytes(this.buf);

        return output;
    }
}
