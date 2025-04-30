package org.bnemu.bncs.chat;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionManager;

import java.nio.charset.StandardCharsets;

public class WhisperManager {
    private final SessionManager sessionManager;

    public WhisperManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void sendWhisper(String fromUser, String toUser, String message) {
        Channel targetChannel = sessionManager.getChannelByUsername(toUser);
        if (targetChannel != null && targetChannel.isActive()) {
            ByteBuf out = Unpooled.buffer();
            out.writeByte(0x03); // EID_WHISPER
            out.writeIntLE(0);   // Flags
            out.writeIntLE(0);   // Ping
            out.writeIntLE(0);   // IP
            out.writeIntLE(0);   // Account number
            out.writeIntLE(0);   // Registration authority
            writeString(out, fromUser);
            writeString(out, message);

            BncsPacket packet = new BncsPacket(BncsPacketId.SID_CHATEVENT, out);
            targetChannel.writeAndFlush(packet);
        } else {
            // Handle the case where the target user is not found or not active
            // For example, send an error message back to the sender
        }
    }

    private void writeString(ByteBuf buf, String value) {
        buf.writeBytes(value.getBytes(StandardCharsets.US_ASCII));
        buf.writeByte(0x00);
    }
}