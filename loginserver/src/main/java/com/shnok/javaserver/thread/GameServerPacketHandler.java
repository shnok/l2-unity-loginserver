package com.shnok.javaserver.thread;

import com.shnok.javaserver.dto.internal.gameserverpackets.*;
import com.shnok.javaserver.dto.internal.loginserverpackets.AuthResponsePacket;
import com.shnok.javaserver.dto.internal.loginserverpackets.PlayerAuthResponsePacket;
import com.shnok.javaserver.enums.GameServerState;
import com.shnok.javaserver.enums.LoginServerFailReason;
import com.shnok.javaserver.enums.packettypes.internal.GameServerPacketType;
import com.shnok.javaserver.model.GameServerInfo;
import com.shnok.javaserver.model.SessionKey;
import com.shnok.javaserver.security.NewCrypt;
import com.shnok.javaserver.service.GameServerController;
import com.shnok.javaserver.service.LoginServerController;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;
import java.util.List;

import static com.shnok.javaserver.config.Configuration.server;
import static com.shnok.javaserver.dto.internal.gameserverpackets.ServerStatusPacket.MAX_PLAYERS;
import static com.shnok.javaserver.dto.internal.gameserverpackets.ServerStatusPacket.SERVER_LIST_STATUS;
import static com.shnok.javaserver.enums.GameServerState.BF_CONNECTED;

@Log4j2
public class GameServerPacketHandler extends Thread {
    private final GameServerThread gameserver;
    private final byte[] data;

    public GameServerPacketHandler(GameServerThread gameserver, byte[] data) {
        this.gameserver = gameserver;
        this.data = data;
    }

    @Override
    public void run() {
        handle();
    }

    public void handle() {
        if(server.printCryptography()) {
            log.debug("<--- [GAME] Encrypted packet {} : {}", data.length, Arrays.toString(data));
        }
        gameserver.getBlowfish().decrypt(data, 0, data.length);
        if(server.printCryptography()) {
            log.debug("<--- [GAME] Decrypted packet {} : {}", data.length, Arrays.toString(data));
        }

        if(!NewCrypt.verifyChecksum(data)) {
            log.warn("Packet's checksum is wrong.");
            return;
        }

        GameServerPacketType type = GameServerPacketType.fromByte(data[0]);

        if(server.printReceivedPackets()) {
            log.debug("[GAME] Received packet: {}", type);
        }

        GameServerState state = gameserver.getLoginConnectionState();
        switch (state) {
            case CONNECTED:
                if(type == GameServerPacketType.BlowFishKey) {
                    onReceiveBlowfishKey();
                } else {
                    log.warn("Unknown Opcode {} in state {} from game server, closing connection!",
                            type, state);
                    gameserver.forceClose(LoginServerFailReason.NOT_AUTHED.getCode());
                }
                break;
            case BF_CONNECTED:
                if(type == GameServerPacketType.AuthRequest) {
                    onReceiveAuthRequest();
                } else {
                    log.warn("Unknown Opcode {} in state {} from game server, closing connection!",
                            type, state);
                    gameserver.forceClose(LoginServerFailReason.NOT_AUTHED.getCode());
                }
                break;
            case AUTHED:
                if(type == GameServerPacketType.ServerStatus) {
                    onReceiveServerStatus();
                }
                if(type == GameServerPacketType.PlayerInGame) {
                    onReceivePlayerInGame();
                }
                if(type == GameServerPacketType.PlayerLogout) {
                    onReceivePlayerLogout();
                }
                if(type == GameServerPacketType.ReplyCharacters) {
                    onReceiveCharacters();
                }
                if(type == GameServerPacketType.PlayerAuthRequest) {
                    onReceivePlayerAuthRequest();
                }
                break;
        }
    }

    private void onReceiveBlowfishKey() {
        BlowFishKeyPacket packet = new BlowFishKeyPacket(data, gameserver);

        gameserver.setBlowfish(new NewCrypt(packet.getBlowFishKey()));

        gameserver.setLoginConnectionState(BF_CONNECTED);
    }

    private void onReceiveAuthRequest() {
        GameServerAuthPacket packet = new GameServerAuthPacket(data);

        log.info("Auth request received.");

        if (handleRegProcess(packet)) {
            gameserver.sendPacket(new AuthResponsePacket(gameserver.getGameServerInfo().getId()));
            gameserver.setLoginConnectionState(GameServerState.AUTHED);

            log.info("Game Server {} enabled.", gameserver.getGameServerInfo().getId());
        }
    }

    private boolean handleRegProcess(GameServerAuthPacket packet) {
        GameServerController gameServerController = GameServerController.getInstance();

        GameServerInfo gsi = gameServerController.getRegisteredGameServerById(packet.getId());

        // is there a game server registered with this id?
        if (gsi != null) {
            log.debug("A gameserver is already registered with ID {}", packet.getId());
            // does the hex id match?
            if (Arrays.equals(gsi.getHexId(), packet.getHexId())) {
                log.debug("Gameserver hexId matches {}", Arrays.toString(packet.getHexId()));
                // check to see if this GS is already connected
                synchronized (gsi) {
                    if (gsi.isAuthed()) {
                        log.error("Gameserver already authed");
                        gameserver.forceClose(LoginServerFailReason.REASON_ALREADY_LOGGED_IN.getCode());
                        return false;
                    }
                    gameserver.attachGameServerInfo(gsi, packet.getPort(), packet.getHost(), packet.getMaxPlayer());
                }
            } else {
                log.debug("There is already a server registered with the desired id and different hex id.");
                log.debug("Old hexid: {}", gsi.getHexId());
                log.debug("New hexid: {}", packet.getHexId());
                // there is already a server registered with the desired id and different hex id
                // try to register this one with an alternative id
                if (server.acceptNewGameserver() && packet.isAcceptAlternate()) {
                    log.debug("Trying to register this one with an alternative id.");
                    gsi = new GameServerInfo(packet.getId(), packet.getHexId(), gameserver);
                    if (gameServerController.registerWithFirstAvailableId(gsi)) {
                        gameserver.attachGameServerInfo(gsi, packet.getPort(), packet.getHost(), packet.getMaxPlayer());
                        GameServerController.getInstance().registerServerOnDB(gsi);
                    } else {
                        log.debug("No free gameserver id remaining.");
                        gameserver.forceClose(LoginServerFailReason.REASON_NO_FREE_ID.getCode());
                        return false;
                    }
                } else {
                    // server id is already taken, and we cant get a new one for you
                    log.error("Server id is already taken, and we cant get a new one for you.");
                    gameserver.forceClose(LoginServerFailReason.REASON_WRONG_HEXID.getCode());
                    return false;
                }
            }
        } else {
            log.debug("A new unregistered gameserver received with ID: {}.", packet.getId());
            // can we register on this id?
            if (server.acceptNewGameserver()) {
                gsi = new GameServerInfo(packet.getId(), packet.getHexId(), gameserver);
                if (gameServerController.register(packet.getId(), gsi)) {
                    gameserver.attachGameServerInfo(gsi, packet.getPort(), packet.getHost(), packet.getMaxPlayer());
                    GameServerController.getInstance().registerServerOnDB(gsi);
                } else {
                    log.error("ID {} is not available anymore.", packet.getId());
                    // someone took this ID meanwhile
                    gameserver.forceClose(LoginServerFailReason.REASON_ID_RESERVED.getCode());
                    return false;
                }
            } else {
                log.error("Wrong gameserver hexId: {}.", Arrays.toString(packet.getHexId()));
                gameserver.forceClose(LoginServerFailReason.REASON_WRONG_HEXID.getCode());
                return false;
            }
        }
        return true;
    }

    private void onReceiveServerStatus() {
        ServerStatusPacket packet = new ServerStatusPacket(data);

        GameServerInfo gsi = gameserver.getGameServerInfo();
        for (ServerStatusPacket.Attribute attribute: packet.getAttributes()) {
            switch (attribute.id) {
                case SERVER_LIST_STATUS:
                    gsi.setStatus(attribute.value);
                    break;
                case MAX_PLAYERS:
                    gsi.setMaxPlayers(attribute.value);
                    break;
            }
        }
    }

    private void onReceivePlayerInGame() {
        PlayerInGamePacket packet = new PlayerInGamePacket(data);

        List<String> loggedUsers = packet.getLoggedUsers();

        loggedUsers.forEach((account) -> {
            gameserver.addAccountOnGameServer(account);
            log.info("Account {} logged in Game Server {}[{}].", account, gameserver.getServerName(),
                    gameserver.getServerId());
        });
    }

    private void onReceivePlayerLogout() {
        PlayerLogoutPacket packet = new PlayerLogoutPacket(data);

        gameserver.removeAccountOnGameServer(packet.getPlayer());

        log.info("Player {} logged out from game server {}[{}].", packet.getPlayer(), gameserver.getServerName(),
                gameserver.getServerId());

    }

    private void onReceiveCharacters() {
        ReplyCharactersPacket packet = new ReplyCharactersPacket(data);

        log.info("Received {} character(s) for account {}.", packet.getCharCount(), packet.getAccount());
        LoginServerController.getInstance().setCharactersOnServer(packet.getAccount(),
                packet.getCharCount(), gameserver.getServerId());
    }

    private void onReceivePlayerAuthRequest() {
        PlayerAuthRequestPacket packet = new PlayerAuthRequestPacket(data);

        SessionKey sessionKey = new SessionKey(packet.getLoginOkID1(), packet.getLoginOkID2(),
                packet.getPlayOkID1(), packet.getPlayOkID2());

        PlayerAuthResponsePacket authResponse;
        SessionKey key = LoginServerController.getInstance().getKeyForAccount(packet.getAccount());
        log.debug("Received session key: {}.", sessionKey);
        log.debug("Local session key: {}.", key);
        if ((key != null) && key.equals(sessionKey)) {
            LoginServerController.getInstance().removeAuthedClient(packet.getAccount());
            authResponse = new PlayerAuthResponsePacket(packet.getAccount(), true);
        } else {
            authResponse = new PlayerAuthResponsePacket(packet.getAccount(), false);
        }

        gameserver.sendPacket(authResponse);
    }
}