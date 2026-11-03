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

public final class JdbcPaymentRepository implements PaymentRepository {

    private final ConnectionProvider provider;
    private final String table;
    private final SqlDialect dialect;

    public JdbcPaymentRepository(ConnectionProvider provider, String table) {
        this.provider = provider;
        this.table = table;
        this.dialect = provider.dialect();
    }

    @Override
    public void create(Transaction transaction) {
        try (Connection connection = provider.getConnection()) {
            create(connection, transaction);
        } catch (SQLException exception) {
            throw new StorageException("Could not persist payment " + transaction.id(), exception);
        }
    }

    @Override
    public void create(Connection connection, Transaction transaction) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(dialect.insertTransactionSql(table))) {
            setTransaction(statement, transaction);
            statement.executeUpdate();
        }
    }

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

    @Override
    public Optional<Transaction> find(String id) {
        try (Connection connection = provider.getConnection()) {
            return find(connection, id);
        } catch (SQLException exception) {
            throw new StorageException("Could not load payment " + id, exception);
        }
    }

    @Override
    public Optional<Transaction> findPendingBySender(UUID sender) {
        try (Connection connection = provider.getConnection()) {
            return findPendingBySender(connection, sender);
        } catch (SQLException exception) {
            throw new StorageException("Could not load pending payment for " + sender, exception);
        }
    }

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

    @Override
    public boolean transition(String id, TransactionState expected, TransactionState next) {
        try (Connection connection = provider.getConnection()) {
            return transition(connection, id, expected, next);
        } catch (SQLException exception) {
            throw new StorageException("Could not update payment " + id, exception);
        }
    }

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

    @Override
    public int expireOverdue(long now) {
        try (Connection connection = provider.getConnection()) {
            return expireOverdue(connection, now);
        } catch (SQLException exception) {
            throw new StorageException("Could not expire pending payments", exception);
        }
    }

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

    private void setTransaction(PreparedStatement statement, Transaction transaction) throws SQLException {
        statement.setString(1, transaction.id());
        statement.setString(2, transaction.sender().toString());
        statement.setString(3, transaction.receiver().toString());
        statement.setLong(4, Money.toStorage(transaction.amount()));
        statement.setString(5, transaction.state().name());
        statement.setLong(6, transaction.createdAt());
        statement.setLong(7, transaction.expiresAt());
    }

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

    private String columns() {
        return "id, sender, receiver, amount, state, created_at, expires_at";
    }
}
