package me.playernguyen.opteco.storage.jdbc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.playernguyen.opteco.core.exception.StorageException;
import me.playernguyen.opteco.core.model.Account;
import me.playernguyen.opteco.core.model.Money;
import me.playernguyen.opteco.core.port.AccountRepository;
import me.playernguyen.opteco.core.port.Clock;

public final class JdbcAccountRepository implements AccountRepository {

    private final ConnectionProvider provider;
    private final String table;
    private final SqlDialect dialect;
    private final Clock clock;

    public JdbcAccountRepository(ConnectionProvider provider, String table, Clock clock) {
        this.provider = provider;
        this.table = table;
        this.dialect = provider.dialect();
        this.clock = clock;
    }

    @Override
    public Optional<Account> find(UUID uuid) {
        try (Connection connection = provider.getConnection()) {
            return find(connection, uuid);
        } catch (SQLException exception) {
            throw new StorageException("Could not load account " + uuid, exception);
        }
    }

    @Override
    public Optional<Account> find(Connection connection, UUID uuid) throws SQLException {
        String sql = "SELECT player, balance FROM " + dialect.quote(table) + " WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Account(
                        uuid, resultSet.getString("player"), Money.fromStorage(resultSet.getLong("balance"))));
            }
        }
    }

    @Override
    public Account getOrCreate(UUID uuid, String name, BigDecimal initialBalance) {
        try (Connection connection = provider.getConnection()) {
            return getOrCreate(connection, uuid, name, initialBalance);
        } catch (SQLException exception) {
            throw new StorageException("Could not create account " + uuid, exception);
        }
    }

    @Override
    public Account getOrCreate(Connection connection, UUID uuid, String name, BigDecimal initialBalance)
            throws SQLException {
        String safeName = name == null ? "" : name;
        try (PreparedStatement statement = connection.prepareStatement(dialect.insertAccountIfAbsentSql(table))) {
            statement.setString(1, uuid.toString());
            statement.setString(2, safeName);
            statement.setLong(3, Money.toStorage(initialBalance));
            statement.setLong(4, clock.now());
            statement.executeUpdate();
        }

        Account account = find(connection, uuid)
                .orElseThrow(() -> new StorageException("Account " + uuid + " disappeared after creation"));
        if (!safeName.isEmpty() && !safeName.equals(account.name())) {
            updateName(connection, uuid, safeName);
            account = account.withName(safeName);
        }
        return account;
    }

    @Override
    public void set(Connection connection, UUID uuid, String name, BigDecimal balance) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(dialect.setAccountSql(table))) {
            statement.setString(1, uuid.toString());
            statement.setString(2, name == null ? "" : name);
            statement.setLong(3, Money.toStorage(balance));
            statement.setLong(4, clock.now());
            statement.executeUpdate();
        }
    }

    @Override
    public Optional<BigDecimal> debit(Connection connection, UUID uuid, BigDecimal amount, BigDecimal floor)
            throws SQLException {
        String sql = "UPDATE " + dialect.quote(table)
                + " SET balance = balance - ?, updated_at = ? "
                + "WHERE uuid = ? AND balance - ? >= ?";
        long storedAmount = Money.toStorage(amount);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, storedAmount);
            statement.setLong(2, clock.now());
            statement.setString(3, uuid.toString());
            statement.setLong(4, storedAmount);
            statement.setLong(5, Money.toStorage(floor));
            if (statement.executeUpdate() == 0) {
                return Optional.empty();
            }
        }
        return findBalance(connection, uuid);
    }

    @Override
    public Optional<BigDecimal> credit(Connection connection, UUID uuid, String name, BigDecimal amount)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(dialect.creditAccountSql(table))) {
            statement.setString(1, uuid.toString());
            statement.setString(2, name == null ? "" : name);
            statement.setLong(3, Money.toStorage(amount));
            statement.setLong(4, clock.now());
            statement.executeUpdate();
        }
        return findBalance(connection, uuid);
    }

    @Override
    public List<Account> top(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 1000));
        String sql = "SELECT uuid, player, balance FROM " + dialect.quote(table) + " ORDER BY balance DESC LIMIT ?";
        List<Account> accounts = new ArrayList<>();
        try (Connection connection = provider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, safeLimit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    try {
                        accounts.add(new Account(
                                UUID.fromString(resultSet.getString("uuid")),
                                resultSet.getString("player"),
                                Money.fromStorage(resultSet.getLong("balance"))));
                    } catch (IllegalArgumentException ignored) {
                        // Ignore invalid legacy UUID rows instead of breaking the leaderboard.
                    }
                }
            }
        } catch (SQLException exception) {
            throw new StorageException("Could not load leaderboard", exception);
        }
        return accounts;
    }

    @Override
    public boolean exists(UUID uuid) {
        String sql = "SELECT 1 FROM " + dialect.quote(table) + " WHERE uuid = ? LIMIT 1";
        try (Connection connection = provider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new StorageException("Could not check account " + uuid, exception);
        }
    }

    private void updateName(Connection connection, UUID uuid, String name) throws SQLException {
        String sql = "UPDATE " + dialect.quote(table) + " SET player = ? WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
        }
    }

    private Optional<BigDecimal> findBalance(Connection connection, UUID uuid) throws SQLException {
        String sql = "SELECT balance FROM " + dialect.quote(table) + " WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(Money.fromStorage(resultSet.getLong("balance")));
            }
        }
    }
}
