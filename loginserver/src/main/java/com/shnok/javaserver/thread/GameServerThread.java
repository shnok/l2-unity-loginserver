package com.shnok.javaserver.thread;

import com.shnok.javaserver.dto.SendablePacket;
import com.shnok.javaserver.dto.internal.loginserverpackets.InitLSPacket;
import com.shnok.javaserver.dto.internal.loginserverpackets.KickPlayerPacket;
import com.shnok.javaserver.dto.internal.loginserverpackets.LoginServerFailPacket;
import com.shnok.javaserver.dto.internal.loginserverpackets.RequestCharactersPacket;
import com.shnok.javaserver.enums.GameServerState;
import com.shnok.javaserver.enums.packettypes.internal.LoginServerPacketType;
import com.shnok.javaserver.model.GameServerInfo;
import com.shnok.javaserver.security.NewCrypt;
import com.shnok.javaserver.service.GameServerController;
import com.shnok.javaserver.service.GameServerListenerService;
import com.shnok.javaserver.service.ThreadPoolManagerService;
import com.shnok.javaserver.util.ServerNameDAO;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.shnok.javaserver.config.Configuration.server;

@Getter
@Setter
@Log4j2
public class GameServerThread extends Thread {
    private InputStream in;
    private OutputStream out;
    private Socket connection;
    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
    private NewCrypt blowfish;
    private GameServerState loginConnectionState = GameServerState.CONNECTED;
    private final String connectionIp;
    private GameServerInfo gameServerInfo;

    /**
     * Authed Clients on a GameServer
     */
    private final Set<String> accountsOnGameServer = ConcurrentHashMap.newKeySet();

    private String _connectionIPAddress;

    public GameServerThread(Socket con) {
        connection = con;
        connectionIp = con.getInetAddress().getHostAddress();

        try {
            in = connection.getInputStream();
            out = new BufferedOutputStream(connection.getOutputStream());
            log.debug("New gameserver connection: {}", connectionIp);
        } catch (IOException e) {
            e.printStackTrace();
        }

        KeyPair pair = GameServerController.getInstance().getKeyPair();
        privateKey = (RSAPrivateKey) pair.getPrivate();
        publicKey = (RSAPublicKey) pair.getPublic();
        blowfish = new NewCrypt("_;v.]05-31!|+-%xT!^[$\00");
        setName(getClass().getSimpleName() + "-" + Thread.currentThread().getId() + "@" + connectionIp);
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
            sendPacket(new InitLSPacket(publicKey.getModulus().toByteArray()));

            for (; ; ) {
                lengthLo = in.read();
                lengthHi = in.read();
                length = (lengthHi * 256) + lengthLo;

                if ((lengthHi < 0) || connection.isClosed()) {
                    log.warn("Gameserver terminated the connection!");
                    break;
                }

                length = length - 2;

                byte[] data = new byte[length];

                int receivedBytes = 0;
                int newBytes = 0;
                while ((newBytes != -1) && (receivedBytes < (length))) {
                    newBytes = in.read(data, 0, length);
                    receivedBytes = receivedBytes + newBytes;
                }

                handlePacket(data);
            }
        } catch (Exception e) {
        } finally {
            log.info("Gameserver {} connection closed.", connectionIp);
            disconnect();
        }
    }

    private void handlePacket(byte[] data) {
        ThreadPoolManagerService.getInstance().handlePacket(new GameServerPacketHandler(this, data));
    }

    public void sendPacket(SendablePacket packet) {
        LoginServerPacketType packetType = LoginServerPacketType.fromByte(packet.getType());

        if(server.printSentPackets()) {
            log.debug("[GAME] Sent packet: {}", packetType);
        }

        NewCrypt.appendChecksum(packet.getData());

        if(server.printCryptography()) {
            log.debug("---> [GAME] Clear packet {} : {}", packet.getData().length, Arrays.toString(packet.getData()));
        }
        blowfish.crypt(packet.getData(), 0, packet.getData().length);
        if(server.printCryptography()) {
            log.debug("---> [GAME] Encrypted packet {} : {}", packet.getData().length, Arrays.toString(packet.getData()));
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

        } catch (IOException e) {
            log.warn("Trying to send packet to a gameserver connection.");
        }
    }

    public void disconnect() {
        try {
            if (gameServerInfo.isAuthed()) {
                gameServerInfo.setDown();

                log.info("Server {}[{}] is now disconnected.", ServerNameDAO.getServer(gameServerInfo.getId()),
                        gameServerInfo.getId());
            }

            GameServerListenerService.getInstance().removeGameServer(this);

            connection.close();
        } catch (IOException e) {
            log.error("Error while closing connection.", e);
        }
    }

    public void attachGameServerInfo(GameServerInfo gsi, int port, String host, int maxPlayers) {
        log.debug("Attaching gameserver with ID: {}.", gsi.getId());

        setGameServerInfo(gsi);
        gsi.setGameServerThread(this);
        gsi.setPort(port);
        setGameHosts(host);
        gsi.setMaxPlayers(maxPlayers);
        gsi.setAuthed(true);
    }

    public void setGameHosts(String hosts) {
        log.info("Updated game server {}[{}] IPs.", gameServerInfo.getName(), gameServerInfo.getId());

        if (!hosts.equals("*"))
        {
            try
            {
                gameServerInfo.setHostname(InetAddress.getByName(hosts).getHostAddress());
            }
            catch (UnknownHostException e)
            {
                log.error("Couldn't resolve hostname '{}'.", hosts, e);
                gameServerInfo.setHostname(connectionIp);
            }
        }
        else
            gameServerInfo.setHostname(connectionIp);

        log.info("Gameserver hostname updated to [{}].", gameServerInfo.getHostname());
    }

    public void forceClose(int reason) {
        sendPacket(new LoginServerFailPacket(reason));

        disconnect();
    }

    public static boolean isBannedGameServerIP(String ipAddress) {
        return false;
    }

    public int getPlayerCount() {
        return accountsOnGameServer.size();
    }

    public boolean hasAccountOnGameServer(String account) {
        return accountsOnGameServer.contains(account);
    }

    public void addAccountOnGameServer(String account) {
        accountsOnGameServer.add(account);
    }

    public void removeAccountOnGameServer(String account) {
        accountsOnGameServer.remove(account);
    }

    public void setBlowfish(NewCrypt newCrypt) {
        log.info("New BlowFish key received, Blowfish Engine initialized.");
        blowfish = newCrypt;
    }

    public void setLoginConnectionState(GameServerState state) {
        log.info("New gameserver connection state: {}", state);
        loginConnectionState = state;
    }

    public int getServerId() {
        return gameServerInfo.getId();
    }

    public String getServerName() {
        return gameServerInfo.getName();
    }

    public void requestCharacters(String account) {
        sendPacket(new RequestCharactersPacket(account));
    }

    public void kickPlayer(String account) {
        sendPacket(new KickPlayerPacket(account));
    }
}
