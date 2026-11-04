package me.playernguyen.opteco.core.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Persisted player-to-player payment request and its lifecycle state.
 *
 * <p>A transaction is created in {@link TransactionState#PENDING} and keeps the
 * amount, participants, and expiry deadline until a confirmation,
 * cancellation, or expiry task moves it to a terminal state. Timestamps are
 * epoch milliseconds supplied by the configured
 * {@link me.playernguyen.opteco.core.port.Clock}. Instances are immutable;
 * {@link #withState(TransactionState)} returns an updated copy.</p>
 */
@Getter
@Accessors(fluent = true)
@EqualsAndHashCode
@ToString
public final class Transaction {

    /** Unique payment identifier stored as the transaction key. */
    private final String id;

    /** Player who created and funds the payment. */
    private final UUID sender;

    /** Player who receives the funds on confirmation. */
    private final UUID receiver;

    /** Amount to transfer, normalized to {@link Money#SCALE} decimal places. */
    private final BigDecimal amount;

    /** Current lifecycle state of the payment. */
    private final TransactionState state;

    /** Creation time in epoch milliseconds. */
    private final long createdAt;

    /** Deadline in epoch milliseconds after which the payment may expire. */
    private final long expiresAt;

    /**
     * Creates a transaction snapshot.
     *
     * @param id unique payment identifier
     * @param sender player funding the payment
     * @param receiver player receiving the funds
     * @param amount amount to transfer, normalized to the fixed scale
     * @param state current lifecycle state
     * @param createdAt creation time in epoch milliseconds
     * @param expiresAt expiry deadline in epoch milliseconds
     * @throws NullPointerException if {@code id}, {@code sender}, {@code receiver}, {@code amount}, or
     *         {@code state} is null
     * @throws IllegalArgumentException if {@code amount} has more than {@link Money#SCALE} decimal places
     */
    public Transaction(
            String id,
            UUID sender,
            UUID receiver,
            BigDecimal amount,
            TransactionState state,
            long createdAt,
            long expiresAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.sender = Objects.requireNonNull(sender, "sender");
        this.receiver = Objects.requireNonNull(receiver, "receiver");
        this.amount = Money.normalize(Objects.requireNonNull(amount, "amount"));
        this.state = Objects.requireNonNull(state, "state");
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    /**
     * Tests whether the payment is still awaiting confirmation.
     *
     * @return {@code true} when the state is {@link TransactionState#PENDING}
     */
    public boolean isPending() {
        return state == TransactionState.PENDING;
    }

    /**
     * Tests whether the payment deadline has been reached.
     *
     * <p>This is a pure check; it does not change the stored state.</p>
     *
     * @param now current time in epoch milliseconds, normally from the configured clock
     * @return {@code true} when {@code now} is at or past {@link #expiresAt}
     */
    public boolean isExpired(long now) {
        return expiresAt <= now;
    }

    /**
     * Returns a copy of this payment with a different lifecycle state.
     *
     * @param newState replacement state
     * @return a new transaction with the same id, participants, amount, and timestamps
     * @throws NullPointerException if {@code newState} is null
     */
    public Transaction withState(TransactionState newState) {
        return new Transaction(id, sender, receiver, amount, newState, createdAt, expiresAt);
    }
}
