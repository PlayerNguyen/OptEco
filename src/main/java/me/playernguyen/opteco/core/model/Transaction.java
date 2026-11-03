package me.playernguyen.opteco.core.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@EqualsAndHashCode
@ToString
public final class Transaction {

    private final String id;
    private final UUID sender;
    private final UUID receiver;
    private final BigDecimal amount;
    private final TransactionState state;
    private final long createdAt;
    private final long expiresAt;

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

    public boolean isPending() {
        return state == TransactionState.PENDING;
    }

    public boolean isExpired(long now) {
        return expiresAt <= now;
    }

    public Transaction withState(TransactionState newState) {
        return new Transaction(id, sender, receiver, amount, newState, createdAt, expiresAt);
    }
}
