package org.bnemu.bncs.net.packet;

import io.netty.buffer.ByteBuf;
import org.bnemu.core.net.packet.Packet;

public class BncsPacket implements Packet {
    private final byte command;
    private final ByteBuf payload;

    public BncsPacket(byte command, ByteBuf payload) {
        this.command = command;
        this.payload = payload;
    }

    public byte getCommand() {
        return command;
    }

    public ByteBuf getPayload() {
        return payload;
    }
    public BncsPacketBuffer getBncsPacketBuffer() {
        return new BncsPacketBuffer(payload);
    }

    public int getLength() {
        return 4 + payload.readableBytes();
    }

    /**
     * Returns a safe-to-send duplicate of this packet with retained buffer reference count.
     * Useful for broadcasting across multiple channels.
     */
    public BncsPacket retainedDuplicate() {
        return new BncsPacket(command, payload.retainedDuplicate());
    }
}
