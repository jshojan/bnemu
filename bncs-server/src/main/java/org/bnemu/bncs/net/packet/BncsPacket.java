package org.bnemu.bncs.net.packet;

import org.bnemu.core.net.packet.Packet;

public record BncsPacket(BncsPacketId packetId,
                         BncsPacketBuffer payload) implements Packet<BncsPacketId, BncsPacketBuffer> {

    public int getLength() {
        return 4 + payload.length();
    }

    /**
     * Returns a safe-to-send duplicate of this packet with retained buffer reference count.
     * Useful for broadcasting across multiple channels.
     */
    public BncsPacket duplicate() {
        return new BncsPacket(packetId, new BncsPacketBuffer(payload.duplicate()));
    }
}
