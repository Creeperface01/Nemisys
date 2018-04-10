package org.itxtech.nemisys.network.protocol.mcpe;

import org.itxtech.nemisys.multiversion.ProtocolGroup;

/**
 * Created on 15-10-13.
 */
public class TextPacket extends DataPacket {

    public static final byte NETWORK_ID = ProtocolInfo.TEXT_PACKET;

    @Override
    public byte pid() {
        return NETWORK_ID;
    }

    public static final byte TYPE_RAW = 0;
    public static final byte TYPE_CHAT = 1;
    public static final byte TYPE_TRANSLATION = 2;
    public static final byte TYPE_POPUP = 3;
    public static final byte TYPE_TIP = 4;
    public static final byte TYPE_SYSTEM = 5;
    public static final byte TYPE_WHISPER = 6;
    public static final byte TYPE_ANNOUNCEMENT = 7;

    public byte type;
    public String source = "";
    public String message = "";
    public String[] parameters = new String[0];
    public boolean isLocalized = false;

    @Override
    public void decode(ProtocolGroup protocol) {
        this.type = (byte) getByte();
        this.isLocalized = this.getBoolean();
        switch (type) {
            case TYPE_POPUP:
            case TYPE_CHAT:
            case TYPE_WHISPER:
            case TYPE_ANNOUNCEMENT:
                this.source = this.getString();

                if (protocol.ordinal() >= ProtocolGroup.PROTOCOL_1213.ordinal()) {
                    getString();
                    getVarInt();
                }
            case TYPE_RAW:
            case TYPE_TIP:
            case TYPE_SYSTEM:
                this.message = this.getString();
                break;

            case TYPE_TRANSLATION:
                this.message = this.getString();
                int count = (int) this.getUnsignedVarInt();
                this.parameters = new String[count];
                for (int i = 0; i < count; i++) {
                    this.parameters[i] = this.getString();
                }
        }

        if (protocol.ordinal() >= ProtocolGroup.PROTOCOL_1213.ordinal()) {
            getString();
        }
    }

    @Override
    public void encode(ProtocolGroup protocol) {
        this.reset(protocol);
        this.putByte(this.type);
        this.putBoolean(this.isLocalized);
        switch (this.type) {
            case TYPE_POPUP:
            case TYPE_CHAT:
            case TYPE_WHISPER:
            case TYPE_ANNOUNCEMENT:
                this.putString(this.source);

                if (protocol.ordinal() >= ProtocolGroup.PROTOCOL_1213.ordinal()) {
                    putString(""); //third party name
                    putVarInt(0); //platform id
                }
            case TYPE_RAW:
            case TYPE_TIP:
            case TYPE_SYSTEM:
                this.putString(this.message);
                break;
            case TYPE_TRANSLATION:
                this.putString(this.message);

                this.putUnsignedVarInt(this.parameters.length);
                for (String parameter : this.parameters) {
                    this.putString(parameter);
                }
        }

        if (protocol.ordinal() >= ProtocolGroup.PROTOCOL_1213.ordinal()) {
            putString(""); //platform id
        }
    }

}
