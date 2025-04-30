package org.bnemu.bncs.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketHandler;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class GetChannelListHandler implements BncsPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(GetChannelListHandler.class);
    private final SessionManager sessions;

    public GetChannelListHandler(SessionManager sessions) {
        this.sessions = sessions;
    }

    @Override
    public boolean supports(byte packetId) {
        return packetId == BncsPacketId.SID_GETCHANNELLIST; // 0x0B
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        ByteBuf buf = packet.getPayload();

        // Read statstring from client
        String statstring = readCString(buf);
        logger.debug("GetChannelListHandler - Received statstring: '{}'", statstring);

        // Store to session
        sessions.set(ctx.channel(), "statstring", statstring);

        // Now we have both username and statstring — send SID_ENTERCHAT response
        String username = sessions.getUsername(ctx.channel());
        if (username == null || username.isEmpty()) {
            logger.warn("GetChannelListHandler - Username missing from session; skipping SID_ENTERCHAT response.");
            return;
        }

        ByteBuf response = Unpooled.buffer();
        writeString(response, username);     // Unique name
        writeString(response, statstring);   // Statstring
        writeString(response, username);     // Account name

        ctx.writeAndFlush(new BncsPacket(BncsPacketId.SID_ENTERCHAT, response));
        logger.debug("GetChannelListHandler - Sent SID_ENTERCHAT for user '{}'", username);
    }

    private String readCString(ByteBuf buf) {
        StringBuilder sb = new StringBuilder();
        while (buf.isReadable()) {
            byte b = buf.readByte();
            if (b == 0x00) break;
            sb.append((char) b);
        }
        return sb.toString();
    }

    private void writeString(ByteBuf buf, String str) {
        buf.writeBytes(str.getBytes(StandardCharsets.US_ASCII));
        buf.writeByte(0x00);
    }
}
