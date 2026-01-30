package org.bnemu.d2cs.net.packet;

/**
 * Represents a decoded MCP packet.
 */
public record McpPacket(McpPacketId packetId, byte rawPacketId, McpPacketBuffer payload) {
}
