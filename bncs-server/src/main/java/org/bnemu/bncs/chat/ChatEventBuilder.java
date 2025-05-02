package org.bnemu.bncs.chat;

import org.bnemu.bncs.net.packet.BncsPacketBuffer;

public class ChatEventBuilder {
    public static BncsPacketBuffer build(int eid, int flags, int ping, int ip, int account, int regAuth, String username, String text) {
        return new BncsPacketBuffer()
            .writeDword(eid)
            .writeDword(flags)
            .writeDword(ping)
            .writeDword(ip)
            .writeDword(account)
            .writeDword(regAuth)
            .writeString(username)
            .writeString(text != null ? text : "");
    }
}
