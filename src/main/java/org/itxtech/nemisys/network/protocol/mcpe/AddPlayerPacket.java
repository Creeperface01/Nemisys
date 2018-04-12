package org.itxtech.nemisys.network.protocol.mcpe;

import org.itxtech.nemisys.multiversion.ProtocolGroup;

import java.util.UUID;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class AddPlayerPacket extends DataPacket {
    public static final byte NETWORK_ID = ProtocolInfo.ADD_PLAYER_PACKET;

    @Override
    public byte pid() {
        return NETWORK_ID;
    }

    public UUID uuid;
    public String username;
    public long entityUniqueId;
    public long entityRuntimeId;
    public float x;
    public float y;
    public float z;
    public float speedX;
    public float speedY;
    public float speedZ;
    public float pitch;
    public float yaw;

    @Override
    public void decode(ProtocolGroup group) {
        uuid = getUUID();
        username = getString();

        if (group.ordinal() >= ProtocolGroup.PROTOCOL_1213.ordinal()) {
            getString(); //third party name
            getVarInt(); //platform id
        }

        entityUniqueId = getEntityUniqueId();
        entityRuntimeId = getEntityRuntimeId();
    }

    @Override
    public void encode(ProtocolGroup group) {
        this.reset();
        this.putUUID(this.uuid);
        this.putString(this.username);

        if (group.ordinal() >= ProtocolGroup.PROTOCOL_1213.ordinal()) {
            this.putString(""); //third party name
            this.putVarInt(0); //platform id
        }

        this.putEntityUniqueId(this.entityUniqueId);
        this.putEntityRuntimeId(this.entityRuntimeId);

        if (group.ordinal() >= ProtocolGroup.PROTOCOL_1213.ordinal()) {
            this.putString(""); //platform chat id
        }

        this.putVector3f(this.x, this.y, this.z);
        this.putVector3f(this.speedX, this.speedY, this.speedZ);
        this.putLFloat(this.pitch);
        this.putLFloat(this.yaw); //TODO headrot
        this.putLFloat(this.yaw);
    }
}
