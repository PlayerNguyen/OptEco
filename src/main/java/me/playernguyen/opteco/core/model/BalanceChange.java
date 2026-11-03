package me.playernguyen.opteco.core.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Value object describing one committed balance transition for a player.
 *
 * <p>Instances are published through
 * {@link me.playernguyen.opteco.core.port.EconomyEventBus} after the database
 * commit and cache update, so listeners observe the old and new balance of a
 * change that has already been applied. The class is immutable and
 * thread-safe.</p>
 */
@Getter
@Accessors(fluent = true)
public final class BalanceChange {

    /** Player whose balance changed. */
    private final UUID player;

    /** Balance before the change. */
    private final BigDecimal oldBalance;

    /** Balance after the change. */
    private final BigDecimal newBalance;

    /** Category describing why the change happened. */
    private final BalanceChangeReason reason;

    /**
     * Creates a balance-change record.
     *
     * @param player player whose balance changed
     * @param oldBalance balance before the change, normalized to the fixed scale
     * @param newBalance balance after the change, normalized to the fixed scale
     * @param reason category describing why the change happened
     * @throws NullPointerException if {@code player} or {@code reason} is null
     * @throws IllegalArgumentException if either balance is null or has more than
     *         {@link Money#SCALE} decimal places
     */
    public BalanceChange(UUID player, BigDecimal oldBalance, BigDecimal newBalance, BalanceChangeReason reason) {
        this.player = Objects.requireNonNull(player, "player");
        this.oldBalance = Money.normalize(oldBalance);
        this.newBalance = Money.normalize(newBalance);
        this.reason = Objects.requireNonNull(reason, "reason");
    }
}
