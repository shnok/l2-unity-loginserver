package com.shnok.javaserver.dto.external.serverpackets;

import com.shnok.javaserver.dto.SendablePacket;
import com.shnok.javaserver.enums.packettypes.external.ServerPacketType;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class InitPacket extends SendablePacket {
    public InitPacket(byte[] scrambledMod, byte[] blowfishKey, int sessionId) {
        super(ServerPacketType.Init.getValue());
        writeI(sessionId);
        writeI(scrambledMod.length);
        writeB(scrambledMod);
        writeI(blowfishKey.length);
        writeB(blowfishKey);

        log.info("sessionId [{}]", sessionId);
        log.info("scrambledMod [{}]: {}", scrambledMod.length, scrambledMod);
        log.info("Blowfish key [{}]: {}", blowfishKey.length, blowfishKey);

        buildPacket();
    }
}