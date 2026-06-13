package com.shnok.javaserver.dto.internal.loginserverpackets;

import com.shnok.javaserver.dto.SendablePacket;
import com.shnok.javaserver.enums.packettypes.internal.LoginServerPacketType;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;
import static com.shnok.javaserver.config.Configuration.server;

@Log4j2
public class InitLSPacket extends SendablePacket {
    public InitLSPacket(byte[] publicKey) {
        super(LoginServerPacketType.InitLS.getValue());
        writeI(server.revision());
        writeI(publicKey.length);
        writeB(publicKey);

        log.debug("Sent RSA public key {} - {}", publicKey.length, Arrays.toString(publicKey));

        buildPacket();
    }
}
