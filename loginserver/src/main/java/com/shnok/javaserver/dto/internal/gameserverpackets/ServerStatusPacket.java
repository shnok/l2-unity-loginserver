package com.shnok.javaserver.dto.internal.gameserverpackets;

import com.shnok.javaserver.dto.ReceivablePacket;
import lombok.Getter;

import java.util.ArrayList;

@Getter
public class ServerStatusPacket extends ReceivablePacket {
    private final ArrayList<Attribute> attributes;

    // Attributes
    public static final int SERVER_LIST_STATUS = 0x01;
    public static final int MAX_PLAYERS = 0x07;

    // Server Status
    public static final int STATUS_LIGHT = 0x00;
    public static final int STATUS_NORMAL = 0x01;
    public static final int STATUS_HEAVY = 0x02;
    public static final int STATUS_FULL = 0x03;
    public static final int STATUS_DOWN = 0x04;
    public static final int STATUS_GM_ONLY = 0x05;

    public static final String[] STATUS_STRING = {
            "Light",
            "Normal",
            "Heavy",
            "Full",
            "Down",
            "Gm Only"
    };

    public ServerStatusPacket(byte[] data) {
        super(data);

        attributes = new ArrayList<>();

        int totalAttributes = readI();
        for(int i = 0; i < totalAttributes; i++) {
            int attributeType = readI();
            int attributeValue = readI();
            attributes.add(new Attribute(attributeType, attributeValue));
        }
    }

    public static class Attribute {
        public int id;
        public int value;

        Attribute(int pId, int pValue) {
            id = pId;
            value = pValue;
        }
    }
}
