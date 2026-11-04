package me.playernguyen.opteco.core.port;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.playernguyen.opteco.core.model.Account;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistence port for player accounts.
 *
 * <p>Balances cross the database boundary as scaled longs (see
 * {@link me.playernguyen.opteco.core.model.Money#toStorage(java.math.BigDecimal)}).
 * Overloads that accept a {@link Connection} join the caller's transaction and
 * propagate {@link SQLException}; the convenience overloads open their own
 * connection and wrap failures in
 * {@link me.playernguyen.opteco.core.exception.StorageException}.</p>
 */
public interface AccountRepository {

    /**
     * Loads an account by UUID in its own connection.
     *
     * @param uuid player UUID
     * @return the stored account, or empty when no row exists
     * @throws me.playernguyen.opteco.core.exception.StorageException if the query fails
     */
    @NotNull
    Optional<Account> find(@NotNull UUID uuid);

    /**
     * Loads an account by UUID inside the caller's transaction.
     *
     * @param connection connection participating in the caller's transaction
     * @param uuid player UUID
     * @return the stored account, or empty when no row exists
     * @throws SQLException if the query fails
     */
    @NotNull
    Optional<Account> find(@NotNull Connection connection, @NotNull UUID uuid) throws SQLException;

    /**
     * Loads an account or inserts it with the supplied starting balance.
     *
     * <p>A non-empty name that differs from the stored name refreshes the stored
     * name.</p>
     *
     * @param uuid player UUID
     * @param name last known player name, or null when unknown
     * @param initialBalance balance used when the account does not exist yet
     * @return the existing or newly created account
     * @throws me.playernguyen.opteco.core.exception.StorageException if the lookup or insertion fails
     */
    @NotNull
    Account getOrCreate(@NotNull UUID uuid, @Nullable String name, @NotNull BigDecimal initialBalance);

    /**
     * Loads an account or inserts it inside the caller's transaction.
     *
     * @param connection connection participating in the caller's transaction
     * @param uuid player UUID
     * @param name last known player name, or null when unknown
     * @param initialBalance balance used when the account does not exist yet
     * @return the existing or newly created account
     * @throws SQLException if the lookup or insertion fails
     */
    @NotNull
    Account getOrCreate(
            @NotNull Connection connection,
            @NotNull UUID uuid,
            @Nullable String name,
            @NotNull BigDecimal initialBalance)
            throws SQLException;

    /**
     * Overwrites an account's name and balance.
     *
     * @param connection connection participating in the caller's transaction
     * @param uuid player UUID
     * @param name last known player name; null or empty keeps the stored name for an existing account
     * @param balance exact balance to store
     * @throws SQLException if the update fails
     */
    void set(@NotNull Connection connection, @NotNull UUID uuid, @Nullable String name, @NotNull BigDecimal balance)
            throws SQLException;

    /**
     * Atomically subtracts an amount while keeping the balance at or above a floor.
     *
     * <p>The conditional update prevents concurrent operations from driving the
     * balance below {@code floor}; an empty result means no row was updated.</p>
     *
     * @param connection connection participating in the caller's transaction
     * @param uuid player UUID
     * @param amount amount to subtract
     * @param floor lowest balance the update may produce
     * @return the new balance, or empty when the update was refused
     * @throws SQLException if the update fails
     */
    @NotNull
    Optional<BigDecimal> debit(
            @NotNull Connection connection, @NotNull UUID uuid, @NotNull BigDecimal amount, @NotNull BigDecimal floor)
            throws SQLException;

    /**
     * Atomically adds an amount, inserting the account when it does not exist.
     *
     * @param connection connection participating in the caller's transaction
     * @param uuid player UUID
     * @param name name to store for a newly created account; null or empty keeps the stored name for an
     *        existing account
     * @param amount amount to add
     * @return the resulting balance, or empty when the row cannot be read back
     * @throws SQLException if the update fails
     */
    @NotNull
    Optional<BigDecimal> credit(
            @NotNull Connection connection, @NotNull UUID uuid, @Nullable String name, @NotNull BigDecimal amount)
            throws SQLException;

    /**
     * Loads the accounts with the highest balances.
     *
     * @param limit maximum number of rows to return
     * @return accounts ordered from highest to lowest balance
     * @throws me.playernguyen.opteco.core.exception.StorageException if the query fails
     */
    @NotNull
    List<Account> top(int limit);

    /**
     * Tests whether an account row exists.
     *
     * @param uuid player UUID
     * @return {@code true} when the account is stored
     * @throws me.playernguyen.opteco.core.exception.StorageException if the query fails
     */
    boolean exists(@NotNull UUID uuid);
}
