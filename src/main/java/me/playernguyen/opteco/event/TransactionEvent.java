package me.playernguyen.opteco.event;

import me.playernguyen.opteco.transaction.Transaction;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public abstract class TransactionEvent extends Event {

    public static final HandlerList handlerList = new HandlerList();

    private Transaction transaction;

    public TransactionEvent(Transaction transaction) {
        this.transaction = transaction;
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
