package me.playernguyen.event;

import me.playernguyen.account.Transaction;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class OptEcoPlayerPendingEvent extends Event {

    private static HandlerList handlerList = new HandlerList();

    private UUID player;
    private Transaction transaction;

    public OptEcoPlayerPendingEvent (UUID player, Transaction transaction) {
        this.player = player;
        this.transaction = transaction;
    }

    public UUID getPlayer() {
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
