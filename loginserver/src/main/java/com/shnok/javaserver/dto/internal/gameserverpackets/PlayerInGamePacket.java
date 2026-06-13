package com.shnok.javaserver.dto.internal.gameserverpackets;

import com.shnok.javaserver.dto.ReceivablePacket;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class PlayerInGamePacket extends ReceivablePacket {
    private final List<String> loggedUsers;
    public PlayerInGamePacket(byte[] data) {
        super(data);
        loggedUsers = new ArrayList<>();

        int playerCount = readH();
        for(int i = 0; i < playerCount; i++) {
            loggedUsers.add(readS());
        }
    }
}
