package org.itxtech.nemisys.event.player;

import lombok.Getter;
import org.itxtech.nemisys.Player;
import org.itxtech.nemisys.event.HandlerList;

public class PlayerLogoutEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();

    @Getter
    private final String message;
    @Getter
    private final LogoutReason reason;

    public PlayerLogoutEvent(Player player) {
        this(player, "disconnect");
    }

    public PlayerLogoutEvent(Player player, String message) {
        this(player, message, LogoutReason.CUSTOM);
    }

    public PlayerLogoutEvent(Player player, String message, LogoutReason reason) {
        super(player);

        this.message = message;
        this.reason = reason;
    }

    public static HandlerList getHandlers() {
        return handlers;
    }

    public enum LogoutReason {
        TIMEOUT,
        QUIT,
        SERVER,
        PLUGIN,
        CUSTOM
    }
}
