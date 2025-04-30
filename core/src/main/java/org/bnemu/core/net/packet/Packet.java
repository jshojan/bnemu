package org.bnemu.core.net.packet;

import io.netty.buffer.ByteBuf;

public interface Packet {
    byte getCommand();
    ByteBuf getPayload();
    int getLength();
}