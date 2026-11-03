package me.playernguyen.opteco.event;

import java.util.Objects;
import me.playernguyen.opteco.core.model.Transaction;
import me.playernguyen.opteco.core.model.TransactionState;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class OptEcoTransactionEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Transaction transaction;
    private final TransactionState previousState;

    public OptEcoTransactionEvent(Transaction transaction, TransactionState previousState) {
        this.transaction = Objects.requireNonNull(transaction, "transaction");
        this.previousState = previousState;
    }

    public Transaction transaction() {
        return transaction;
    }

    public TransactionState previousState() {
        return previousState;
    }

    public TransactionState state() {
        return transaction.state();
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
