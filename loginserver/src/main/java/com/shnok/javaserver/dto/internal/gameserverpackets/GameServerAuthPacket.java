package com.shnok.javaserver.dto.internal.gameserverpackets;

import com.shnok.javaserver.dto.ReceivablePacket;
import lombok.Getter;

@Getter
public class GameServerAuthPacket extends ReceivablePacket {
    private final byte id;
    private final boolean acceptAlternate;
    private final boolean reserveHost;
    private final String host;
    private final int port;
    private final int maxPlayer;
    private final int hexIdLength;
    private final byte[] hexId;

    public GameServerAuthPacket(byte[] data) {
        super(data);

        id = readB();
        acceptAlternate = readB() == 1;
        reserveHost = readB() == 1;
        host = readS();
        port = readH();
        maxPlayer = readI();
        hexIdLength = readI();
        hexId = readB(hexIdLength);
    }
}
