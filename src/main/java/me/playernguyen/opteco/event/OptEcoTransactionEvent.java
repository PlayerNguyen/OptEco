package me.playernguyen.opteco.event;

import java.util.Objects;
import me.playernguyen.opteco.core.model.Transaction;
import me.playernguyen.opteco.core.model.TransactionState;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired on the Bukkit main thread when a payment transaction changes state.
 *
 * <p>{@link me.playernguyen.opteco.runtime.BukkitEconomyEventBus} dispatches the
 * event for the creation of a {@code PENDING} payment and for the transitions to
 * {@code CONFIRMED}, {@code CANCELLED}, {@code EXPIRED} and {@code FAILED}. The
 * event carries the updated immutable {@link Transaction} plus the state it came
 * from; the previous state is {@code null} when the payment is first created.
 * Balance effects are reported separately through {@link OptEcoPointChangedEvent}.
 * The event is informational and not cancellable.</p>
 */
public final class OptEcoTransactionEvent extends Event {

    /** Bukkit handler list shared by every instance of this event. */
    private static final HandlerList HANDLERS = new HandlerList();

    /** Transaction in its new state. */
    private final Transaction transaction;

    /** State before the transition, or {@code null} when the payment was just created. */
    private final TransactionState previousState;

    /**
     * Creates the event for a state transition.
     *
     * @param transaction transaction carrying the new state
     * @param previousState state before the transition, or {@code null} when the payment was just created
     * @throws NullPointerException when {@code transaction} is {@code null}
     */
    public OptEcoTransactionEvent(Transaction transaction, TransactionState previousState) {
        this.transaction = Objects.requireNonNull(transaction, "transaction");
        this.previousState = previousState;
    }

    /**
     * Returns the transaction in its new state.
     *
     * @return immutable transaction snapshot
     */
    public Transaction transaction() {
        return transaction;
    }

    /**
     * Returns the state the transaction moved away from.
     *
     * @return previous state, or {@code null} when the payment was just created
     */
    public TransactionState previousState() {
        return previousState;
    }

    /**
     * Returns the transaction's current state.
     *
     * @return state carried by {@link #transaction()}
     */
    public TransactionState state() {
        return transaction.state();
    }

    /**
     * Returns the Bukkit handler list for this event type.
     *
     * @return the shared handler list
     */
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Returns the static handler list required by Bukkit's event registration.
     *
     * @return the shared handler list
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
