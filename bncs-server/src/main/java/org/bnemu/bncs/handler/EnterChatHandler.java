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
import java.util.Arrays;

public class EnterChatHandler implements BncsPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(EnterChatHandler.class);
    private final SessionManager sessions;

    public EnterChatHandler(SessionManager sessions) {
        this.sessions = sessions;
    }

    @Override
    public boolean supports(byte packetId) {
        return packetId == BncsPacketId.SID_ENTERCHAT;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        ByteBuf buf = packet.getPayload();

        // Debug: Raw packet bytes
        byte[] rawBytes = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), rawBytes);
        logger.debug("EnterChatHandler - Raw buffer bytes: {}", Arrays.toString(rawBytes));

        // Read only the username
        String username = readCString(buf);
        logger.debug("EnterChatHandler - Parsed username: '{}'", username);

        if (username == null || username.isEmpty()) {
            ByteBuf errorBuf = Unpooled.buffer();
            writeString(errorBuf, "You must provide a username.");
            ctx.writeAndFlush(new BncsPacket(BncsPacketId.SID_MESSAGEBOX, errorBuf));
            return;
        }

        // Just store username for now — statstring will come in next packet (0x0B)
        sessions.setUsername(ctx.channel(), username);
        logger.debug("EnterChatHandler - Stored username: '{}'", username);
    }

    private void writeString(ByteBuf buf, String str) {
        buf.writeBytes(str.getBytes(StandardCharsets.US_ASCII));
        buf.writeByte(0x00);
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
}
