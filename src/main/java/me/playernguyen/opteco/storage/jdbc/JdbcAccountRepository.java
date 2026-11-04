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

/**
 * SQLite and MySQL implementation of {@link AccountRepository} for the
 * configured account table.
 *
 * <p>Balances cross the database boundary as scaled longs through
 * {@link Money#toStorage(BigDecimal)} and {@link Money#fromStorage(long)}, so no
 * floating point value ever reaches storage. {@code updated_at} is filled from
 * the injected {@link Clock}. The table name comes from configuration and is
 * already validated as an SQL identifier; values always travel through prepared
 * statements.
 *
 * <p>Overloads that accept a {@link Connection} join the caller's transaction
 * and never commit, while the overloads that take only a uuid borrow a
 * connection from the {@link ConnectionProvider} and translate
 * {@link SQLException} into {@link StorageException}. Every call is blocking and
 * runs on the OptEco database threads.
 */
public final class JdbcAccountRepository implements AccountRepository {

    /** Provider used by the convenience overloads that own their connection. */
    private final ConnectionProvider provider;

    /** Validated account table name from configuration. */
    private final String table;

    /** Dialect captured from the provider, used for quoting and upserts. */
    private final SqlDialect dialect;

    /** Clock that supplies {@code updated_at} timestamps in epoch milliseconds. */
    private final Clock clock;

    /**
     * Creates a repository bound to an account table.
     *
     * @param provider the connection source and dialect owner
     * @param table the validated account table name
     * @param clock clock used for {@code updated_at} values
     */
    public JdbcAccountRepository(ConnectionProvider provider, String table, Clock clock) {
        this.provider = provider;
        this.table = table;
        this.dialect = provider.dialect();
        this.clock = clock;
    }

    /**
     * Loads an account using its own short-lived connection.
     *
     * @param uuid the account owner
     * @return the stored account, or empty when the player has no row yet
     * @throws StorageException when the query fails
     */
    @Override
    public Optional<Account> find(UUID uuid) {
        try (Connection connection = provider.getConnection()) {
            return find(connection, uuid);
        } catch (SQLException exception) {
            throw new StorageException("Could not load account " + uuid, exception);
        }
    }

    /**
     * Loads an account inside the caller's transaction.
     *
     * @param connection the connection participating in the current transaction
     * @param uuid the account owner
     * @return the stored account, or empty when no row exists
     * @throws SQLException when the query fails
     */
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

    /**
     * Creates the account when it is missing, using its own connection.
     *
     * @param uuid the account owner
     * @param name the current player name, or {@code null} when unknown
     * @param initialBalance the balance written only when the row is created
     * @return the existing or newly created account
     * @throws StorageException when the insert or follow-up read fails
     */
    @Override
    public Account getOrCreate(UUID uuid, String name, BigDecimal initialBalance) {
        try (Connection connection = provider.getConnection()) {
            return getOrCreate(connection, uuid, name, initialBalance);
        } catch (SQLException exception) {
            throw new StorageException("Could not create account " + uuid, exception);
        }
    }

    /**
     * Creates the account when it is missing inside the caller's transaction,
     * then refreshes the stored name if a non-empty name was supplied.
     *
     * @param connection the connection participating in the current transaction
     * @param uuid the account owner
     * @param name the current player name, or {@code null} when unknown
     * @param initialBalance the balance written only when the row is created
     * @return the existing or newly created account
     * @throws SQLException when the insert, read or name update fails
     */
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

    /**
     * Overwrites the stored name and balance inside the caller's transaction,
     * creating the row when it does not exist yet.
     *
     * @param connection the connection participating in the current transaction
     * @param uuid the account owner
     * @param name the player name, or {@code null} to store an empty name
     * @param balance the new absolute balance
     * @throws SQLException when the upsert fails
     */
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

    /**
     * Atomically subtracts {@code amount} only when the resulting balance stays
     * at or above {@code floor}.
     *
     * <p>The guard lives in the {@code WHERE} clause, so two concurrent debits
     * cannot both pass it.
     *
     * @param connection the connection participating in the current transaction
     * @param uuid the account owner
     * @param amount the amount to remove
     * @param floor the lowest balance the account may keep
     * @return the new balance, or empty when the row is missing or the guard rejected
     *         the debit
     * @throws SQLException when the update or follow-up read fails
     */
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

    /**
     * Adds {@code amount} to the balance, creating the row with that amount when
     * it does not exist yet.
     *
     * <p>An empty or {@code null} name leaves the stored player name unchanged.
     *
     * @param connection the connection participating in the current transaction
     * @param uuid the account owner
     * @param name the player name, or {@code null} when unknown
     * @param amount the amount to add
     * @return the new balance, or empty when the row disappeared after the upsert
     * @throws SQLException when the upsert or follow-up read fails
     */
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

    /**
     * Reads the leaderboard in descending balance order.
     *
     * <p>The limit is clamped to {@code 1..1000}. Rows whose uuid cannot be
     * parsed are legacy leftovers; they are skipped instead of breaking the whole
     * leaderboard.
     *
     * @param limit the requested number of entries
     * @return the highest balances, possibly fewer than {@code limit}
     * @throws StorageException when the query fails
     */
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

    /**
     * Checks whether an account row exists without loading the balance.
     *
     * @param uuid the account owner
     * @return {@code true} when a row exists
     * @throws StorageException when the query fails
     */
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

    /**
     * Stores a new player name for an existing account inside the caller's
     * transaction.
     *
     * @param connection the connection participating in the current transaction
     * @param uuid the account owner
     * @param name the name to store
     * @throws SQLException when the update fails
     */
    private void updateName(Connection connection, UUID uuid, String name) throws SQLException {
        String sql = "UPDATE " + dialect.quote(table) + " SET player = ? WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
        }
    }

    /**
     * Re-reads a balance inside the caller's transaction.
     *
     * @param connection the connection participating in the current transaction
     * @param uuid the account owner
     * @return the stored balance, or empty when the row is missing
     * @throws SQLException when the query fails
     */
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
