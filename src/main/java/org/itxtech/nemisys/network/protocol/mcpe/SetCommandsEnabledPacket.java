package org.itxtech.nemisys.network.protocol.mcpe;

public class SetCommandsEnabledPacket extends DataPacket {

    public static final byte NETWORK_ID = ProtocolInfo.SET_COMMANDS_ENABLED_PACKET;

    public boolean enabled;

    @Override
    public byte pid() {
        return NETWORK_ID;
    }

    @Override
    public void decode() {

    }

    @Override
    public void encode() {
        this.reset();
        this.putBoolean(this.enabled);
    }
}
