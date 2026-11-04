package me.playernguyen.opteco.core.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import me.playernguyen.opteco.config.OptEcoConfig;
import me.playernguyen.opteco.core.model.Account;
import me.playernguyen.opteco.core.model.BalanceChange;
import me.playernguyen.opteco.core.model.BalanceChangeReason;
import me.playernguyen.opteco.core.model.Money;
import me.playernguyen.opteco.core.model.TransferResult;
import me.playernguyen.opteco.core.model.TransferStatus;
import me.playernguyen.opteco.core.port.AccountCache;
import me.playernguyen.opteco.core.port.AccountRepository;
import me.playernguyen.opteco.core.port.EconomyEventBus;
import me.playernguyen.opteco.core.port.UnitOfWork;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Coordinates account reads, balance mutations, and transfers.
 *
 * <p>This is the only service allowed to change a balance. Each mutation runs
 * inside a {@link UnitOfWork} transaction, refreshes the write-through
 * {@link AccountCache} after the commit, and then publishes a
 * {@link BalanceChange}. Mutations for the same player are serialized with
 * {@link KeyedLocks} so the cache update stays ordered with the transaction that
 * produced it.</p>
 *
 * <p>The service is thread-safe and designed to be called from the async
 * database executor for writes; reads may be served from the cache. It holds no
 * player-specific state beyond the shared cache.</p>
 */
public final class EconomyService {

    /** Account persistence used for all reads and writes. */
    private final AccountRepository accounts;

    /** Transaction boundary shared with the storage layer. */
    private final UnitOfWork unitOfWork;

    /** Authoritative balance cache for online players. */
    private final AccountCache cache;

    /** Event sink notified after committed balance changes. */
    private final EconomyEventBus events;

    /** Supplies the current config snapshot, which a reload can replace. */
    private final Supplier<OptEcoConfig> config;

    /** Striped per-player locks that keep cache updates ordered with mutations. */
    private final KeyedLocks locks = new KeyedLocks(128);

    /**
     * Creates the economy service.
     *
     * @param accounts account persistence
     * @param unitOfWork transaction runner used for every mutation
     * @param cache write-through balance cache
     * @param events event sink for committed balance changes
     * @param config supplies the current configuration snapshot
     */
    public EconomyService(
            AccountRepository accounts,
            UnitOfWork unitOfWork,
            AccountCache cache,
            EconomyEventBus events,
            Supplier<OptEcoConfig> config) {
        this.accounts = accounts;
        this.unitOfWork = unitOfWork;
        this.cache = cache;
        this.events = events;
        this.config = config;
    }

    /**
     * Returns the cached balance without touching storage.
     *
     * @param uuid player UUID
     * @return cached balance, or empty when the player is not cached
     */
    @NotNull
    public Optional<BigDecimal> cachedBalance(@NotNull UUID uuid) {
        return cache.find(uuid);
    }

    /**
     * Returns a player's balance, loading it into the cache on a miss.
     *
     * <p>Missing accounts are not created; they report {@link Money#ZERO} until
     * {@link #ensureAccount(UUID, String)} runs.</p>
     *
     * @param uuid player UUID
     * @return cached or stored balance, or zero when the account does not exist
     * @throws me.playernguyen.opteco.core.exception.StorageException if the account cannot be loaded
     */
    @NotNull
    public BigDecimal getBalance(@NotNull UUID uuid) {
        Optional<BigDecimal> cached = cache.find(uuid);
        if (cached.isPresent()) {
            return cached.get();
        }
        Optional<Account> account = accounts.find(uuid);
        if (!account.isPresent()) {
            return Money.ZERO;
        }
        cache.put(uuid, account.get().balance());
        return account.get().balance();
    }

    /**
     * Tests whether a player account exists in the cache or in storage.
     *
     * @param uuid player UUID
     * @return {@code true} when the account is known
     * @throws me.playernguyen.opteco.core.exception.StorageException if storage cannot be queried
     */
    public boolean hasAccount(@NotNull UUID uuid) {
        return cache.find(uuid).isPresent() || accounts.exists(uuid);
    }

    /**
     * Creates the account when needed and caches its balance.
     *
     * <p>The configured start balance is used only when the row is first
     * inserted; an existing balance is left unchanged.</p>
     *
     * @param uuid player UUID
     * @param name last known player name
     * @throws me.playernguyen.opteco.core.exception.StorageException if the account cannot be loaded or created
     */
    public void ensureAccount(@NotNull UUID uuid, @NotNull String name) {
        BigDecimal startBalance = config.get().startBalance();
        Account account = accounts.getOrCreate(uuid, name, startBalance);
        cache.put(uuid, account.balance());
    }

    /**
     * Drops a player's cached balance, for example when the player quits.
     *
     * @param uuid player UUID
     */
    public void evict(@NotNull UUID uuid) {
        cache.remove(uuid);
    }

    /**
     * Drops every cached balance, for example during shutdown.
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * Sets a balance to an exact value.
     *
     * <p>The new value must not be below the configured minimum balance. The
     * account is created with the configured start balance first when it does not
     * exist.</p>
     *
     * @param uuid player UUID
     * @param name last known player name; null or empty keeps the stored name for an existing account
     * @param amount exact balance to store
     * @param reason event reason published with the change
     * @throws IllegalArgumentException if the amount has unsupported precision, is not an integer when
     *         fractional amounts are disabled, or is below the configured minimum
     * @throws me.playernguyen.opteco.core.exception.StorageException if the transaction fails
     */
    public void set(
            @NotNull UUID uuid,
            @Nullable String name,
            @NotNull BigDecimal amount,
            @NotNull BalanceChangeReason reason) {
        BigDecimal normalized = validateAmount(amount);
        if (normalized.compareTo(config.get().minBalance()) < 0) {
            throw new IllegalArgumentException("Balance cannot be below the configured minimum");
        }
        BigDecimal oldBalance = mutate(uuid, connection -> {
            Account account =
                    accounts.getOrCreate(connection, uuid, name, config.get().startBalance());
            accounts.set(connection, uuid, name, normalized);
            return account.balance();
        });
        cache.put(uuid, normalized);
        publish(uuid, oldBalance, normalized, reason);
    }

    /**
     * Adds a positive amount to a balance.
     *
     * @param uuid player UUID
     * @param name last known player name; null or empty keeps the stored name for an existing account
     * @param amount amount to add
     * @param reason event reason published with the change
     * @throws IllegalArgumentException if the amount is not positive or has unsupported precision
     * @throws IllegalStateException if the credit does not return a balance
     * @throws me.playernguyen.opteco.core.exception.StorageException if the transaction fails
     */
    public void add(
            @NotNull UUID uuid,
            @Nullable String name,
            @NotNull BigDecimal amount,
            @NotNull BalanceChangeReason reason) {
        BigDecimal normalized = requirePositive(amount);
        BigDecimal[] balances = mutate(uuid, connection -> {
            Account account =
                    accounts.getOrCreate(connection, uuid, name, config.get().startBalance());
            BigDecimal newBalance = accounts.credit(connection, uuid, name, normalized)
                    .orElseThrow(() -> new IllegalStateException("Credit did not return a balance"));
            return new BigDecimal[] {account.balance(), newBalance};
        });
        cache.put(uuid, balances[1]);
        publish(uuid, balances[0], balances[1], reason);
    }

    /**
     * Removes a positive amount from a balance, respecting the configured minimum.
     *
     * @param uuid player UUID
     * @param name last known player name; null or empty keeps the stored name for an existing account
     * @param amount amount to remove
     * @param reason event reason published with the change
     * @return {@code true} when the balance was reduced, {@code false} when funds were insufficient
     * @throws IllegalArgumentException if the amount is not positive or has unsupported precision
     * @throws me.playernguyen.opteco.core.exception.StorageException if the transaction fails
     */
    public boolean take(
            @NotNull UUID uuid,
            @Nullable String name,
            @NotNull BigDecimal amount,
            @NotNull BalanceChangeReason reason) {
        BigDecimal normalized = requirePositive(amount);
        BigDecimal[] balances = mutate(uuid, connection -> {
            accounts.getOrCreate(connection, uuid, name, config.get().startBalance());
            Optional<BigDecimal> newBalance =
                    accounts.debit(connection, uuid, normalized, config.get().minBalance());
            if (!newBalance.isPresent()) {
                return null;
            }
            return new BigDecimal[] {newBalance.get().add(normalized), newBalance.get()};
        });
        if (balances == null) {
            return false;
        }
        cache.put(uuid, balances[1]);
        publish(uuid, balances[0], balances[1], reason);
        return true;
    }

    /**
     * Atomically moves funds between two players.
     *
     * <p>Both players are locked together and both balance updates share one
     * transaction. The receiver reason becomes
     * {@link BalanceChangeReason#PAYMENT_RECEIVED} when the sender reason is
     * {@link BalanceChangeReason#PAYMENT_SENT}; any other reason is copied to
     * both sides. Cache entries and events are updated only after the commit.</p>
     *
     * @param from sender UUID
     * @param fromName sender name, or null when unknown
     * @param to receiver UUID
     * @param toName receiver name, or null when unknown
     * @param amount amount to move
     * @param reason event reason for the sender; the receiver reason is derived from it
     * @return the transfer outcome, including resulting balances on success
     * @throws IllegalArgumentException if the amount has unsupported precision or is not an integer when
     *         fractional amounts are disabled
     * @throws IllegalStateException if the transaction returns no outcome or a successful outcome is missing
     *         balances
     * @throws me.playernguyen.opteco.core.exception.StorageException if the transaction fails
     */
    public TransferResult transfer(
            @NotNull UUID from,
            @Nullable String fromName,
            @NotNull UUID to,
            @Nullable String toName,
            @NotNull BigDecimal amount,
            @NotNull BalanceChangeReason reason) {
        BigDecimal normalized = validateAmount(amount);
        if (normalized.signum() <= 0) {
            return TransferResult.failure(TransferStatus.INVALID_AMOUNT);
        }
        if (from.equals(to)) {
            return TransferResult.failure(TransferStatus.SAME_ACCOUNT);
        }
        TransferOutcome outcome = locks.withLock(
                from,
                to,
                () -> unitOfWork.execute(
                        connection -> transferWithin(connection, from, fromName, to, toName, normalized)));
        if (outcome == null) {
            throw new IllegalStateException("Transfer did not return an outcome");
        }
        if (!outcome.isSuccess()) {
            BigDecimal oldSenderBalance = outcome.oldSenderBalance();
            if (outcome.status() == TransferStatus.INSUFFICIENT_FUNDS && oldSenderBalance != null) {
                return TransferResult.insufficientFunds(oldSenderBalance);
            }
            return TransferResult.failure(outcome.status());
        }
        BalanceChangeReason receiverReason =
                reason == BalanceChangeReason.PAYMENT_SENT ? BalanceChangeReason.PAYMENT_RECEIVED : reason;
        completeTransfer(outcome, reason, receiverReason);
        BigDecimal newSender = outcome.newSenderBalance();
        BigDecimal newReceiver = outcome.newReceiverBalance();
        if (newSender == null || newReceiver == null) {
            throw new IllegalStateException("Successful transfer is missing balances");
        }
        return TransferResult.success(newSender, newReceiver);
    }

    /**
     * Performs the balance side of a transfer on an existing connection.
     *
     * <p>The caller must already hold the per-player locks and provide the
     * surrounding transaction; this method neither locks nor commits. Payment
     * confirmation uses it so the payment state change and the balance moves
     * commit together.</p>
     *
     * @param connection connection participating in the caller's transaction
     * @param from sender UUID
     * @param fromName sender name, or null when unknown
     * @param to receiver UUID
     * @param toName receiver name, or null when unknown
     * @param amount amount to move
     * @return the balance outcome, including the unchanged sender balance on insufficient funds
     * @throws IllegalArgumentException if {@code amount} cannot be normalized to the fixed scale
     * @throws SQLException if a database operation fails
     */
    @NotNull
    public TransferOutcome transferWithin(
            @NotNull Connection connection,
            @NotNull UUID from,
            @Nullable String fromName,
            @NotNull UUID to,
            @Nullable String toName,
            @NotNull BigDecimal amount)
            throws SQLException {
        Account sender =
                accounts.getOrCreate(connection, from, fromName, config.get().startBalance());
        Account receiver =
                accounts.getOrCreate(connection, to, toName, config.get().startBalance());

        Optional<BigDecimal> senderBalance =
                accounts.debit(connection, from, amount, config.get().minBalance());
        if (!senderBalance.isPresent()) {
            return TransferOutcome.insufficientFunds(from, sender.balance());
        }
        Optional<BigDecimal> receiverBalance = accounts.credit(connection, to, toName, amount);
        if (!receiverBalance.isPresent()) {
            throw new SQLException("Could not credit receiver");
        }
        return TransferOutcome.success(
                from, to, sender.balance(), senderBalance.get(), receiver.balance(), receiverBalance.get());
    }

    /**
     * Applies a successful transfer outcome to the cache and publishes both events.
     *
     * <p>This must run after the transaction that produced {@code outcome} has
     * committed. A non-successful outcome is ignored. Both cache entries are
     * updated before the events are published.</p>
     *
     * @param outcome successful transfer produced inside a transaction
     * @param senderReason event reason for the sender
     * @param receiverReason event reason for the receiver
     * @throws IllegalStateException if a successful outcome is missing a participant or balance
     */
    public void completeTransfer(
            @NotNull TransferOutcome outcome,
            @NotNull BalanceChangeReason senderReason,
            @NotNull BalanceChangeReason receiverReason) {
        if (!outcome.isSuccess()) {
            return;
        }
        UUID sender = outcome.sender();
        UUID receiver = outcome.receiver();
        BigDecimal oldSender = outcome.oldSenderBalance();
        BigDecimal newSender = outcome.newSenderBalance();
        BigDecimal oldReceiver = outcome.oldReceiverBalance();
        BigDecimal newReceiver = outcome.newReceiverBalance();
        if (receiver == null || oldSender == null || newSender == null || oldReceiver == null || newReceiver == null) {
            throw new IllegalStateException("Successful transfer is missing balances");
        }
        cache.put(sender, newSender);
        cache.put(receiver, newReceiver);
        publish(sender, oldSender, newSender, senderReason);
        publish(receiver, oldReceiver, newReceiver, receiverReason);
    }

    /**
     * Returns the accounts with the highest balances.
     *
     * @param limit maximum number of accounts to return
     * @return accounts ordered from highest to lowest balance
     * @throws me.playernguyen.opteco.core.exception.StorageException if the query fails
     */
    @NotNull
    public List<Account> top(int limit) {
        return accounts.top(limit);
    }

    /**
     * Runs a compound balance operation while holding the same per-player locks
     * used by the mutation methods. Payment confirmation uses this to keep its
     * transaction-state update, debit/credit, cache update, and events ordered
     * with concurrent admin/API changes.
     *
     * <p>The action should wrap a single unit of work and finish inside the
     * locks; do not run arbitrary player code here.</p>
     *
     * @param first first player UUID, normally the sender
     * @param second second player UUID, normally the receiver; may equal {@code first}
     * @param action work to execute while the locks are held
     * @param <T> return type of the action
     * @return the value returned by {@code action}
     */
    public <T> T withBalanceLocks(@NotNull UUID first, @NotNull UUID second, @NotNull Supplier<T> action) {
        return locks.withLock(first, second, action);
    }

    /**
     * Runs a balance operation under the player lock and a database transaction.
     *
     * @param uuid player whose balance is mutated
     * @param work SQL work executed with the transaction connection
     * @param <T> result type
     * @return the value returned by {@code work}
     */
    private <T> T mutate(UUID uuid, SqlWork<T> work) {
        return locks.withLock(uuid, uuid, () -> unitOfWork.execute(connection -> work.execute(connection)));
    }

    /**
     * Normalizes an amount and enforces the integer-only configuration.
     *
     * @param amount amount to validate
     * @return the normalized amount
     * @throws IllegalArgumentException if the amount cannot be normalized or is fractional while
     *         fractional amounts are disabled
     */
    private BigDecimal validateAmount(BigDecimal amount) {
        BigDecimal normalized = Money.normalize(amount);
        if (!config.get().useDouble() && !Money.isInteger(normalized)) {
            throw new IllegalArgumentException("Amount must be an integer");
        }
        return normalized;
    }

    /**
     * Validates that an amount is positive after normalization.
     *
     * @param amount amount to validate
     * @return the normalized amount
     * @throws IllegalArgumentException if the amount is invalid or not greater than zero
     */
    private BigDecimal requirePositive(BigDecimal amount) {
        BigDecimal normalized = validateAmount(amount);
        if (normalized.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        return normalized;
    }

    /**
     * Publishes a balance-change event, skipping no-op changes.
     *
     * @param uuid player whose balance changed
     * @param oldBalance balance before the change
     * @param newBalance balance after the change
     * @param reason category describing the change
     */
    private void publish(UUID uuid, BigDecimal oldBalance, BigDecimal newBalance, BalanceChangeReason reason) {
        if (oldBalance.compareTo(newBalance) != 0) {
            events.balanceChanged(new BalanceChange(uuid, oldBalance, newBalance, reason));
        }
    }

    /**
     * SQL work that may fail with a checked {@link SQLException}.
     *
     * @param <T> result type
     */
    @FunctionalInterface
    private interface SqlWork<T> {

        /**
         * Executes the work on a transactional connection.
         *
         * @param connection connection bound to the current transaction
         * @return result of the work
         * @throws SQLException if a database operation fails
         */
        T execute(Connection connection) throws SQLException;
    }
}
