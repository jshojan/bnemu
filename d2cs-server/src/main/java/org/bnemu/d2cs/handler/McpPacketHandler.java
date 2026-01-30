package org.bnemu.d2cs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.d2cs.net.packet.McpPacket;
import org.bnemu.d2cs.net.packet.McpPacketBuffer;
import org.bnemu.d2cs.net.packet.McpPacketId;

/**
 * Base class for MCP packet handlers.
 */
public abstract class McpPacketHandler {
    public abstract McpPacketId packetId();

    public abstract void handle(ChannelHandlerContext ctx, McpPacket packet);

    protected void send(ChannelHandlerContext ctx, McpPacketId id, McpPacketBuffer buffer) {
        ctx.writeAndFlush(new McpPacket(id, id.getCode(), buffer));
    }

    protected void send(ChannelHandlerContext ctx, McpPacketBuffer buffer) {
        send(ctx, packetId(), buffer);
    }
}
