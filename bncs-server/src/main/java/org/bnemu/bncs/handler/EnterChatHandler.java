package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketBuffer;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnterChatHandler extends BncsPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(EnterChatHandler.class);
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
        logger.debug("EnterChatHandler - Parsed username: '{}'", username);

        if (username == null || username.isEmpty()) {
            var error = new BncsPacketBuffer().writeString("You must provide a username.");
            ctx.writeAndFlush(new BncsPacket(BncsPacketId.SID_MESSAGEBOX, error));
            return;
        }

        var output = new BncsPacketBuffer()
                .writeString(username)
                .writeString("RATS")
                .writeString(username);
        send(ctx, output);
    }
}
