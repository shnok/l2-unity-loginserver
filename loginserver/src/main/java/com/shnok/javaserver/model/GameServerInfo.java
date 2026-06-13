package com.shnok.javaserver.model;

import com.shnok.javaserver.enums.ServerStatus;
import com.shnok.javaserver.thread.GameServerThread;
import com.shnok.javaserver.util.ServerNameDAO;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

@Getter
@Setter
@Log4j2
public class GameServerInfo {
    // auth
    private int id;
    private final byte[] hexId;
    private boolean isAuthed;
    // status
    private GameServerThread gameServerThread;
    private int status;
    // network
    private String hostname;
    private int port;
    // config
    private final boolean isPvp = true;
    private int maxPlayers;

    /**
     * Instantiates a new game server info.
     * @param id the id
     * @param hexId the hex id
     * @param gameServerThread the gst
     */
    public GameServerInfo(int id, byte[] hexId, GameServerThread gameServerThread) {
        this.id = id;
        this.hexId = hexId;
        this.gameServerThread = gameServerThread;
        status = ServerStatus.STATUS_DOWN.getCode();
    }

    /**
     * Instantiates a new game server info.
     * @param id the id
     * @param hexId the hex id
     */
    public GameServerInfo(int id, byte[] hexId) {
        this(id, hexId, null);
    }

    public String getName() {
        return ServerNameDAO.getServer(id);
    }

    public void setStatus(int value) {
        status = value;
        log.info("Server {}[{}] status changed to {}.", getName(),
                getId(), getStatusName());
    }

    public String getStatusName() {
         switch (status) {
            case 0: return "Light";
            case 1: return "Normal";
            case 2: return "Heavy";
            case 3: return "Full";
            case 4: return "Down";
            case 5: return "GM Only";
            default: return "Unknown";
        }
    }

    /**
     * Gets the current player count.
     * @return the current player count
     */
    public int getCurrentPlayerCount() {
        if (gameServerThread == null) {
            return 0;
        }
        return gameServerThread.getPlayerCount();
    }

    /**
     * Sets the down.
     */
    public void setDown() {
        setAuthed(false);
        setPort(0);
        setGameServerThread(null);
        setStatus(ServerStatus.STATUS_DOWN.getCode());
    }
}
