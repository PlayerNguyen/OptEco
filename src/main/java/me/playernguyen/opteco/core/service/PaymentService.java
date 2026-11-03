package me.playernguyen.opteco.core.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import me.playernguyen.opteco.config.OptEcoConfig;
import me.playernguyen.opteco.core.exception.StorageException;
import me.playernguyen.opteco.core.model.BalanceChangeReason;
import me.playernguyen.opteco.core.model.Money;
import me.playernguyen.opteco.core.model.PaymentResult;
import me.playernguyen.opteco.core.model.PaymentStatus;
import me.playernguyen.opteco.core.model.Transaction;
import me.playernguyen.opteco.core.model.TransactionState;
import me.playernguyen.opteco.core.port.Clock;
import me.playernguyen.opteco.core.port.EconomyEventBus;
import me.playernguyen.opteco.core.port.PaymentRepository;
import me.playernguyen.opteco.core.port.UnitOfWork;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PaymentService {

    private final PaymentRepository payments;
    private final UnitOfWork unitOfWork;
    private final EconomyService economy;
    private final EconomyEventBus events;
    private final Supplier<OptEcoConfig> config;
    private final Clock clock;
    private final ConcurrentMap<UUID, Transaction> pending = new ConcurrentHashMap<>();

    public PaymentService(
            PaymentRepository payments,
            UnitOfWork unitOfWork,
            EconomyService economy,
            EconomyEventBus events,
            Supplier<OptEcoConfig> config,
            Clock clock) {
        this.payments = payments;
        this.unitOfWork = unitOfWork;
        this.economy = economy;
        this.events = events;
        this.config = config;
        this.clock = clock;
    }

    @NotNull
    public synchronized PaymentResult request(
            @NotNull UUID sender,
            @NotNull String senderName,
            @NotNull UUID receiver,
            @NotNull String receiverName,
            @NotNull BigDecimal amount) {
        BigDecimal normalized;
        try {
            normalized = Money.normalize(amount);
        } catch (RuntimeException exception) {
            return PaymentResult.of(PaymentStatus.INVALID_AMOUNT, null);
        }
        if (normalized.signum() <= 0) {
            return PaymentResult.of(PaymentStatus.INVALID_AMOUNT, null);
        }
        if (!config.get().useDouble() && !Money.isInteger(normalized)) {
            return PaymentResult.of(PaymentStatus.INVALID_AMOUNT, null);
        }
        if (sender.equals(receiver)) {
            return PaymentResult.of(PaymentStatus.SAME_ACCOUNT, null);
        }

        long now = clock.now();
        Transaction existing = findPending(sender);
        if (existing != null) {
            if (existing.isExpired(now)) {
                expireTransaction(existing);
            } else {
                return PaymentResult.of(PaymentStatus.ALREADY_PENDING, existing);
            }
        }

        try {
            economy.ensureAccount(sender, senderName);
            economy.ensureAccount(receiver, receiverName);
            BigDecimal senderBalance = economy.getBalance(sender);
            if (senderBalance.subtract(normalized).compareTo(config.get().minBalance()) < 0) {
                return PaymentResult.of(PaymentStatus.INSUFFICIENT_FUNDS, null);
            }

            long expiresAt = now + config.get().paymentDurationSeconds() * 1000L;
            Transaction transaction = new Transaction(
                    UUID.randomUUID().toString(),
                    sender,
                    receiver,
                    normalized,
                    TransactionState.PENDING,
                    now,
                    expiresAt);
            payments.create(transaction);
            pending.put(sender, transaction);
            events.transactionChanged(transaction, null);
            return PaymentResult.of(PaymentStatus.SUCCESS, transaction);
        } catch (StorageException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new StorageException("Could not create payment", exception);
        }
    }

    @NotNull
    public synchronized PaymentResult confirm(@NotNull UUID sender) {
        Transaction transaction = findPending(sender);
        if (transaction == null) {
            return PaymentResult.of(PaymentStatus.NOT_FOUND, null);
        }
        long now = clock.now();
        if (transaction.isExpired(now)) {
            expireTransaction(transaction);
            return PaymentResult.of(PaymentStatus.EXPIRED, transaction.withState(TransactionState.EXPIRED));
        }

        return economy.withBalanceLocks(transaction.sender(), transaction.receiver(), () -> confirmLocked(transaction));
    }

    @NotNull
    private PaymentResult confirmLocked(@NotNull Transaction transaction) {
        TransferOutcome outcome;
        try {
            outcome = unitOfWork.execute(connection -> {
                boolean claimed = payments.transition(
                        connection, transaction.id(), TransactionState.PENDING, TransactionState.CONFIRMED);
                if (!claimed) {
                    return null;
                }
                TransferOutcome transferOutcome = economy.transferWithin(
                        connection, transaction.sender(), null, transaction.receiver(), null, transaction.amount());
                if (!transferOutcome.isSuccess()) {
                    throw new PaymentRollbackException();
                }
                return transferOutcome;
            });
        } catch (PaymentRollbackException exception) {
            payments.transition(transaction.id(), TransactionState.PENDING, TransactionState.FAILED);
            pending.remove(transaction.sender());
            Transaction failed = transaction.withState(TransactionState.FAILED);
            events.transactionChanged(failed, TransactionState.PENDING);
            return PaymentResult.of(PaymentStatus.INSUFFICIENT_FUNDS, failed);
        } catch (StorageException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new StorageException("Could not confirm payment", exception);
        }

        if (outcome == null) {
            pending.remove(transaction.sender());
            return PaymentResult.of(PaymentStatus.ALREADY_HANDLED, transaction);
        }

        economy.completeTransfer(outcome, BalanceChangeReason.PAYMENT_SENT, BalanceChangeReason.PAYMENT_RECEIVED);
        pending.remove(transaction.sender());
        Transaction confirmed = transaction.withState(TransactionState.CONFIRMED);
        events.transactionChanged(confirmed, TransactionState.PENDING);
        BigDecimal senderBalance = outcome.newSenderBalance();
        BigDecimal receiverBalance = outcome.newReceiverBalance();
        if (senderBalance == null || receiverBalance == null) {
            throw new IllegalStateException("Successful payment is missing balances");
        }
        return PaymentResult.success(confirmed, senderBalance, receiverBalance);
    }

    @NotNull
    public synchronized PaymentResult cancel(@NotNull UUID sender) {
        Transaction transaction = findPending(sender);
        if (transaction == null) {
            return PaymentResult.of(PaymentStatus.NOT_FOUND, null);
        }
        boolean changed = payments.transition(transaction.id(), TransactionState.PENDING, TransactionState.CANCELLED);
        pending.remove(sender);
        if (!changed) {
            return PaymentResult.of(PaymentStatus.ALREADY_HANDLED, transaction);
        }
        Transaction cancelled = transaction.withState(TransactionState.CANCELLED);
        events.transactionChanged(cancelled, TransactionState.PENDING);
        return PaymentResult.of(PaymentStatus.SUCCESS, cancelled);
    }

    @NotNull
    public synchronized PaymentResult expire(@NotNull UUID sender) {
        Transaction transaction = findPending(sender);
        if (transaction == null) {
            return PaymentResult.of(PaymentStatus.NOT_FOUND, null);
        }
        if (!transaction.isExpired(clock.now())) {
            return PaymentResult.of(PaymentStatus.NOT_FOUND, transaction);
        }
        expireTransaction(transaction);
        return PaymentResult.of(PaymentStatus.EXPIRED, transaction.withState(TransactionState.EXPIRED));
    }

    public int expireOverdue() {
        return payments.expireOverdue(clock.now());
    }

    @NotNull
    public Optional<Transaction> pendingFor(@NotNull UUID sender) {
        return Optional.ofNullable(findPending(sender));
    }

    public boolean isPending(@NotNull UUID sender) {
        return findPending(sender) != null;
    }

    @NotNull
    public List<Transaction> pendingTransactions() {
        return new ArrayList<>(pending.values());
    }

    public void shutdown() {
        pending.clear();
    }

    @Nullable
    private Transaction findPending(@NotNull UUID sender) {
        Transaction transaction = pending.get(sender);
        if (transaction != null) {
            return transaction;
        }
        Optional<Transaction> stored = payments.findPendingBySender(sender);
        if (stored.isPresent()) {
            pending.put(sender, stored.get());
            return stored.get();
        }
        return null;
    }

    private void expireTransaction(Transaction transaction) {
        payments.transition(transaction.id(), TransactionState.PENDING, TransactionState.EXPIRED);
        pending.remove(transaction.sender());
        events.transactionChanged(transaction.withState(TransactionState.EXPIRED), TransactionState.PENDING);
    }

    private static final class PaymentRollbackException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
