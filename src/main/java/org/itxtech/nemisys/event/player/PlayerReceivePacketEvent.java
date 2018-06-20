package org.itxtech.nemisys.event.player;

import lombok.Getter;
import org.itxtech.nemisys.Player;
import org.itxtech.nemisys.event.Cancellable;
import org.itxtech.nemisys.network.protocol.mcpe.DataPacket;

/**
 * @author CreeperFace
 */
public class PlayerReceivePacketEvent extends PlayerEvent implements Cancellable {

    @Getter
    private final DataPacket packet;

    public PlayerReceivePacketEvent(Player player, DataPacket packet) {
        super(player);
        this.packet = packet;
    }
}
