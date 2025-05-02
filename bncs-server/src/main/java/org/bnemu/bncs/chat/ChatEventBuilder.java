package org.bnemu.bncs.chat;

import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketBuffer;
import org.bnemu.bncs.net.packet.BncsPacketId;

public class ChatEventBuilder {
    public static BncsPacket build(int eid, int flags, int ping, int ip, int account, int regAuth, String username, String text) {
        var output = new BncsPacketBuffer()
                .writeDword(eid)
                .writeDword(flags)
                .writeDword(ping)
                .writeDword(ip)
                .writeDword(account)
                .writeDword(regAuth)
                .writeString(username)
                .writeString(text != null ? text : "");

        return new BncsPacket(BncsPacketId.SID_CHATEVENT, output);
    }
}
