package com.shnok.javaserver.dto.internal.gameserverpackets;

import com.shnok.javaserver.dto.ReceivablePacket;
import com.shnok.javaserver.security.NewCrypt;
import com.shnok.javaserver.thread.GameServerThread;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import javax.crypto.Cipher;
import java.util.Arrays;

import static com.shnok.javaserver.config.Configuration.server;
import static com.shnok.javaserver.enums.GameServerState.BF_CONNECTED;
import static javax.crypto.Cipher.DECRYPT_MODE;

@Log4j2
@Getter
public class BlowFishKeyPacket extends ReceivablePacket {
    private byte[] blowFishKey;

    public BlowFishKeyPacket(byte[] data, GameServerThread gameServer) {
        super(data);

        int blowfishLength = readI();

        byte[] tempKey = readB(blowfishLength);

        if(server.printCryptography()) {
            log.debug("Encrypted blowfish key [{}]: {}", tempKey.length, Arrays.toString(tempKey));
        }

        try {
            byte[] tempDecryptKey;
            Cipher rsaCipher = Cipher.getInstance(server.gameserverRsaPaddingMode());
            rsaCipher.init(DECRYPT_MODE, gameServer.getPrivateKey());
            tempDecryptKey = rsaCipher.doFinal(tempKey);

            // there are nulls before the key we must remove them
            int i = 0;
            int len = tempDecryptKey.length;
            for (; i < len; i++) {
                if (tempDecryptKey[i] != 0) {
                    break;
                }
            }

            byte[] key = new byte[len - i];
            System.arraycopy(tempDecryptKey, i, key, 0, len - i);

            if(server.printCryptography()) {
                log.debug("Decrypted blowfish key: {}", Arrays.toString(key));
            }

            blowFishKey = key;
        } catch (Exception ex) {
            log.error("There has been an error while decrypting blowfish key (RSA)!", ex);
        }
    }
}
