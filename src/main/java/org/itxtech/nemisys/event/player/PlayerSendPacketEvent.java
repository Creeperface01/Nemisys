package org.itxtech.nemisys.event.player;

import lombok.Getter;
import org.itxtech.nemisys.Player;
import org.itxtech.nemisys.event.Cancellable;
import org.itxtech.nemisys.event.HandlerList;
import org.itxtech.nemisys.network.protocol.mcpe.DataPacket;

/**
 * @author CreeperFace
 */
public class PlayerSendPacketEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    @Getter
    private final DataPacket packet;

    public PlayerSendPacketEvent(Player player, DataPacket packet) {
        super(player);
        this.packet = packet;
    }
}
