package org.bnemu.bncs.chat;

import io.netty.channel.Channel;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketBuffer;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionManager;

public class WhisperManager {
    private final SessionManager sessionManager;

    public WhisperManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void sendWhisper(String fromUser, String toUser, String message) {
        Channel targetChannel = sessionManager.getChannelByUsername(toUser);
        if (targetChannel != null && targetChannel.isActive()) {
            var output = new BncsPacketBuffer()
                    .writeByte(0x03)
                    .writeDword(0)
                    .writeDword(0)
                    .writeDword(0)
                    .writeDword(0)
                    .writeDword(0)
                    .writeString(fromUser)
                    .writeString(toUser);

            BncsPacket packet = new BncsPacket(BncsPacketId.SID_CHATEVENT, output);
            targetChannel.writeAndFlush(packet);
        }
    }
}