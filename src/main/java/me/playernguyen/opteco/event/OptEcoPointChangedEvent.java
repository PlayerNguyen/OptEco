package me.playernguyen.opteco.event;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import me.playernguyen.opteco.core.model.BalanceChange;
import me.playernguyen.opteco.core.model.BalanceChangeReason;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired on the Bukkit main thread after a balance change has been committed.
 *
 * <p>{@link me.playernguyen.opteco.runtime.BukkitEconomyEventBus} builds and
 * dispatches the event, so listeners may safely touch players and other server
 * state. It is informational and not cancellable: the new balance has already
 * been written to the database and the account cache when the event fires.
 * Balances are exact decimals scaled to
 * {@link me.playernguyen.opteco.core.model.Money#SCALE} decimal places.</p>
 */
public final class OptEcoPointChangedEvent extends Event {

    /** Bukkit handler list shared by every instance of this event. */
    private static final HandlerList HANDLERS = new HandlerList();

    /** Account whose balance changed. */
    private final UUID player;

    /** Balance before the committed change. */
    private final BigDecimal oldBalance;

    /** Balance after the committed change. */
    private final BigDecimal newBalance;

    /** Operation that caused the change. */
    private final BalanceChangeReason reason;

    /**
     * Creates the event from the committed change record.
     *
     * @param change committed balance change carrying the player, both balances and the reason
     * @throws NullPointerException when {@code change} is {@code null}
     */
    public OptEcoPointChangedEvent(BalanceChange change) {
        this.player = Objects.requireNonNull(change, "change").player();
        this.oldBalance = change.oldBalance();
        this.newBalance = change.newBalance();
        this.reason = change.reason();
    }

    /**
     * Returns the account whose balance changed.
     *
     * @return unique id of the affected player
     */
    public UUID player() {
        return player;
    }

    /**
     * Returns the balance before the change.
     *
     * @return previous balance, scaled to eight decimal places
     */
    public BigDecimal oldBalance() {
        return oldBalance;
    }

    /**
     * Returns the balance after the change.
     *
     * @return new balance, scaled to eight decimal places
     */
    public BigDecimal newBalance() {
        return newBalance;
    }

    /**
     * Returns the operation that caused the change.
     *
     * @return reason such as an admin command, payment or API call
     */
    public BalanceChangeReason reason() {
        return reason;
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
