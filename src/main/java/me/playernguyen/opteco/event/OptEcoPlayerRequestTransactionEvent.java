package me.playernguyen.opteco.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class OptEcoPlayerRequestTransactionEvent  extends PlayerEvent {

    private static HandlerList handlerList;

    public OptEcoPlayerRequestTransactionEvent(Player who) {
        super(who);
    }



    public static HandlerList getHandlerList() {
        return handlerList;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
