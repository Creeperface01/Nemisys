package org.itxtech.nemisys.network.protocol.mcpe;

import org.itxtech.nemisys.multiversion.ProtocolGroup;

/**
 * @author CreeperFace
 */
public class SetDisplayObjectivePacket extends DataPacket {

    public static final byte NETWORK_ID = 0x6b;

    public String objective;

    @Override
    public byte pid() {
        return NETWORK_ID;
    }

    @Override
    public void encode(ProtocolGroup protocol) {

    }

    @Override
    public void decode(ProtocolGroup protocol) {
        getString(); // display slot
        objective = getString();
    }
}
