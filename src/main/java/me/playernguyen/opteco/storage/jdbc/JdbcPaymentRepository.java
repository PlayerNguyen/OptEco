package me.playernguyen.opteco.storage.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.playernguyen.opteco.core.exception.StorageException;
import me.playernguyen.opteco.core.model.Money;
import me.playernguyen.opteco.core.model.Transaction;
import me.playernguyen.opteco.core.model.TransactionState;
import me.playernguyen.opteco.core.port.PaymentRepository;

/**
 * SQLite and MySQL implementation of {@link PaymentRepository} for the
 * configured transaction table.
 *
 * <p>A row holds {@code id, sender, receiver, amount, state, created_at,
 * expires_at}. Amounts are stored as scaled longs via {@link Money}, states by
 * their {@link TransactionState#name()}, and timestamps as epoch milliseconds
 * taken from the {@link Transaction} itself. State changes use a compare-and-set
 * on the current state, which is what makes confirm, cancel and timeout paths
 * safe when they race.
 *
 * <p>Overloads that accept a {@link Connection} join the caller's transaction
 * and never commit; the convenience overloads borrow a connection from the
 * {@link ConnectionProvider} and translate {@link SQLException} into
 * {@link StorageException}. Every call is blocking and runs on the OptEco
 * database threads.
 */
public final class JdbcPaymentRepository implements PaymentRepository {

    /** Provider used by the convenience overloads that own their connection. */
    private final ConnectionProvider provider;

    /** Validated transaction table name from configuration. */
    private final String table;

    /** Dialect captured from the provider, used for quoting and inserts. */
    private final SqlDialect dialect;

    /**
     * Creates a repository bound to a transaction table.
     *
     * @param provider the connection source and dialect owner
     * @param table the validated transaction table name
     */
    public JdbcPaymentRepository(ConnectionProvider provider, String table) {
        this.provider = provider;
        this.table = table;
        this.dialect = provider.dialect();
    }

    /**
     * Persists a new payment using its own short-lived connection.
     *
     * @param transaction the pending payment to store
     * @throws StorageException when the insert fails, including a duplicate id
     */
    @Override
    public void create(Transaction transaction) {
        try (Connection connection = provider.getConnection()) {
            create(connection, transaction);
        } catch (SQLException exception) {
            throw new StorageException("Could not persist payment " + transaction.id(), exception);
        }
    }

    /**
     * Persists a new payment inside the caller's transaction.
     *
     * @param connection the connection participating in the current transaction
     * @param transaction the pending payment to store
     * @throws SQLException when the insert fails, including a duplicate id
     */
    @Override
    public void create(Connection connection, Transaction transaction) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(dialect.insertTransactionSql(table))) {
            setTransaction(statement, transaction);
            statement.executeUpdate();
        }
    }

    /**
     * Loads a payment by id inside the caller's transaction.
     *
     * @param connection the connection participating in the current transaction
     * @param id the payment id, normally a uuid string
     * @return the stored payment, or empty when the id is unknown
     * @throws SQLException when the query fails
     * @throws StorageException when the row holds an unparsable uuid, state or amount
     */
    @Override
    public Optional<Transaction> find(Connection connection, String id) throws SQLException {
        String sql = "SELECT " + columns() + " FROM " + dialect.quote(table) + " WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(readTransaction(resultSet));
            }
        }
    }

    /**
     * Loads a payment by id using its own short-lived connection.
     *
     * @param id the payment id, normally a uuid string
     * @return the stored payment, or empty when the id is unknown
     * @throws StorageException when the query fails or the row cannot be read
     */
    @Override
    public Optional<Transaction> find(String id) {
        try (Connection connection = provider.getConnection()) {
            return find(connection, id);
        } catch (SQLException exception) {
            throw new StorageException("Could not load payment " + id, exception);
        }
    }

    /**
     * Finds the newest pending payment sent by a player, using its own
     * connection.
     *
     * @param sender the paying player
     * @return the pending payment, or empty when the player has none
     * @throws StorageException when the query fails or the row cannot be read
     */
    @Override
    public Optional<Transaction> findPendingBySender(UUID sender) {
        try (Connection connection = provider.getConnection()) {
            return findPendingBySender(connection, sender);
        } catch (SQLException exception) {
            throw new StorageException("Could not load pending payment for " + sender, exception);
        }
    }

    /**
     * Finds the newest pending payment sent by a player inside the caller's
     * transaction.
     *
     * @param connection the connection participating in the current transaction
     * @param sender the paying player
     * @return the pending payment, or empty when the player has none
     * @throws SQLException when the query fails
     * @throws StorageException when the row cannot be read
     */
    @Override
    public Optional<Transaction> findPendingBySender(Connection connection, UUID sender) throws SQLException {
        String sql = "SELECT " + columns() + " FROM " + dialect.quote(table)
                + " WHERE sender = ? AND state = ? ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sender.toString());
            statement.setString(2, TransactionState.PENDING.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(readTransaction(resultSet));
            }
        }
    }

    /**
     * Moves a payment from one state to another inside the caller's transaction.
     *
     * <p>The expected state is part of the {@code WHERE} clause, so only one of
     * several racing callers can win the transition.
     *
     * @param connection the connection participating in the current transaction
     * @param id the payment id
     * @param expected the state the row must currently have
     * @param next the state to write
     * @return {@code true} when exactly one row changed state
     * @throws SQLException when the update fails
     */
    @Override
    public boolean transition(Connection connection, String id, TransactionState expected, TransactionState next)
            throws SQLException {
        String sql = "UPDATE " + dialect.quote(table) + " SET state = ? WHERE id = ? AND state = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, next.name());
            statement.setString(2, id);
            statement.setString(3, expected.name());
            return statement.executeUpdate() == 1;
        }
    }

    /**
     * Moves a payment from one state to another using its own short-lived
     * connection.
     *
     * @param id the payment id
     * @param expected the state the row must currently have
     * @param next the state to write
     * @return {@code true} when exactly one row changed state
     * @throws StorageException when the update fails
     */
    @Override
    public boolean transition(String id, TransactionState expected, TransactionState next) {
        try (Connection connection = provider.getConnection()) {
            return transition(connection, id, expected, next);
        } catch (SQLException exception) {
            throw new StorageException("Could not update payment " + id, exception);
        }
    }

    /**
     * Marks every pending payment whose expiry is at or before {@code now} as
     * expired, inside the caller's transaction.
     *
     * @param connection the connection participating in the current transaction
     * @param now the current time in epoch milliseconds
     * @return the number of rows that changed state
     * @throws SQLException when the update fails
     */
    @Override
    public int expireOverdue(Connection connection, long now) throws SQLException {
        String sql = "UPDATE " + dialect.quote(table) + " SET state = ? WHERE state = ? AND expires_at <= ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TransactionState.EXPIRED.name());
            statement.setString(2, TransactionState.PENDING.name());
            statement.setLong(3, now);
            return statement.executeUpdate();
        }
    }

    /**
     * Marks overdue pending payments as expired using its own short-lived
     * connection.
     *
     * @param now the current time in epoch milliseconds
     * @return the number of rows that changed state
     * @throws StorageException when the update fails
     */
    @Override
    public int expireOverdue(long now) {
        try (Connection connection = provider.getConnection()) {
            return expireOverdue(connection, now);
        } catch (SQLException exception) {
            throw new StorageException("Could not expire pending payments", exception);
        }
    }

    /**
     * Lists the most recent payments sent by a player, newest first.
     *
     * <p>The limit is clamped to {@code 1..100}.
     *
     * @param sender the paying player
     * @param limit the requested number of entries
     * @return recent payments of any state, possibly fewer than {@code limit}
     * @throws StorageException when the query fails or a row cannot be read
     */
    @Override
    public List<Transaction> findRecentBySender(UUID sender, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        String sql = "SELECT " + columns() + " FROM " + dialect.quote(table)
                + " WHERE sender = ? ORDER BY created_at DESC LIMIT ?";
        List<Transaction> transactions = new ArrayList<>();
        try (Connection connection = provider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sender.toString());
            statement.setInt(2, safeLimit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    transactions.add(readTransaction(resultSet));
                }
            }
        } catch (SQLException exception) {
            throw new StorageException("Could not load payment history", exception);
        }
        return transactions;
    }

    /**
     * Binds the seven insert parameters in table column order.
     *
     * @param statement the prepared insert
     * @param transaction the payment supplying the values
     * @throws SQLException when a parameter cannot be bound
     */
    private void setTransaction(PreparedStatement statement, Transaction transaction) throws SQLException {
        statement.setString(1, transaction.id());
        statement.setString(2, transaction.sender().toString());
        statement.setString(3, transaction.receiver().toString());
        statement.setLong(4, Money.toStorage(transaction.amount()));
        statement.setString(5, transaction.state().name());
        statement.setLong(6, transaction.createdAt());
        statement.setLong(7, transaction.expiresAt());
    }

    /**
     * Maps the current row to a domain transaction.
     *
     * @param resultSet a result set positioned on a payment row
     * @return the parsed transaction
     * @throws SQLException when a column cannot be read
     * @throws StorageException when a uuid, amount or state value is invalid
     */
    private Transaction readTransaction(ResultSet resultSet) throws SQLException {
        try {
            return new Transaction(
                    resultSet.getString("id"),
                    UUID.fromString(resultSet.getString("sender")),
                    UUID.fromString(resultSet.getString("receiver")),
                    Money.fromStorage(resultSet.getLong("amount")),
                    TransactionState.valueOf(resultSet.getString("state")),
                    resultSet.getLong("created_at"),
                    resultSet.getLong("expires_at"));
        } catch (IllegalArgumentException exception) {
            throw new StorageException("Invalid transaction row in database", exception);
        }
    }

    /**
     * Selects the shared transaction column list.
     *
     * @return the comma-separated column names in row order
     */
    private String columns() {
        return "id, sender, receiver, amount, state, created_at, expires_at";
    }
}
