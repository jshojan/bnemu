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
            // Check DND (Do Not Disturb) - block the whisper entirely
            String dndMsg = sessionManager.get(targetChannel, "dnd");
            if (dndMsg != null) {
                var dndError = ChatEventBuilder.build(
                    ChatEventIds.EID_ERROR.getId(), 0, 0, 0, 0, 0, "",
                    toUser + " is in Do Not Disturb mode (" + dndMsg + ")."
                );
                senderChannel.writeAndFlush(new BncsPacket(BncsPacketId.SID_CHATEVENT, dndError));
                return;
            }

            // Send EID_WHISPER to the recipient with the message
            var whisperToTarget = ChatEventBuilder.build(
                ChatEventIds.EID_WHISPER.getId(), 0, 0, 0, 0, 0, fromUser, message
            );
            targetChannel.writeAndFlush(new BncsPacket(BncsPacketId.SID_CHATEVENT, whisperToTarget));

            // Send EID_WHISPERSENT confirmation to the sender
            var whisperSent = ChatEventBuilder.build(
                ChatEventIds.EID_WHISPERSENT.getId(), 0, 0, 0, 0, 0, toUser, message
            );
            senderChannel.writeAndFlush(new BncsPacket(BncsPacketId.SID_CHATEVENT, whisperSent));

            // Check away status - deliver whisper but notify sender
            String awayMsg = sessionManager.get(targetChannel, "away");
            if (awayMsg != null) {
                var awayInfo = ChatEventBuilder.build(
                    ChatEventIds.EID_INFO.getId(), 0, 0, 0, 0, 0, "",
                    toUser + " is away (" + awayMsg + ")."
                );
                senderChannel.writeAndFlush(new BncsPacket(BncsPacketId.SID_CHATEVENT, awayInfo));
            }
        } else {
            // User not found - send error to sender
            var errorPacket = ChatEventBuilder.build(
                ChatEventIds.EID_ERROR.getId(), 0, 0, 0, 0, 0, "",
                "That user is not logged on."
            );
            senderChannel.writeAndFlush(new BncsPacket(BncsPacketId.SID_CHATEVENT, errorPacket));
        }
    }
}