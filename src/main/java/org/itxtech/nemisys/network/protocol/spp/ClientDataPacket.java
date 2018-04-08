package org.itxtech.nemisys.network.protocol.spp;

import org.itxtech.nemisys.utils.ClientData;
import org.itxtech.nemisys.utils.ClientData.Entry;

/**
 * @author CreeperFace
 */
public class ClientDataPacket extends SynapseDataPacket {

    public static final byte NETWORK_ID = SynapseInfo.CLIENT_DATA_PACKET;

    public ClientData.Entry[] entries;

    @Override
    public byte pid() {
        return NETWORK_ID;
    }

    @Override
    public void encode() {
        this.reset();
        this.putUnsignedVarInt(entries.length);

        for (int i = 0; i < entries.length; i++) {
            ClientData.Entry entry = entries[i];

            putString(entry.getIp());
            putInt(entry.getPort());
            putInt(entry.getPlayerCount());
            putInt(entry.getMaxPlayers());
            putString(entry.getDescription());
            putFloat(entry.getTicksPerSecond());
            putFloat(entry.getTickUsage());
            putLong(entry.getUpTime());
            putString(entry.getCustomData());
        }
    }

    @Override
    public void decode() {
        int len = (int) getUnsignedVarInt();

        entries = new Entry[len];

        for (int i = 0; i < len; i++) {
            String ip = getString();
            int port = getInt();
            int players = getInt();
            int maxPlayers = getInt();
            String desc = getString();
            float tps = getFloat();
            float load = getFloat();
            long uptime = getLong();
            String customData = getString();

            entries[i] = new Entry(ip, port, players, maxPlayers, desc, tps, load, uptime, customData);
        }
    }
}
