package org.bnemu.bncs.net.packet;

import org.bnemu.core.net.packet.Packet;

public record BncsPacket(BncsPacketId packetId,
                         byte rawPacketId,
                         BncsPacketBuffer payload) implements Packet<BncsPacketId, BncsPacketBuffer> {

    // Constructor for outgoing packets where we don't need rawPacketId
    public BncsPacket(BncsPacketId packetId, BncsPacketBuffer payload) {
        this(packetId, packetId.getCode(), payload);
    }

    public int getLength() {
        return 4 + payload.length();
    }

    /**
     * Returns a safe-to-send duplicate of this packet with retained buffer reference count.
     * Useful for broadcasting across multiple channels.
     */
    public BncsPacket duplicate() {
        return new BncsPacket(packetId, rawPacketId, new BncsPacketBuffer(payload.duplicate()));
    }
}
