package com.shnok.javaserver.dto.internal.loginserverpackets;

import com.shnok.javaserver.dto.SendablePacket;
import com.shnok.javaserver.enums.packettypes.internal.LoginServerPacketType;
import com.shnok.javaserver.util.ServerNameDAO;

public class AuthResponsePacket extends SendablePacket {
    public AuthResponsePacket(int id) {
        super(LoginServerPacketType.AuthResponse.getValue());
        writeB((byte) id);
        writeS(ServerNameDAO.getServer(id));
        buildPacket();
    }
}