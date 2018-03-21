package org.itxtech.nemisys.network.protocol.mcpe;

import org.itxtech.nemisys.raknet.protocol.EncapsulatedPacket;
import org.itxtech.nemisys.utils.BinaryStream;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class DataPacket extends BinaryStream implements Cloneable {

    public boolean isEncoded = false;
    public EncapsulatedPacket encapsulatedPacket;
    public byte reliability;
    public Integer orderIndex = null;
    public Integer orderChannel = null;
    private int channel = 0;

    public abstract byte pid();

    public void decode() {

    }

    public void encode() {

    }

    public void encode(int protocol) {
        this.encode();
    }

    public void decode(int protocol) {
        this.decode();
    }

    @Override
    public void reset() {
        reset(ProtocolInfo.CURRENT_PROTOCOL);
    }

    public void reset(int protocol) {
        super.reset();
        this.putByte(this.pid());

        if (protocol > 113) {
            this.putShort(0);
        }
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public DataPacket clean() {
        this.setBuffer(null);

        this.isEncoded = false;
        this.offset = 0;
        return this;
    }

    @Override
    public DataPacket clone() {
        try {
            return (DataPacket) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
