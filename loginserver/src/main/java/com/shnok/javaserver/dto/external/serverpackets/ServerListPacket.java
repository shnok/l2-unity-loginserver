package com.shnok.javaserver.dto.external.serverpackets;

import com.shnok.javaserver.dto.SendablePacket;
import com.shnok.javaserver.enums.packettypes.external.ServerPacketType;
import com.shnok.javaserver.model.GameServerInfo;
import com.shnok.javaserver.service.GameServerController;
import com.shnok.javaserver.thread.LoginClientThread;
import lombok.extern.log4j.Log4j2;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Log4j2
public class ServerListPacket extends SendablePacket {

    private List<ServerData> servers;
    private int lastServer;
    private Map<Integer, Integer> charsOnServers;

//    private Map<Integer, long[]> charsToDelete;

    public ServerListPacket(LoginClientThread client) {
        super(ServerPacketType.ServerList.getValue());

        fillServerList(client);

        writeB((byte) servers.size());
        writeB((byte) lastServer);

        for (ServerData server : servers) {
            writeB((byte) server.serverId); // server id

            writeB((byte) (server.ip[0] & 0xff));
            writeB((byte) (server.ip[1] & 0xff));
            writeB((byte) (server.ip[2] & 0xff));
            writeB((byte) (server.ip[3] & 0xff));

            writeI(server.port);
            writeI(server.currentPlayers);
            writeI(server.maxPlayers);
            writeB((byte) server.status);
        }

        if (charsOnServers != null) {
            writeB((byte) charsOnServers.size());
            for (int servId : charsOnServers.keySet()) {
                writeB((byte) servId);
                writeB(charsOnServers.get(servId).byteValue());

                log.debug("Client has {} character(s) on server {}", charsOnServers.get(servId), servId);

//                if ((charsToDelete == null) || !charsToDelete.containsKey(servId)) {
//                    writeB((byte) 0x00);
//                } else {
//                    writeB((byte) charsToDelete.get(servId).length);
//                    for (long deleteTime : charsToDelete.get(servId)) {
//                        writeI((int) ((deleteTime - System.currentTimeMillis()) / 1000));
//                    }
//                }
            }
        } else {
            writeB((byte) 0x00);
        }

        buildPacket();
    }

    private void fillServerList(LoginClientThread client) {
        servers = new ArrayList<>();
        lastServer = client.getLastGameserver();
        for (GameServerInfo gsi : GameServerController.getInstance().getRegisteredGameServers().values()) {
            servers.add(new ServerData(client, gsi));
        }

        charsOnServers = client.getCharsOnServ();

        if(charsOnServers != null) {
            log.debug("Client as {} character(s) on all servers.", charsOnServers.size());
        }
    }

    static class ServerData {
        protected byte[] ip;
        protected final int port;
        protected final int currentPlayers;
        protected final int maxPlayers;
        protected boolean clock;
        protected final int status;
        protected final int serverId;

        ServerData(LoginClientThread client, GameServerInfo gsi) {
            try {
                ip = InetAddress.getByName(gsi.getHostname()).getAddress();
            } catch (UnknownHostException ex) {
                log.warn("There has been an error getting IP from host!", ex);
                ip = new byte[4];
                ip[0] = 127;
                ip[1] = 0;
                ip[2] = 0;
                ip[3] = 1;
            }

            port = gsi.getPort();
            currentPlayers = gsi.getCurrentPlayerCount();
            maxPlayers = gsi.getMaxPlayers();
            status = gsi.getStatus();
            serverId = gsi.getId();
        }
    }
}
