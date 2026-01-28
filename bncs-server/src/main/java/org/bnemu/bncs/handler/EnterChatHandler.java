package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketBuffer;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionManager;

public class EnterChatHandler extends BncsPacketHandler {
    private final SessionManager sessions;

    public EnterChatHandler(SessionManager sessions) {
        this.sessions = sessions;
    }

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

        // Get product code from session (set by AuthInfoHandler)
        String product = sessions.get(ctx.channel(), "product");
        if (product == null || product.isEmpty()) {
            product = "RATS"; // Default to StarCraft
        }

        // Generate statstring - format: "PRODUCT <wins> <losses> <disconnects> <rating> <rank> <high rating> <high rank> <unknown> PRODUCT"
        String statstring = product + " 0 0 0 0 0 0 0 0 " + product;

        // Store username and statstring in session for use in channel events
        sessions.setUsername(ctx.channel(), username);
        sessions.set(ctx.channel(), "statstring", statstring);

        var output = new BncsPacketBuffer()
            .writeString(username)
            .writeString(statstring)
            .writeString(username);
        send(ctx, output);
    }
}
