package me.playernguyen.opteco.core.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Immutable snapshot of a player account: the owning UUID, the last known player
 * name, and the current balance.
 *
 * <p>Repositories and services create new instances instead of mutating an
 * existing one; {@link #withBalance(BigDecimal)} and {@link #withName(String)}
 * return updated copies. Lombok generates fluent accessors ({@code uuid()},
 * {@code name()}, {@code balance()}). Equality and hashing use only the UUID,
 * so an account stays equal to itself when its balance or name changes. The
 * class is immutable and therefore thread-safe.</p>
 */
@Getter
@Accessors(fluent = true)
@EqualsAndHashCode(of = "uuid")
@ToString
public final class Account {

    /** Unique player identifier and the account key in storage. */
    private final UUID uuid;

    /** Last known player name; never null and empty when the name is unknown. */
    private final String name;

    /** Current balance, normalized to {@link Money#SCALE} decimal places. */
    private final BigDecimal balance;

    /**
     * Creates an account snapshot.
     *
     * @param uuid owning player UUID
     * @param name last known player name; null is stored as an empty string
     * @param balance current balance, normalized to the fixed scale
     * @throws NullPointerException if {@code uuid} or {@code balance} is null
     * @throws IllegalArgumentException if {@code balance} has more than {@link Money#SCALE} decimal places
     */
    public Account(UUID uuid, String name, BigDecimal balance) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.name = name == null ? "" : name;
        this.balance = Money.normalize(Objects.requireNonNull(balance, "balance"));
    }

    /**
     * Returns a copy of this account with a different balance.
     *
     * @param newBalance replacement balance
     * @return a new account with the same UUID and name
     * @throws NullPointerException if {@code newBalance} is null
     * @throws IllegalArgumentException if {@code newBalance} has more than {@link Money#SCALE} decimal places
     */
    public Account withBalance(BigDecimal newBalance) {
        return new Account(uuid, name, newBalance);
    }

    /**
     * Returns a copy of this account with a different player name.
     *
     * @param newName replacement name; null becomes an empty string
     * @return a new account with the same UUID and balance
     */
    public Account withName(String newName) {
        return new Account(uuid, newName, balance);
    }
}
