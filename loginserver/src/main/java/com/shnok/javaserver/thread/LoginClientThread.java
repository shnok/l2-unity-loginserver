package com.shnok.javaserver.thread;

import com.shnok.javaserver.dto.SendablePacket;
import com.shnok.javaserver.dto.external.serverpackets.*;
import com.shnok.javaserver.enums.AccountKickedReason;
import com.shnok.javaserver.enums.LoginClientState;
import com.shnok.javaserver.enums.LoginFailReason;
import com.shnok.javaserver.enums.PlayFailReason;
import com.shnok.javaserver.enums.packettypes.external.ServerPacketType;
import com.shnok.javaserver.model.SessionKey;
import com.shnok.javaserver.security.LoginCrypt;
import com.shnok.javaserver.security.Rnd;
import com.shnok.javaserver.security.ScrambledKeyPair;
import com.shnok.javaserver.service.LoginServerController;
import com.shnok.javaserver.service.ThreadPoolManagerService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import javax.swing.*;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.shnok.javaserver.config.Configuration.server;

@Getter
@Setter
@Log4j2
public class LoginClientThread extends Thread {
    private final Socket connection;
    private final String connectionIp;
    public boolean authenticated;
    private InputStream in;
    private OutputStream out;
    private String username;
    private boolean clientReady = false;
    private long lastEcho;
    private Timer watchDog;
    private LoginClientState loginClientState;
    private final LoginCrypt loginCrypt;
    private final ScrambledKeyPair scrambledPair;
    private final byte[] blowfishKey;
    private int accessLevel;
    private int lastGameserver;
    private boolean joinedGS;
    private int sessionId;
    private SessionKey sessionKey;
    private Map<Integer, Integer> charsOnServers;
    private Map<Integer, long[]> charsToDelete;
    private int connectionTimeoutMs;
    private int expectedCharacterCount;

    public LoginClientThread(Socket con) {
        connection = con;
        connectionIp = con.getInetAddress().getHostAddress();
        scrambledPair = LoginServerController.getInstance().getScrambledRSAKeyPair();
        blowfishKey = LoginServerController.getInstance().getBlowfishKey();
        loginCrypt = new LoginCrypt();
        loginCrypt.setKey(blowfishKey);
        sessionId = Rnd.nextInt();
        connectionTimeoutMs = server.serverConnectionTimeoutMs();

        try {
            in = connection.getInputStream();
            out = new BufferedOutputStream(connection.getOutputStream());
            log.debug("New connection: {}" + connectionIp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        startReadingPackets();
    }

    private void startReadingPackets() {
        int lengthHi;
        int lengthLo;
        int length;

        try {
//            System.out.println("MODULUS " + ((RSAPublicKey) scrambledPair.getPair().getPublic()).getModulus().toByteArray().length +   " : " + Arrays.toString(((RSAPublicKey) scrambledPair.getPair().getPublic()).getModulus().toByteArray()));
//            System.out.println("EXP: " + Arrays.toString(((RSAPublicKey) scrambledPair.getPair().getPublic()).getPublicExponent().toByteArray()));
//            System.out.println("SCRAMBLED: " + Arrays.toString(getScrambledPair().getScrambledModulus()));

            sendPacket(new InitPacket(getScrambledPair().getScrambledModulus(), getBlowfishKey(), sessionId));

            for (; ; ) {
                lengthLo = in.read();
                lengthHi = in.read();
                length = (lengthHi * 256) + lengthLo;

                if ((lengthHi < 0) || connection.isClosed()) {
                    log.warn("Loginserver terminated the connection!");
                    break;
                }

                int len = length - 2;
                byte[] data = new byte[len];

                int receivedBytes = 0;
                int newBytes = 0;
                while ((newBytes != -1) && (receivedBytes < (len))) {
                    newBytes = in.read(data, 0, len);
                    receivedBytes = receivedBytes + newBytes;
                }

                handlePacket(data);
            }
        } catch (Exception e) {
            log.error("Exception while reading client packets.");
        } finally {
            log.info("User {} disconnected", connectionIp);
            disconnect();
        }
    }

    public void close(AccountKickedReason kickedReason) {
        sendPacket(new AccountKickedPacket(kickedReason));
        disconnect();
    }

    public void close(LoginFailReason failReason) {
        sendPacket(new LoginFailPacket(failReason));
        disconnect();
    }

    public void close(PlayFailReason failReason) {
        sendPacket(new PlayFailPacket(failReason));
        disconnect();
    }

    public void disconnect() {
        try {
            removeSelf();
            connection.close();
        } catch (IOException e) {
            log.error("Error while closing connection.", e);
        }
    }

    public boolean sendPacket(SendablePacket packet) {
        ServerPacketType packetType = ServerPacketType.fromByte(packet.getType());

        if(server.printSentPackets() && packetType != ServerPacketType.Ping) {
            log.debug("[CLIENT] Sent packet: {}", packetType);
        }

        if(server.printCryptography()) {
            log.debug("---> [CLIENT] Clear packet {} : {}", packet.getData().length, Arrays.toString(packet.getData()));
        }
        if(!encrypt(packet.getData(), packet.getData().length)) {
            return false;
        }
        if(server.printCryptography()) {
            log.debug("---> [CLIENT] Encrypted packet {} : {}", packet.getData().length, Arrays.toString(packet.getData()));
        }

        try {
            synchronized (out) {
                int len = packet.getData().length + 2;
                out.write((byte)(len) & 0xff);
                out.write((byte)((len) >> 8) & 0xff);

                for (byte b : packet.getData()) {
                    out.write(b & 0xFF);
                }
                out.flush();
            }

            return true;
        } catch (IOException e) {
            log.warn("Trying to send packet to a closed game client.");
        }

        return false;
    }

    void handlePacket(byte[] data) {
        ThreadPoolManagerService.getInstance().handlePacket(new ClientPacketHandler(this, data));
    }

    public void setLastEcho(long lastEcho, Timer watchDog) {
        if(this.watchDog != null) {
            watchDog.stop();
        }

        this.lastEcho = lastEcho;
        this.watchDog = watchDog;
    }

    void authenticate() {
        log.debug("Authenticating new player.");

    }

    private void removeSelf() {
        if (authenticated) {
            authenticated = false;

            if(!clientReady) {
                return;
            }
        }

        LoginServerController.getInstance().removeClient(this);
        this.interrupt();
    }

    public RSAPrivateKey getRSAPrivateKey() {
        return (RSAPrivateKey) scrambledPair.getPair().getPrivate();
    }

    public boolean decrypt(byte[] packetData, int size) {
        try {
            if (!loginCrypt.decrypt(packetData, 0, size)) {
                log.warn("Wrong checksum from client {}!", this);
                //super.getConnection().close((SendablePacket<L2LoginClient>) null);
                disconnect();
                return false;
            }
            return true;
        } catch (IOException ex) {
            log.warn("There has been an error decrypting message!", ex);
            //super.getConnection().close((SendablePacket<L2LoginClient>) null);
            disconnect();
            return false;
        }
    }

    public boolean encrypt(byte[] packetData, int size) {
        try {
            loginCrypt.encrypt(packetData, 0, size);
        } catch (IOException ex) {
            log.warn("There has been an error encrypting message!", ex);
            return false;
        }

        return true;
    }

    public void setLoginClientState(LoginClientState newState) {
        if(getLoginClientState() != newState) {
            loginClientState = newState;

            log.debug("Account {} state updated to {}.", getUsername(), newState);
        }
    }

    public void setCharsOnServ(int servId, int chars) {
        if (charsOnServers == null) {
            charsOnServers = new HashMap<>();
        }
        charsOnServers.put(servId, chars);

        if(charsOnServers.size() >= expectedCharacterCount) {
            if (server.showLicense()) {
                sendPacket(new LoginOkPacket(getSessionKey()));
            } else {
                sendPacket(new ServerListPacket(this));
            }
        }
    }

    public Map<Integer, Integer> getCharsOnServ() {
        return charsOnServers;
    }

    public void serCharsWaitingDelOnServ(int servId, long[] charsToDel) {
        if (charsToDelete == null) {
            charsToDelete = new HashMap<>();
        }
        charsToDelete.put(servId, charsToDel);
    }

    public Map<Integer, long[]> getCharsWaitingDelOnServ() {
        return charsToDelete;
    }
}
