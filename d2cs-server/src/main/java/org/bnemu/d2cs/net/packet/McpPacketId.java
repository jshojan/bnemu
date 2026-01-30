package org.bnemu.d2cs.net.packet;

/**
 * MCP (Master Realm Protocol) packet IDs for D2CS.
 */
public enum McpPacketId {
    MCP_UNKNOWN(-1),
    MCP_STARTUP(0x01),           // Initial handshake
    MCP_CHARCREATE(0x02),        // Create character
    MCP_CREATEGAME(0x03),        // Create game
    MCP_JOINGAME(0x04),          // Join game
    MCP_GAMELIST(0x05),          // List games
    MCP_GAMEINFO(0x06),          // Game info
    MCP_CHARLOGON(0x07),         // Select character
    MCP_CHARDELETE(0x0A),        // Delete character
    MCP_REQUESTLADDERDATA(0x11), // Ladder data request
    MCP_MOTD(0x12),              // Message of the day
    MCP_CHARLIST(0x17),          // Character list (old, 4 chars max)
    MCP_CHARUPGRADE(0x18),       // Upgrade classic character to expansion
    MCP_CHARLIST2(0x19),         // Character list (8 chars max)
    MCP_CHARRANK(0x1A);          // Character ranking

    private final int code;

    McpPacketId(int code) {
        this.code = code;
    }

    public static McpPacketId fromCode(int code) {
        for (var id : McpPacketId.values()) {
            if (id.getCode() == code) {
                return id;
            }
        }
        return McpPacketId.MCP_UNKNOWN;
    }

    public byte getCode() {
        return (byte) code;
    }
}
