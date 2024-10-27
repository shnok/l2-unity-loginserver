package com.shnok.javaserver.enums.packettypes.internal;

import java.util.HashMap;
import java.util.Map;

public enum LoginServerPacketType {
    InitLS((byte)0),
    Fail((byte)1),
    AuthResponse((byte) 2),
    PlayerAuthResponse((byte) 3),
    KickPlayer((byte) 4),
    ReceivableList((byte) 5),
    RequestCharacters((byte) 6); //Not used in ACIS interlude gameserver

    private final byte value;

    LoginServerPacketType(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    private static final Map<Byte, LoginServerPacketType> BY_VALUE = new HashMap<>();

    static {
        for (LoginServerPacketType type : values()) {
            BY_VALUE.put(type.getValue(), type);
        }
    }

    public static LoginServerPacketType fromByte(byte value) {
        LoginServerPacketType result = BY_VALUE.get(value);
        if (result == null) {
            throw new IllegalArgumentException("Invalid byte value for ClientPacketType: " + value);
        }
        return result;
    }
}
