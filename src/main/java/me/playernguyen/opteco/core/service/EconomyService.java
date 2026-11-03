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

public final class EconomyService {

    private final AccountRepository accounts;
    private final UnitOfWork unitOfWork;
    private final AccountCache cache;
    private final EconomyEventBus events;
    private final Supplier<OptEcoConfig> config;
    private final KeyedLocks locks = new KeyedLocks(128);

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

    @NotNull
    public Optional<BigDecimal> cachedBalance(@NotNull UUID uuid) {
        return cache.find(uuid);
    }

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

    public boolean hasAccount(@NotNull UUID uuid) {
        return cache.find(uuid).isPresent() || accounts.exists(uuid);
    }

    public void ensureAccount(@NotNull UUID uuid, @NotNull String name) {
        BigDecimal startBalance = config.get().startBalance();
        Account account = accounts.getOrCreate(uuid, name, startBalance);
        cache.put(uuid, account.balance());
    }

    public void evict(@NotNull UUID uuid) {
        cache.remove(uuid);
    }

    public void clearCache() {
        cache.clear();
    }

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

    @NotNull
    public List<Account> top(int limit) {
        return accounts.top(limit);
    }

    /**
     * Runs a compound balance operation while holding the same per-player locks
     * used by the mutation methods. Payment confirmation uses this to keep its
     * transaction-state update, debit/credit, cache update, and events ordered
     * with concurrent admin/API changes.
     */
    public <T> T withBalanceLocks(@NotNull UUID first, @NotNull UUID second, @NotNull Supplier<T> action) {
        return locks.withLock(first, second, action);
    }

    private <T> T mutate(UUID uuid, SqlWork<T> work) {
        return locks.withLock(uuid, uuid, () -> unitOfWork.execute(connection -> work.execute(connection)));
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        BigDecimal normalized = Money.normalize(amount);
        if (!config.get().useDouble() && !Money.isInteger(normalized)) {
            throw new IllegalArgumentException("Amount must be an integer");
        }
        return normalized;
    }

    private BigDecimal requirePositive(BigDecimal amount) {
        BigDecimal normalized = validateAmount(amount);
        if (normalized.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        return normalized;
    }

    private void publish(UUID uuid, BigDecimal oldBalance, BigDecimal newBalance, BalanceChangeReason reason) {
        if (oldBalance.compareTo(newBalance) != 0) {
            events.balanceChanged(new BalanceChange(uuid, oldBalance, newBalance, reason));
        }
    }

    @FunctionalInterface
    private interface SqlWork<T> {
        T execute(Connection connection) throws SQLException;
    }
}
