package org.itxtech.nemisys.network.protocol.spp;

import java.util.UUID;

/**
 * @author CreeperFace
 */
public class PlayerSpawnPacket extends SynapseDataPacket {

    public static final byte NETWORK_ID = SynapseInfo.PLAYER_SPAWN_PACKET;

    public static final byte ACTION_SPAWN = 0;
    public static final byte ACTION_DESPAWN = 1;

    public UUID playerUid;
    public long id;
    public byte action;

    @Override
    public byte pid() {
        return NETWORK_ID;
    }

    @Override
    public void encode() {
        this.reset();
        this.putUUID(this.playerUid);
        this.putLong(this.id);
        this.putByte(action);
    }

    @Override
    public void decode() {
        this.playerUid = getUUID();
        this.id = this.getLong();
        this.action = (byte) getByte();
    }
}
