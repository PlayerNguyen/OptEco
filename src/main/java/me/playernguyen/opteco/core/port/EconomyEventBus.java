package me.playernguyen.opteco.core.port;

import me.playernguyen.opteco.core.model.BalanceChange;
import me.playernguyen.opteco.core.model.Transaction;
import me.playernguyen.opteco.core.model.TransactionState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Outbound port for economy events.
 *
 * <p>Services call this after the database commit and cache update, so an
 * implementation may translate the change into a Bukkit event without exposing
 * uncommitted state. Dispatch may happen inline or be scheduled; implementations
 * should keep the work short.</p>
 */
public interface EconomyEventBus {

    /**
     * Publishes a committed balance change.
     *
     * @param change old balance, new balance, player, and reason
     */
    void balanceChanged(@NotNull BalanceChange change);

    /**
     * Publishes a payment state transition.
     *
     * @param transaction payment in its new state
     * @param previousState state before the transition, or null when the payment was just created
     */
    void transactionChanged(@NotNull Transaction transaction, @Nullable TransactionState previousState);
}
