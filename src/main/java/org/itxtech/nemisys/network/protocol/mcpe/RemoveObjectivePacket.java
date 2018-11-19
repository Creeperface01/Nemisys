package org.itxtech.nemisys.network.protocol.mcpe;

import org.itxtech.nemisys.multiversion.ProtocolGroup;

/**
 * @author CreeperFace
 */
public class RemoveObjectivePacket extends DataPacket {

    public static final byte NETWORK_ID = 0x6a;

    public String objective;

    @Override
    public byte pid() {
        return NETWORK_ID;
    }

    @Override
    public void encode(ProtocolGroup protocol) {
        reset(protocol);
        putString(objective);
    }

    @Override
    public void decode(ProtocolGroup protocol) {
        objective = getString();
    }
}
