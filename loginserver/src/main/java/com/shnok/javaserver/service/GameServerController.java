package com.shnok.javaserver.service;

import com.shnok.javaserver.db.entity.DBGameServer;
import com.shnok.javaserver.db.repository.GameServerRepository;
import com.shnok.javaserver.model.GameServerInfo;
import com.shnok.javaserver.security.Rnd;
import com.shnok.javaserver.util.HexUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
@Getter
@Setter
public class GameServerController {
    private static final Map<Integer, GameServerInfo> GAME_SERVER_TABLE = new HashMap<>();
    private static final int KEYS_SIZE = 10;
    private KeyPair[] keyPairs;
    private static GameServerController instance;

    public static GameServerController getInstance() {
        if (instance == null) {
            instance = new GameServerController();
        }
        return instance;
    }

    public GameServerController() {
        loadRegisteredGameServers();
        log.info("{}: Loaded {} registered Game Servers.", getClass().getSimpleName(), GAME_SERVER_TABLE.size());

        initRSAKeys();
        log.info("{}: Cached {} RSA keys for Game Server communication.", getClass().getSimpleName(), keyPairs.length);
    }

    private void initRSAKeys() {
        try {
            final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(new RSAKeyGenParameterSpec(512, RSAKeyGenParameterSpec.F4));
            keyPairs = new KeyPair[KEYS_SIZE];
            for (int i = 0; i < KEYS_SIZE; i++) {
                keyPairs[i] = keyGen.genKeyPair();
            }
        } catch (Exception e) {
            log.error("{}: Error loading RSA keys for Game Server communication!", getClass().getSimpleName(), e);
        }
    }

    private void loadRegisteredGameServers() {
        List<DBGameServer> gameServerList = GameServerRepository.getInstance().getAllGameServers();
        gameServerList.forEach(gameServer -> {
            GAME_SERVER_TABLE.put(gameServer.getServerId(), new GameServerInfo(gameServer.getServerId(),
                    HexUtils.stringToHex(gameServer.getHexId())));
        });

        log.info("Loaded {} registered gameserver(s) from DB.", gameServerList.size());
    }

    /**
     * Gets the registered game servers.
     * @return the registered game servers
     */
    public Map<Integer, GameServerInfo> getRegisteredGameServers() {
        return GAME_SERVER_TABLE;
    }

    /**
     * Gets the registered game server by id.
     * @param id the game server ID
     * @return the registered game server by id
     */
    public GameServerInfo getRegisteredGameServerById(int id) {
        return GAME_SERVER_TABLE.get(id);
    }

    /**
     * Checks for registered game server on id.
     * @param id the id
     * @return true, if successful
     */
    public boolean hasRegisteredGameServerOnId(int id) {
        return GAME_SERVER_TABLE.containsKey(id);
    }

    /**
     * Register with first available id.
     * @param gsi the game server information DTO
     * @return true, if successful
     */
    public boolean registerWithFirstAvailableId(GameServerInfo gsi) {
        // avoid two servers registering with the same "free" id
        synchronized (GAME_SERVER_TABLE) {
            for (int i = 0; i < 16; i++) {
                if (!GAME_SERVER_TABLE.containsKey(i)) {
                    GAME_SERVER_TABLE.put(i, gsi);
                    gsi.setId(i);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Register a game server.
     * @param id the id
     * @param gsi the gsi
     * @return true, if successful
     */
    public boolean register(int id, GameServerInfo gsi) {
        // avoid two servers registering with the same id
        synchronized (GAME_SERVER_TABLE) {
            if (!GAME_SERVER_TABLE.containsKey(id)) {
                GAME_SERVER_TABLE.put(id, gsi);
                return true;
            }
        }
        return false;
    }

    /**
     * Wrapper method.
     * @param gsi the game server info DTO.
     */
    public void registerServerOnDB(GameServerInfo gsi) {
        registerServerOnDB(gsi.getHexId(), gsi.getId(), gsi.getHostname());
    }

    /**
     * Register server on db.
     * @param hexId the hex id
     * @param id the id
     * @param externalHost the external host
     */
    public void registerServerOnDB(byte[] hexId, int id, String externalHost) {
        register(id, new GameServerInfo(id, hexId));

        DBGameServer gameServer = new DBGameServer();
        gameServer.setHexId(HexUtils.hexToString(hexId));
        gameServer.setServerId(id);
        gameServer.setHost(externalHost);

        GameServerRepository.getInstance().addGameServer(gameServer);
    }

    /**
     * Gets the key pair.
     * @return a random key pair.
     */
    public KeyPair getKeyPair() {
        return keyPairs[Rnd.nextInt(10)];
    }
}
