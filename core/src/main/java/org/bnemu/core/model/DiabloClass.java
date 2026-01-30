package org.bnemu.core.model;

/**
 * Diablo 2 character classes with MCP protocol codes.
 */
public enum DiabloClass {
    AMAZON(0),
    SORCERESS(1),
    NECROMANCER(2),
    PALADIN(3),
    BARBARIAN(4),
    DRUID(5),
    ASSASSIN(6);

    private final int code;

    DiabloClass(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static DiabloClass fromCode(int code) {
        for (var cls : DiabloClass.values()) {
            if (cls.code == code) {
                return cls;
            }
        }
        return null;
    }
}
