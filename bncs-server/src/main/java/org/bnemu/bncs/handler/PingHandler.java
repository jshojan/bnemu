package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingHandler extends BncsPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(PingHandler.class);

    private final SessionManager sessions;

    public PingHandler(SessionManager sessions) {
        this.sessions = sessions;
    }

    @Override
    public BncsPacketId bncsPacketId() {
        return BncsPacketId.SID_PING;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        // Ping is measured once at login (sent alongside SID_AUTH_INFO response).
        // Subsequent pings from SessionTimeoutHandler are keep-alive only.
        if (sessions.has(ctx.channel(), "ping")) {
            return;
        }

        var payload = packet.payload();
        if (payload.length() >= 4) {
            // Client echoes back the DWORD cookie (System.currentTimeMillis() truncated to int).
            // RTT = current time - echoed value (works despite int overflow for reasonable RTT).
            int echoedValue = payload.readDword();
            int rtt = (int) System.currentTimeMillis() - echoedValue;
            if (rtt < 0) rtt = 0;
            sessions.set(ctx.channel(), "ping", String.valueOf(rtt));
            logger.info("Login ping from {}: {}ms", ctx.channel().remoteAddress(), rtt);
        }
    }
}