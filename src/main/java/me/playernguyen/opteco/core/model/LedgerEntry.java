package me.playernguyen.opteco.core.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable row of the append-only balance ledger.
 *
 * <p>Each committed balance mutation writes one entry with the signed delta
 * ({@code amount}), the balances before and after the change, and the reason.
 * A payment produces two entries that share the payment's transaction id.
 * Entries are never updated or deleted; history reads page through them newest
 * first.</p>
 *
 * <p>Amounts are normalized to {@link Money#SCALE} decimal places through
 * {@link Money#normalize(BigDecimal)}. Instances are immutable and therefore
 * thread-safe.</p>
 */
@Getter
@Accessors(fluent = true)
@EqualsAndHashCode
@ToString
public final class LedgerEntry {

    /** Unique ledger entry identifier, normally a random uuid string. */
    private final String id;

    /** Player whose balance changed. */
    private final UUID player;

    /** Other participant for a transfer, or null for a single-player change. */
    @Nullable
    private final UUID counterparty;

    /** Signed delta applied to the balance: positive for credits, negative for debits. */
    private final BigDecimal amount;

    /** Balance before the change. */
    private final BigDecimal balanceBefore;

    /** Balance after the change. */
    private final BigDecimal balanceAfter;

    /** Category describing why the balance changed. */
    private final BalanceChangeReason reason;

    /** Payment identifier shared by both transfer entries, or null for admin/API changes. */
    @Nullable
    private final String transactionId;

    /** Creation time in epoch milliseconds. */
    private final long createdAt;

    /**
     * Creates a ledger entry.
     *
     * @param id unique ledger entry identifier
     * @param player player whose balance changed
     * @param counterparty other participant for a transfer, or null
     * @param amount signed delta applied to the balance
     * @param balanceBefore balance before the change
     * @param balanceAfter balance after the change
     * @param reason category describing why the balance changed
     * @param transactionId payment identifier, or null for non-payment changes
     * @param createdAt creation time in epoch milliseconds
     * @throws NullPointerException if {@code id}, {@code player}, {@code amount},
     *         {@code balanceBefore}, {@code balanceAfter} or {@code reason} is null
     * @throws IllegalArgumentException if an amount has more than {@link Money#SCALE} decimal places
     */
    public LedgerEntry(
            String id,
            UUID player,
            @Nullable UUID counterparty,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            BalanceChangeReason reason,
            @Nullable String transactionId,
            long createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.player = Objects.requireNonNull(player, "player");
        this.counterparty = counterparty;
        this.amount = Money.normalize(Objects.requireNonNull(amount, "amount"));
        this.balanceBefore = Money.normalize(Objects.requireNonNull(balanceBefore, "balanceBefore"));
        this.balanceAfter = Money.normalize(Objects.requireNonNull(balanceAfter, "balanceAfter"));
        this.reason = Objects.requireNonNull(reason, "reason");
        this.transactionId = transactionId;
        this.createdAt = createdAt;
    }
}
