package org.itxtech.nemisys.network.protocol.mcpe;

import org.itxtech.nemisys.math.BlockVector3;
import org.itxtech.nemisys.multiversion.ProtocolGroup;

/**
 * @author Nukkit Project Team
 */
public class AddPaintingPacket extends DataPacket {

    public static final byte NETWORK_ID = ProtocolInfo.ADD_PAINTING_PACKET;

    public long entityUniqueId;
    public long entityRuntimeId;
    public int x;
    public int y;
    public int z;
    public int direction;
    public String title;

    @Override
    public void decode(ProtocolGroup group) {
        if (group.ordinal() < ProtocolGroup.PROTOCOL_12.ordinal())
            return;
        entityUniqueId = getEntityUniqueId();
        entityRuntimeId = getEntityRuntimeId();
        BlockVector3 pos = getBlockVector3();

        x = pos.x;
        y = pos.y;
        z = pos.z;

        direction = getVarInt();
        title = getString();
    }

    @Override
    public void encode(ProtocolGroup group) {
        if (group.ordinal() < ProtocolGroup.PROTOCOL_12.ordinal())
            return;

        this.reset(group);
        this.putEntityUniqueId(this.entityUniqueId);
        this.putEntityRuntimeId(this.entityRuntimeId);
        this.putBlockVector3(this.x, this.y, this.z);
        this.putVarInt(this.direction);
        this.putString(this.title);
    }

    @Override
    public byte pid() {
        return NETWORK_ID;
    }

}
