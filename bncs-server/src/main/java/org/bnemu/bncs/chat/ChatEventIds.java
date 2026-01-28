package org.bnemu.bncs.chat;

public enum ChatEventIds {
    EID_SHOWUSER(0x01),           // User in channel (shown on join)
    EID_JOIN(0x02),               // User joined channel
    EID_LEAVE(0x03),              // User left channel
    EID_WHISPER(0x04),            // Private message received
    EID_TALK(0x05),               // User chat message in channel
    EID_BROADCAST(0x06),          // Server announcement
    EID_CHANNEL(0x07),            // Channel information
    EID_USERFLAGS(0x09),          // User flags update
    EID_WHISPERSENT(0x0A),        // Whisper sent confirmation
    EID_CHANNELFULL(0x0D),        // Channel at capacity
    EID_CHANNELDOESNOTEXIST(0x0E),// Channel not found
    EID_CHANNELRESTRICTED(0x0F),  // Access denied to channel
    EID_INFO(0x12),               // Information message
    EID_ERROR(0x13),              // Error message
    EID_IGNORE(0x15),             // User ignored (defunct)
    EID_ACCEPT(0x16),             // User unignored (defunct)
    EID_EMOTE(0x17);              // Emote action

    private final int id;

    ChatEventIds(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
