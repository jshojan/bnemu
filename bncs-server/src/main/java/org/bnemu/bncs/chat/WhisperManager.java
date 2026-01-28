package org.bnemu.bncs.chat;

import io.netty.channel.Channel;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionManager;

public class WhisperManager {
    private final SessionManager sessionManager;

    public WhisperManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void sendWhisper(Channel senderChannel, String fromUser, String toUser, String message) {
        Channel targetChannel = sessionManager.getChannelByUsername(toUser);

        if (targetChannel != null && targetChannel.isActive()) {
            // Send EID_WHISPER to the recipient with the message
            var whisperToTarget = ChatEventBuilder.build(
                ChatEventIds.EID_WHISPER.getId(),
                0,  // flags
                0,  // ping
                0,  // ip (defunct)
                0,  // account (defunct)
                0,  // regAuth (defunct)
                fromUser,
                message
            );
            targetChannel.writeAndFlush(new BncsPacket(BncsPacketId.SID_CHATEVENT, whisperToTarget));

            // Send EID_WHISPERSENT confirmation to the sender
            var whisperSent = ChatEventBuilder.build(
                ChatEventIds.EID_WHISPERSENT.getId(),
                0,  // flags (sender's flags)
                0,  // ping (sender's ping)
                0,  // ip (defunct)
                0,  // account (defunct)
                0,  // regAuth (defunct)
                toUser,
                message
            );
            senderChannel.writeAndFlush(new BncsPacket(BncsPacketId.SID_CHATEVENT, whisperSent));
        } else {
            // User not found - send error to sender
            var errorPacket = ChatEventBuilder.build(
                ChatEventIds.EID_ERROR.getId(),
                0,
                0,
                0,
                0,
                0,
                "",
                "That user is not logged on."
            );
            senderChannel.writeAndFlush(new BncsPacket(BncsPacketId.SID_CHATEVENT, errorPacket));
        }
    }
}