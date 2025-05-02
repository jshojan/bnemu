package org.bnemu.core.net.packet;

public interface Packet<A, B extends PacketBuffer<A, B>> {
    A packetId();

    B payload();

    int getLength();
}