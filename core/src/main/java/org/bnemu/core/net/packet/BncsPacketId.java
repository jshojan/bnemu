package org.bnemu.core.net.packet;

public final class BncsPacketId {
    public static final byte SID_NULL = 0x00;
    public static final byte SID_PING = 0x25;
    public static final byte SID_ENTERCHAT = 0x0A;
    public static final byte SID_GETCHANNELLIST = 0x0B;
    public static final byte SID_JOINCHANNEL = 0x0C;
    public static final byte SID_LOGONRESPONSE2 = 0x3A;
    public static final byte SID_CREATEACCOUNT2 = 0x3D;
    public static final byte SID_AUTH_INFO = 0x50;
    public static final byte SID_AUTH_CHECK = 0x51;
    public static final byte SID_CREATEACCOUNT = 0x52;
    public static final byte SID_AUTH_ACCOUNTLOGON = 0x53;
    public static final byte SID_AUTH_ACCOUNTLOGONPROOF = 0x54;
    public static final byte SID_CHATCOMMAND = 0x0E;
    public static final byte SID_CHATEVENT = 0x0F;
    public static final byte SID_MESSAGEBOX = 0x19;

    private BncsPacketId() {} // Prevent instantiation
}