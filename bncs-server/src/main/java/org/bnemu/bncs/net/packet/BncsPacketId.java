package org.bnemu.bncs.net.packet;

public enum BncsPacketId {
    SID_UNKNOWN(-1),
    SID_NULL(0x00),
    SID_STARTVERSIONING(0x06),      // W2BN/older games version check request
    SID_REPORTVERSION(0x07),         // W2BN/older games version check response
    SID_ENTERCHAT(0x0A),
    SID_GETCHANNELLIST(0x0B),
    SID_JOINCHANNEL(0x0C),
    SID_CHATCOMMAND(0x0E),
    SID_CHATEVENT(0x0F),
    SID_LEAVECHAT(0x10),
    SID_LOCALEINFO(0x12),            // W2BN locale information
    SID_MESSAGEBOX(0x19),
    SID_CLIENTID(0x1E),              // W2BN/older games client identification
    SID_PING(0x25),
    SID_LOGONRESPONSE(0x29),         // W2BN/older games login
    SID_CREATEACCOUNT_OLD(0x2A),     // W2BN/older games account creation
    SID_CDKEY2(0x36),                // W2BN/older games CD key verification
    SID_LOGONRESPONSE2(0x3A),
    SID_CREATEACCOUNT2(0x3D),
    SID_AUTH_INFO(0x50),
    SID_AUTH_CHECK(0x51),
    SID_CREATEACCOUNT(0x52),
    SID_AUTH_ACCOUNTLOGON(0x53),
    SID_AUTH_ACCOUNTLOGONPROOF(0x54);

    private final int code;

    BncsPacketId(int code) {
        this.code = code;
    }

    public static BncsPacketId fromCode(int code) {
        for (var statusCode : BncsPacketId.values()) {
            if (statusCode.getCode() == code) {
                return statusCode;
            }
        }
        return BncsPacketId.SID_UNKNOWN;
    }

    public byte getCode() {
        return (byte) code;
    }
}