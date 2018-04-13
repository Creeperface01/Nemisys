package org.itxtech.nemisys.network.protocol.mcpe;

import org.itxtech.nemisys.multiversion.ProtocolGroup;
import org.itxtech.nemisys.utils.Skin;

import java.util.UUID;

/**
 * @author Nukkit Project Team
 */
public class PlayerListPacket extends DataPacket {

    public static final byte NETWORK_ID = ProtocolInfo.PLAYER_LIST_PACKET;

    public static final byte TYPE_ADD = 0;
    public static final byte TYPE_REMOVE = 1;

    public byte type;
    public Entry[] entries = new Entry[0];

    @Override
    public void decode(ProtocolGroup group) {
        type = (byte) getByte();

        int len = (int) getUnsignedVarInt();
        entries = new Entry[len];

        while (len-- > 0) {
            Entry entry = new Entry(getUUID());

            if (type == TYPE_ADD) {
                entry.entityId = getVarLong();
                entry.name = getString();

                if (group.ordinal() >= ProtocolGroup.PROTOCOL_1213.ordinal()) {
                    getString(); //third party name
                    getVarInt(); //platform id
                }

                entry.skin = getSkin(group);

                if (group.ordinal() >= ProtocolGroup.PROTOCOL_12.ordinal()) {
                    entry.geometryModel = getString();
                    entry.geometryData = getByteArray();
                    entry.xboxUserId = getString();
                }

                if (group.ordinal() >= ProtocolGroup.PROTOCOL_1213.ordinal()) {
                    this.getString(); //platform chat id
                }
            }

            entries[len] = entry;
        }
    }

    @Override
    public void encode(ProtocolGroup group) {
        this.reset(group);
        this.putByte(this.type);
        this.putUnsignedVarInt(this.entries.length);
        for (Entry entry : this.entries) {
            this.putUUID(entry.uuid);

            if (type == TYPE_ADD) {
                this.putVarLong(entry.entityId);
                this.putString(entry.name);

                if (group.ordinal() >= ProtocolGroup.PROTOCOL_1213.ordinal()) {
                    this.putString(""); //third party name
                    this.putVarInt(0); //platform id
                }

                this.putSkin(entry.skin, group);

                if (group.ordinal() >= ProtocolGroup.PROTOCOL_12.ordinal()) {
                    this.putString(entry.geometryModel);
                    this.putByteArray(entry.geometryData);
                    this.putString(entry.xboxUserId);
                }

                if (group.ordinal() >= ProtocolGroup.PROTOCOL_1213.ordinal()) {
                    this.putString(""); //platform chat id
                }
            }
        }

    }

    @Override
    public byte pid() {
        return NETWORK_ID;
    }

    public static class Entry {

        public final UUID uuid;
        public long entityId = 0;
        public String name = "";
        public Skin skin;
        public byte[] capeData = new byte[0];
        public String geometryModel = "";
        public byte[] geometryData = new byte[0];
        public String xboxUserId = "";

        public Entry(UUID uuid) {
            this.uuid = uuid;
        }

        public Entry(UUID uuid, long entityId, String name, Skin skin) {
            this(uuid, entityId, name, skin, "");
        }

        public Entry(UUID uuid, long entityId, String name, Skin skin, String xboxUserId) {
            this.uuid = uuid;
            this.entityId = entityId;
            this.name = name;
            this.skin = skin;
            this.capeData = skin.getCape().getData();
            this.geometryData = skin.geometry;
            this.geometryModel = skin.geometryName;
            this.xboxUserId = xboxUserId == null ? "" : xboxUserId;
        }
    }

}
