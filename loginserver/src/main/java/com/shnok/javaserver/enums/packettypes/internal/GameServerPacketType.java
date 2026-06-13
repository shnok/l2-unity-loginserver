package com.shnok.javaserver.enums.packettypes.internal;

import java.util.HashMap;
import java.util.Map;

public enum GameServerPacketType {
    BlowFishKey((byte) 0),
    AuthRequest((byte) 1),
    PlayerInGame((byte) 2),
    PlayerLogout((byte) 3),
    ChangeAccessLevel((byte) 4),
    PlayerAuthRequest((byte) 5),
    ServerStatus((byte) 6),
    ReplyCharacters((byte) 7);

    private final byte value;

    GameServerPacketType(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    private static final Map<Byte, GameServerPacketType> BY_VALUE = new HashMap<>();

    static {
        for (GameServerPacketType type : values()) {
            BY_VALUE.put(type.getValue(), type);
        }
    }

    public static GameServerPacketType fromByte(byte value) {
        GameServerPacketType result = BY_VALUE.get(value);
        if (result == null) {
            throw new IllegalArgumentException("Invalid byte value for ClientPacketType: " + value);
        }
        return result;
    }
}