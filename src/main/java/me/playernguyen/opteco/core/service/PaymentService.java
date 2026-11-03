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

/**
 * Implements the persisted pending-payment state machine.
 *
 * <p>A payment request creates a {@link Transaction} in
 * {@link TransactionState#PENDING} and caches it by sender. Confirmation
 * atomically claims the transition to {@link TransactionState#CONFIRMED} and
 * moves funds in the same database transaction; cancellation and expiry move it
 * to {@link TransactionState#CANCELLED} or {@link TransactionState#EXPIRED}
 * without touching balances. The request, confirm, cancel, and expire operations
 * are {@code synchronized} so the in-memory index stays consistent with the
 * conditional database updates.</p>
 *
 * <p>Storage failures surface as {@link StorageException}. Expiry is driven by
 * the countdown task through {@link #expire(UUID)}; {@link #expireOverdue()}
 * cleans up payments left over from a previous run.</p>
 */
public final class PaymentService {

    /** Payment persistence for the pending and historical transaction rows. */
    private final PaymentRepository payments;

    /** Transaction runner shared with the balance moves. */
    private final UnitOfWork unitOfWork;

    /** Economy service used to validate and move funds. */
    private final EconomyService economy;

    /** Event sink notified after committed state transitions. */
    private final EconomyEventBus events;

    /** Supplies the current config snapshot, which a reload can replace. */
    private final Supplier<OptEcoConfig> config;

    /** Time source used for creation, expiry, and duration calculations. */
    private final Clock clock;

    /** In-memory index of the newest pending payment per sender, populated lazily from storage. */
    private final ConcurrentMap<UUID, Transaction> pending = new ConcurrentHashMap<>();

    /**
     * Creates the payment service.
     *
     * @param payments payment persistence
     * @param unitOfWork transaction runner used by confirmation
     * @param economy economy service that supplies balances and transfers
     * @param events event sink for transaction transitions
     * @param config supplies the current configuration snapshot
     * @param clock time source for expiry calculations
     */
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

    /**
     * Creates a pending payment from one player to another.
     *
     * <p>The amount must be positive, representable, and fractional only when
     * fractional amounts are enabled. The sender must be able to cover it without
     * crossing the configured minimum balance. A sender may have only one
     * unexpired pending payment; an expired one is marked expired first. Both
     * accounts are created when missing.</p>
     *
     * @param sender sender UUID
     * @param senderName sender name used when the account is created
     * @param receiver receiver UUID
     * @param receiverName receiver name used when the account is created
     * @param amount amount to pay
     * @return a result with the created transaction on success, or an explanatory status
     * @throws StorageException if the payment row cannot be stored
     */
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

    /**
     * Confirms a sender's pending payment and moves the funds.
     *
     * <p>An expired payment is marked expired instead of being confirmed. The
     * lock covers both participants, and the state transition plus debit/credit
     * share one database transaction; a failed transfer rolls the claim back and
     * marks the payment {@link TransactionState#FAILED}.</p>
     *
     * @param sender sender UUID
     * @return a result with both resulting balances on success, or an explanatory status
     * @throws StorageException if the confirmation cannot be completed in storage
     */
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

    /**
     * Runs the claim-and-transfer step while the caller holds both player locks.
     *
     * @param transaction pending payment to confirm
     * @return confirmation result
     * @throws StorageException if the transaction fails
     */
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

    /**
     * Cancels a sender's pending payment without moving funds.
     *
     * @param sender sender UUID
     * @return a result carrying the cancelled transaction, or an explanatory status
     * @throws StorageException if the state change cannot be stored
     */
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

    /**
     * Marks a sender's pending payment as expired when its deadline has passed.
     *
     * @param sender sender UUID
     * @return a result with status {@link PaymentStatus#EXPIRED} when a payment expired, or an explanatory status
     * @throws StorageException if the state change cannot be stored
     */
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

    /**
     * Expires every overdue pending payment in storage.
     *
     * <p>This bulk update publishes no per-payment events and leaves the
     * in-memory index alone; the plugin uses it at startup to clean up payments
     * left pending by a previous run.</p>
     *
     * @return number of payments moved to {@link TransactionState#EXPIRED}
     * @throws StorageException if the update fails
     */
    public int expireOverdue() {
        return payments.expireOverdue(clock.now());
    }

    /**
     * Returns the pending payment for a sender, loading it from storage on a miss.
     *
     * @param sender sender UUID
     * @return the pending payment, or empty when the sender has none
     * @throws StorageException if the lookup fails
     */
    @NotNull
    public Optional<Transaction> pendingFor(@NotNull UUID sender) {
        return Optional.ofNullable(findPending(sender));
    }

    /**
     * Tests whether a sender has a pending payment.
     *
     * @param sender sender UUID
     * @return {@code true} when a pending payment exists
     * @throws StorageException if the lookup fails
     */
    public boolean isPending(@NotNull UUID sender) {
        return findPending(sender) != null;
    }

    /**
     * Returns a snapshot of the currently indexed pending payments.
     *
     * <p>The returned list is a copy and is safe to iterate while payments are
     * confirmed or expired concurrently.</p>
     *
     * @return pending payments currently held in memory
     */
    @NotNull
    public List<Transaction> pendingTransactions() {
        return new ArrayList<>(pending.values());
    }

    /**
     * Drops the in-memory pending index.
     *
     * <p>Stored payments are untouched; they are reloaded lazily on the next
     * lookup and expired at the next startup if their deadline passed.</p>
     */
    public void shutdown() {
        pending.clear();
    }

    /**
     * Finds a sender's pending payment, consulting the in-memory index first and
     * caching the stored row on a miss.
     *
     * @param sender sender UUID
     * @return the pending payment, or null when none exists
     * @throws StorageException if the storage lookup fails
     */
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

    /**
     * Transitions a payment from pending to expired, updates the index, and
     * publishes the event.
     *
     * @param transaction pending payment whose deadline has passed
     * @throws StorageException if the state change cannot be stored
     */
    private void expireTransaction(Transaction transaction) {
        payments.transition(transaction.id(), TransactionState.PENDING, TransactionState.EXPIRED);
        pending.remove(transaction.sender());
        events.transactionChanged(transaction.withState(TransactionState.EXPIRED), TransactionState.PENDING);
    }

    /**
     * Internal marker used to roll back a confirmation when the transfer cannot
     * complete; never escapes this class.
     */
    private static final class PaymentRollbackException extends RuntimeException {

        /** Serialization id required because exceptions are {@link java.io.Serializable}. */
        private static final long serialVersionUID = 1L;
    }
}
