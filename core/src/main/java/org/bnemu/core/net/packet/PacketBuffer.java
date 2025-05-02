package org.bnemu.core.net.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public abstract class PacketBuffer<B, A extends PacketBuffer<B, A>> {
    protected ByteBuf buf;

    public PacketBuffer() {
        buf = Unpooled.buffer();
    }

    public PacketBuffer(ByteBuf buf) {
        this.buf = buf;
    }

    public ByteBuf getBuf() {
        return buf;
    }

    public ByteBuf duplicate() {
        return buf.retainedDuplicate();
    }

    public int length() {
        return buf.readableBytes();
    }

    protected abstract A self();

    public abstract ByteBuf withHeader(B id);

    public A writeByte(byte b) {
        buf.writeByte(b);
        return self();
    }

    public A writeByte(int b) {
        buf.writeByte((byte) b);
        return self();
    }

    public A writeWord(short s) {
        buf.writeShortLE(s);
        return self();
    }

    public A writeDword(int i) {
        buf.writeIntLE(i);
        return self();
    }

    public A writeString(String s) {
        buf.writeBytes(s.getBytes());
        buf.writeByte((byte) 0x00);
        return self();
    }

    public A writeBytes(byte[] b) {
        buf.writeBytes(b);
        return self();
    }

    public byte readByte() {
        return buf.readByte();
    }

    public byte[] readBytes(int length) {
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return bytes;
    }

    public short readWord() {
        return buf.readShortLE();
    }

    public int readDword() {
        return buf.readIntLE();
    }

    public String readString() {
        final var builder = new StringBuilder();
        var b = buf.readByte();
        while (b != 0x00) {
            builder.append((char) b);
            b = buf.readByte();
        }

        return builder.toString();
    }

    public A skipBytes(int n) {
        buf.skipBytes(n);
        return self();
    }
}
