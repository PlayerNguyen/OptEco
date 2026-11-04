package me.playernguyen.opteco.event;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.playernguyen.opteco.core.model.BalanceChange;
import me.playernguyen.opteco.core.model.BalanceChangeReason;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

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
@Getter
@Accessors(fluent = true)
public final class OptEcoPointChangedEvent extends Event {

    /** Bukkit handler list shared by every instance of this event. */
    private static final HandlerList HANDLERS = new HandlerList();

    /** Account whose balance changed. */
    @NotNull
    private final UUID player;

    /** Balance before the committed change. */
    @NotNull
    private final BigDecimal oldBalance;

    /** Balance after the committed change. */
    @NotNull
    private final BigDecimal newBalance;

    /** Operation that caused the change. */
    @NotNull
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
