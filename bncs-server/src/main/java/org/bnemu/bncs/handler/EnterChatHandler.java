package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketBuffer;
import org.bnemu.bncs.net.packet.BncsPacketId;

public class EnterChatHandler extends BncsPacketHandler {
    @Override
    public BncsPacketId bncsPacketId() {
        return BncsPacketId.SID_ENTERCHAT;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        var input = packet.payload();
        var username = input.readString();

        if (username == null || username.isEmpty()) {
            var error = new BncsPacketBuffer().writeString("You must provide a username.");
            ctx.writeAndFlush(new BncsPacket(BncsPacketId.SID_MESSAGEBOX, error));
            return;
        }

        var output = new BncsPacketBuffer()
                .writeString(username)
                .writeString("RATS 0 0 0 0 0 0 0 0 RATS")
                .writeString(username);
        send(ctx, output);
    }
}
