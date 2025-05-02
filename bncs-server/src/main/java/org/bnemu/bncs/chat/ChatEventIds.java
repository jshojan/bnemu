package org.bnemu.bncs.chat;

// TODO: turn this into an enum like BncsPacketId
public class ChatEventIds {
    public static final int EID_SHOWUSER = 0x01;
    public static final int EID_JOIN = 0x02;
    public static final int EID_LEAVE = 0x03;
    public static final int EID_WHISPER = 0x04;
    public static final int EID_TALK = 0x0F;
    public static final int EID_EMOTE = 0x17;
    public static final int EID_CHANNEL = 0x07;
    public static final int EID_BROADCAST = 0x06;
    public static final int EID_USERFLAGS = 0x09;
    public static final int EID_INFO = 0x12;
    public static final int EID_CHANNELDOESNOTEXIST = 0x0E;

    private ChatEventIds() {
        // Prevent instantiation
    }
}
