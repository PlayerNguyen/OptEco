package me.playernguyen.event;

import me.playernguyen.account.Transaction;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class OptEcoPlayerPendingEvent extends Event {

    private static HandlerList handlerList = new HandlerList();

    private Player player;
    private Transaction transaction;

    public OptEcoPlayerPendingEvent (Player player, Transaction transaction) {
        this.player = player;
        this.transaction = transaction;
    }

    public Player getPlayer() {
        return player;
    }

    public Transaction getTransaction() {
        return transaction;
    }


    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }
}
