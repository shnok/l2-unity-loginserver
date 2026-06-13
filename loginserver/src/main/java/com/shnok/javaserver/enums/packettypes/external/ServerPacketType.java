package com.shnok.javaserver.enums.packettypes.external;

import java.util.HashMap;
import java.util.Map;

public enum ServerPacketType {
    Ping((byte)99),
    Init((byte)0),
    LoginFail((byte)1),
    AccountKicked((byte)2),
    LoginOk((byte)3),
    ServerList((byte)4),
    PlayFail((byte)6),
    PlayOk((byte)7),
    GGAuth((byte)0x0b);

    private final byte value;

    ServerPacketType(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    private static final Map<Byte, ServerPacketType> BY_VALUE = new HashMap<>();

    static {
        for (ServerPacketType type : values()) {
            BY_VALUE.put(type.getValue(), type);
        }
    }

    public static ServerPacketType fromByte(byte value) {
        ServerPacketType result = BY_VALUE.get(value);
        if (result == null) {
            throw new IllegalArgumentException("Invalid byte value for ClientPacketType: " + value);
        }
        return result;
    }
}
