package org.bnemu.core.chat;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bnemu.core.net.packet.BncsPacket;
import org.bnemu.core.net.packet.BncsPacketId;

import java.nio.charset.StandardCharsets;

public class ChatEventBuilder {
    public static BncsPacket build(int eid, int flags, int ping, int ip, int account, int regAuth, String username, String text) {
        ByteBuf out = Unpooled.buffer();
        out.writeByte(eid);
        out.writeIntLE(flags);
        out.writeIntLE(ping);
        out.writeIntLE(ip);
        out.writeIntLE(account);
        out.writeIntLE(regAuth);
        writeString(out, username);
        if (text != null) {
            writeString(out, text);
        }
        return new BncsPacket(BncsPacketId.SID_CHATEVENT, out);
    }

    private static void writeString(ByteBuf buf, String value) {
        buf.writeBytes(value.getBytes(StandardCharsets.UTF_8));
        buf.writeByte(0x00);
    }
}
