package me.playernguyen.opteco.api;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.playernguyen.opteco.core.model.Transaction;
import me.playernguyen.opteco.core.model.TransferResult;
import org.jetbrains.annotations.NotNull;

/**
 * Public OptEco API, registered with Bukkit's {@link org.bukkit.plugin.ServicesManager}.
 *
 * <p>Look the service up after OptEco has enabled:
 * {@code Bukkit.getServicesManager().load(OptEcoAPI.class)}. Amounts are exact
 * {@link BigDecimal} values normalized to
 * {@link me.playernguyen.opteco.core.model.Money#SCALE} decimal places, so
 * compare them with {@code compareTo} instead of {@code equals}.</p>
 *
 * <p>Threading: {@link #hasAccount}, {@link #getBalance}, and every mutating
 * method can block on database I/O and should be called from an asynchronous
 * task; use {@link #getBalanceAsync} when the caller is on the Bukkit main
 * thread. {@link #getCachedBalance} never touches the database.
 * {@link #isPending} and {@link #getPendingTransaction} normally read the
 * in-memory pending map but may fall back to a database lookup when the entry
 * is not cached. Mutations update the write-through cache and fire the custom
 * Bukkit events after the database commit.</p>
 */
public interface OptEcoAPI {

    /**
     * Checks whether an account exists for the player.
     *
     * @param uuid player unique id
     * @return {@code true} when the account is cached or persisted
     * @throws me.playernguyen.opteco.core.exception.StorageException when the account table cannot be queried
     */
    boolean hasAccount(@NotNull UUID uuid);

    /**
     * Reads the current balance, loading it from storage when the player is not cached.
     *
     * @param uuid player unique id
     * @return the exact balance, or zero when no account exists
     * @throws me.playernguyen.opteco.core.exception.StorageException when the balance cannot be loaded
     */
    @NotNull
    BigDecimal getBalance(@NotNull UUID uuid);

    /**
     * Reads the cached balance without touching the database.
     *
     * @param uuid player unique id
     * @return the cached balance, or zero when the player has not been loaded
     */
    @NotNull
    BigDecimal getCachedBalance(@NotNull UUID uuid);

    /**
     * Reads the balance on the database executor.
     *
     * @param uuid player unique id
     * @return a future completed with the exact balance on a database worker thread, or completed exceptionally
     *     when OptEco is shutting down or the read fails
     */
    @NotNull
    CompletableFuture<BigDecimal> getBalanceAsync(@NotNull UUID uuid);

    /**
     * Replaces a player's balance, creating the account when necessary.
     *
     * @param uuid player unique id
     * @param amount new exact balance
     * @throws IllegalArgumentException when the amount has more than eight decimal places, is fractional while
     *     {@code UseDouble} is disabled, or is below the configured minimum balance
     * @throws me.playernguyen.opteco.core.exception.StorageException when the change cannot be committed
     */
    void setBalance(@NotNull UUID uuid, @NotNull BigDecimal amount);

    /**
     * Adds an amount to a player's balance, creating the account when necessary.
     *
     * @param uuid player unique id
     * @param amount positive exact amount to add
     * @throws IllegalArgumentException when the amount is not positive, has more than eight decimal places, or is
     *     fractional while {@code UseDouble} is disabled
     * @throws me.playernguyen.opteco.core.exception.StorageException when the change cannot be committed
     */
    void addBalance(@NotNull UUID uuid, @NotNull BigDecimal amount);

    /**
     * Removes an amount from a player's balance.
     *
     * @param uuid player unique id
     * @param amount positive exact amount to remove
     * @return {@code true} when the debit was committed, {@code false} when it would leave the balance below the
     *     configured minimum
     * @throws IllegalArgumentException when the amount is not positive, has more than eight decimal places, or is
     *     fractional while {@code UseDouble} is disabled
     * @throws me.playernguyen.opteco.core.exception.StorageException when the change cannot be committed
     */
    boolean takeBalance(@NotNull UUID uuid, @NotNull BigDecimal amount);

    /**
     * Transfers an amount between two accounts inside one database transaction.
     *
     * @param from sender unique id
     * @param to receiver unique id
     * @param amount positive exact amount to move
     * @return the transfer outcome; expected business failures such as insufficient funds or identical accounts are
     *     reported through {@link TransferResult} instead of an exception
     * @throws IllegalArgumentException when the amount has more than eight decimal places or is fractional while
     *     {@code UseDouble} is disabled
     * @throws me.playernguyen.opteco.core.exception.StorageException when the transfer cannot be committed
     */
    @NotNull
    TransferResult transfer(@NotNull UUID from, @NotNull UUID to, @NotNull BigDecimal amount);

    /**
     * Checks whether the player has a pending outgoing payment.
     *
     * @param uuid player unique id
     * @return {@code true} when a pending payment exists for the player as sender
     */
    boolean isPending(@NotNull UUID uuid);

    /**
     * Returns the player's pending outgoing payment.
     *
     * @param uuid player unique id
     * @return the pending transaction, or an empty optional when none exists
     */
    @NotNull
    Optional<Transaction> getPendingTransaction(@NotNull UUID uuid);

    /**
     * Returns the configured currency label.
     *
     * @return text substituted for {@code %currency%} in messages, for example {@code points}
     */
    @NotNull
    String getCurrencySymbol();

    /**
     * Reads the balance as a {@code double}.
     *
     * @param uuid player unique id
     * @return the balance converted to a {@code double}
     * @deprecated binary floating point loses precision above eight decimal places; use
     *     {@link #getBalance(UUID)} and keep the exact {@link BigDecimal} value
     */
    @Deprecated
    default double getPoints(@NotNull UUID uuid) {
        return getBalance(uuid).doubleValue();
    }

    /**
     * Replaces the balance from a {@code double} amount.
     *
     * @param uuid player unique id
     * @param amount new balance
     * @return always {@code true}; failures are raised as exceptions
     * @deprecated binary floating point cannot represent balances exactly; use {@link #setBalance(UUID, BigDecimal)}
     */
    @Deprecated
    default boolean setPoints(@NotNull UUID uuid, double amount) {
        setBalance(uuid, BigDecimal.valueOf(amount));
        return true;
    }

    /**
     * Adds a {@code double} amount to the balance.
     *
     * @param uuid player unique id
     * @param amount amount to add
     * @return always {@code true}; failures are raised as exceptions
     * @deprecated binary floating point cannot represent balances exactly; use {@link #addBalance(UUID, BigDecimal)}
     */
    @Deprecated
    default boolean addPoints(@NotNull UUID uuid, double amount) {
        addBalance(uuid, BigDecimal.valueOf(amount));
        return true;
    }

    /**
     * Removes a {@code double} amount from the balance.
     *
     * @param uuid player unique id
     * @param amount amount to remove
     * @return {@code true} when the debit was committed, {@code false} when it would leave the balance below the
     *     configured minimum
     * @deprecated binary floating point cannot represent balances exactly; use {@link #takeBalance(UUID, BigDecimal)}
     */
    @Deprecated
    default boolean takePoints(@NotNull UUID uuid, double amount) {
        return takeBalance(uuid, BigDecimal.valueOf(amount));
    }
}
