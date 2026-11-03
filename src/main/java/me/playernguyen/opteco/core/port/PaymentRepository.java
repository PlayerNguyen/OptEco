package me.playernguyen.opteco.core.port;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.playernguyen.opteco.core.model.Transaction;
import me.playernguyen.opteco.core.model.TransactionState;
import org.jetbrains.annotations.NotNull;

/**
 * Persistence port for payment requests ({@link Transaction} rows).
 *
 * <p>Overloads that accept a {@link Connection} let a payment state change share
 * the caller's transaction with the matching balance moves; they propagate
 * {@link SQLException}. The convenience overloads open their own connection and
 * wrap failures in
 * {@link me.playernguyen.opteco.core.exception.StorageException}.</p>
 */
public interface PaymentRepository {

    /**
     * Persists a new payment using its own connection.
     *
     * @param transaction payment to insert
     * @throws me.playernguyen.opteco.core.exception.StorageException if the insert fails
     */
    void create(@NotNull Transaction transaction);

    /**
     * Persists a new payment inside the caller's transaction.
     *
     * @param connection connection participating in the caller's transaction
     * @param transaction payment to insert
     * @throws SQLException if the insert fails
     */
    void create(@NotNull Connection connection, @NotNull Transaction transaction) throws SQLException;

    /**
     * Loads a payment by id inside the caller's transaction.
     *
     * @param connection connection participating in the caller's transaction
     * @param id payment identifier
     * @return the stored payment, or empty when no row exists
     * @throws SQLException if the query fails
     */
    @NotNull
    Optional<Transaction> find(@NotNull Connection connection, @NotNull String id) throws SQLException;

    /**
     * Loads a payment by id in its own connection.
     *
     * @param id payment identifier
     * @return the stored payment, or empty when no row exists
     * @throws me.playernguyen.opteco.core.exception.StorageException if the query fails
     */
    @NotNull
    Optional<Transaction> find(@NotNull String id);

    /**
     * Finds the newest pending payment created by a sender.
     *
     * @param sender sender UUID
     * @return the pending payment, or empty when the sender has none
     * @throws me.playernguyen.opteco.core.exception.StorageException if the query fails
     */
    @NotNull
    Optional<Transaction> findPendingBySender(@NotNull UUID sender);

    /**
     * Finds the newest pending payment for a sender inside the caller's transaction.
     *
     * @param connection connection participating in the caller's transaction
     * @param sender sender UUID
     * @return the pending payment, or empty when the sender has none
     * @throws SQLException if the query fails
     */
    @NotNull
    Optional<Transaction> findPendingBySender(@NotNull Connection connection, @NotNull UUID sender) throws SQLException;

    /**
     * Changes a payment state only when it still has the expected state.
     *
     * <p>The conditional update is a compare-and-set: concurrent callers race to
     * claim the transition and only one can succeed.</p>
     *
     * @param connection connection participating in the caller's transaction
     * @param id payment identifier
     * @param expected state the row must currently have
     * @param next state to store
     * @return {@code true} when exactly one row changed, {@code false} when the state did not match
     * @throws SQLException if the update fails
     */
    boolean transition(
            @NotNull Connection connection,
            @NotNull String id,
            @NotNull TransactionState expected,
            @NotNull TransactionState next)
            throws SQLException;

    /**
     * Changes a payment state using its own connection when it still has the expected state.
     *
     * @param id payment identifier
     * @param expected state the row must currently have
     * @param next state to store
     * @return {@code true} when exactly one row changed, {@code false} when the state did not match
     * @throws me.playernguyen.opteco.core.exception.StorageException if the update fails
     */
    boolean transition(@NotNull String id, @NotNull TransactionState expected, @NotNull TransactionState next);

    /**
     * Marks pending payments whose deadline has passed as expired.
     *
     * @param connection connection participating in the caller's transaction
     * @param now current time in epoch milliseconds
     * @return number of rows moved to {@link TransactionState#EXPIRED}
     * @throws SQLException if the update fails
     */
    int expireOverdue(@NotNull Connection connection, long now) throws SQLException;

    /**
     * Marks overdue pending payments as expired using its own connection.
     *
     * @param now current time in epoch milliseconds
     * @return number of rows moved to {@link TransactionState#EXPIRED}
     * @throws me.playernguyen.opteco.core.exception.StorageException if the update fails
     */
    int expireOverdue(long now);

    /**
     * Loads a sender's most recent payments, newest first.
     *
     * @param sender sender UUID
     * @param limit maximum number of rows to return
     * @return recent payments created by the sender
     * @throws me.playernguyen.opteco.core.exception.StorageException if the query fails
     */
    @NotNull
    List<Transaction> findRecentBySender(@NotNull UUID sender, int limit);
}
