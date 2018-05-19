package org.itxtech.nemisys.command.data;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.itxtech.nemisys.multiversion.ProtocolGroup;

import java.util.EnumMap;
import java.util.Map;

/**
 * @author CreeperFace
 */
public enum CommandParamType {
    INT(0x01),
    FLOAT(0x02),
    VALUE(0x03),
    WILDCARD_INT(0x01, new MapBuilder().put(ProtocolGroup.PROTOCOL_14, 0x04).build()),
    TARGET(0x04, new MapBuilder().put(ProtocolGroup.PROTOCOL_14, 0x05).build()),
    WILDCARD_TARGET(0x04, new MapBuilder().put(ProtocolGroup.PROTOCOL_14, 0x06).build()),

    STRING(0x0d, new MapBuilder().put(ProtocolGroup.PROTOCOL_14, 0x0f).build()),
    POSITION(0x0e, new MapBuilder().put(ProtocolGroup.PROTOCOL_14, 0x10).build()),

    RAWTEXT(0x11, new MapBuilder().put(ProtocolGroup.PROTOCOL_14, 0x15).build()),

    TEXT(0x13, new MapBuilder().put(ProtocolGroup.PROTOCOL_14, 0x13).build()),

    JSON(0x16, new MapBuilder().put(ProtocolGroup.PROTOCOL_14, 0x18).build()),

    COMMAND(0x1d, new MapBuilder().put(ProtocolGroup.PROTOCOL_14, 0x1f).build());

    private static Map<ProtocolGroup, Int2ObjectMap<CommandParamType>> LOOKUP = new EnumMap<>(ProtocolGroup.class);

    static {
        for (ProtocolGroup group : ProtocolGroup.values()) {
            Int2ObjectMap<CommandParamType> params = new Int2ObjectOpenHashMap<>();

            for (CommandParamType type : values()) {
                params.put(type.getId(group), type);
            }

            LOOKUP.put(group, params);
        }
    }

    private final int id;
    private final Map<ProtocolGroup, Integer> ids;

    CommandParamType(int id) {
        this(id, new EnumMap<>(ProtocolGroup.class));
    }

    CommandParamType(int id, Map<ProtocolGroup, Integer> ids) {
        this.id = id;
        this.ids = ids;
    }

    public int getId(ProtocolGroup protocol) {
        return ids.getOrDefault(protocol, id);
    }

    public static CommandParamType of(int index, ProtocolGroup group) {
        return LOOKUP.getOrDefault(group, new Int2ObjectOpenHashMap<>()).getOrDefault(index, CommandParamType.RAWTEXT);
    }

    private static class MapBuilder {

        private EnumMap<ProtocolGroup, Integer> map = new EnumMap<>(ProtocolGroup.class);

        public MapBuilder put(ProtocolGroup group, Integer value) {
            map.put(group, value);
            return this;
        }

        Map<ProtocolGroup, Integer> build() {
            return map;
        }
    }
}
