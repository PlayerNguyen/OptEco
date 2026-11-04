package me.playernguyen.opteco.api;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.playernguyen.opteco.config.ConfigService;
import me.playernguyen.opteco.core.model.BalanceChangeReason;
import me.playernguyen.opteco.core.model.Money;
import me.playernguyen.opteco.core.model.Transaction;
import me.playernguyen.opteco.core.model.TransferResult;
import me.playernguyen.opteco.core.service.EconomyService;
import me.playernguyen.opteco.core.service.PaymentService;
import me.playernguyen.opteco.runtime.AsyncExecutor;
import org.jetbrains.annotations.NotNull;

/**
 * Default {@link OptEcoAPI} implementation backed by the core services.
 *
 * <p>{@link me.playernguyen.opteco.OptEco} creates one instance during startup,
 * registers it with Bukkit's {@code ServicesManager}, and lets the service
 * manager drop it on disable. The adapter only delegates: balance rules,
 * locking, cache updates and events live in {@link EconomyService}, while
 * pending-payment state lives in {@link PaymentService}. It holds no mutable
 * state, so it is safe to share between threads, subject to the blocking
 * behaviour documented on the API methods.</p>
 */
public final class OptEcoApiImpl implements OptEcoAPI {

    /** Executor used to serve asynchronous balance reads. */
    private final AsyncExecutor async;

    /** Balance mutations, caching and balance events. */
    private final EconomyService economy;

    /** Pending-payment state machine. */
    private final PaymentService payments;

    /** Source of the live configuration snapshot, including the currency symbol. */
    private final ConfigService config;

    /**
     * Creates the adapter around the core services.
     *
     * @param async executor used for {@link #getBalanceAsync(UUID)}
     * @param economy economy service that owns balance mutations
     * @param payments payment service that owns pending transactions
     * @param config configuration service read for the currency symbol
     */
    public OptEcoApiImpl(
            @NotNull AsyncExecutor async,
            @NotNull EconomyService economy,
            @NotNull PaymentService payments,
            @NotNull ConfigService config) {
        this.async = async;
        this.economy = economy;
        this.payments = payments;
        this.config = config;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} when the account is cached or persisted
     * @throws me.playernguyen.opteco.core.exception.StorageException when the account table cannot be queried
     */
    @Override
    public boolean hasAccount(@NotNull UUID uuid) {
        return economy.hasAccount(uuid);
    }

    /**
     * {@inheritDoc}
     *
     * @return the exact balance, or zero when no account exists
     * @throws me.playernguyen.opteco.core.exception.StorageException when the balance cannot be loaded
     */
    @Override
    @NotNull
    public BigDecimal getBalance(@NotNull UUID uuid) {
        return economy.getBalance(uuid);
    }

    /**
     * {@inheritDoc}
     *
     * @return the cached balance, or zero when the player has not been loaded
     */
    @Override
    @NotNull
    public BigDecimal getCachedBalance(@NotNull UUID uuid) {
        return economy.cachedBalance(uuid).orElse(Money.ZERO);
    }

    /**
     * {@inheritDoc}
     *
     * @return a future completed on a database worker thread, or exceptionally when the read fails
     */
    @Override
    @NotNull
    public CompletableFuture<BigDecimal> getBalanceAsync(@NotNull UUID uuid) {
        return async.supply(() -> economy.getBalance(uuid));
    }

    /**
     * {@inheritDoc}
     *
     * <p>The resulting event carries {@link BalanceChangeReason#API_SET}.</p>
     *
     * @throws IllegalArgumentException when the amount violates the configured money rules
     * @throws me.playernguyen.opteco.core.exception.StorageException when the change cannot be committed
     */
    @Override
    public void setBalance(@NotNull UUID uuid, @NotNull BigDecimal amount) {
        economy.set(uuid, null, amount, BalanceChangeReason.API_SET);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The resulting event carries {@link BalanceChangeReason#API_ADD}.</p>
     *
     * @throws IllegalArgumentException when the amount is not positive or violates the configured money rules
     * @throws me.playernguyen.opteco.core.exception.StorageException when the change cannot be committed
     */
    @Override
    public void addBalance(@NotNull UUID uuid, @NotNull BigDecimal amount) {
        economy.add(uuid, null, amount, BalanceChangeReason.API_ADD);
    }

    /**
     * {@inheritDoc}
     *
     * <p>A successful debit fires an event with {@link BalanceChangeReason#API_TAKE}.</p>
     *
     * @return {@code true} when the debit was committed, {@code false} when it would leave the balance below the
     *     configured minimum
     * @throws IllegalArgumentException when the amount is not positive or violates the configured money rules
     * @throws me.playernguyen.opteco.core.exception.StorageException when the change cannot be committed
     */
    @Override
    public boolean takeBalance(@NotNull UUID uuid, @NotNull BigDecimal amount) {
        return economy.take(uuid, null, amount, BalanceChangeReason.API_TAKE);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Both legs of the transfer fire events with {@link BalanceChangeReason#API_TRANSFER}.</p>
     *
     * @return the transfer outcome, including expected business failures
     * @throws IllegalArgumentException when the amount violates the configured money rules
     * @throws me.playernguyen.opteco.core.exception.StorageException when the transfer cannot be committed
     */
    @Override
    @NotNull
    public TransferResult transfer(@NotNull UUID from, @NotNull UUID to, @NotNull BigDecimal amount) {
        return economy.transfer(from, null, to, null, amount, BalanceChangeReason.API_TRANSFER);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} when a pending payment exists for the player as sender
     */
    @Override
    public boolean isPending(@NotNull UUID uuid) {
        return payments.isPending(uuid);
    }

    /**
     * {@inheritDoc}
     *
     * @return the pending transaction, or an empty optional when none exists
     */
    @Override
    @NotNull
    public Optional<Transaction> getPendingTransaction(@NotNull UUID uuid) {
        return payments.pendingFor(uuid);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads the current snapshot, so the value follows {@code /points reload}.</p>
     *
     * @return the configured currency label
     */
    @Override
    @NotNull
    public String getCurrencySymbol() {
        return config.current().currencySymbol();
    }
}
