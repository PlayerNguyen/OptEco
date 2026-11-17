package me.playernguyen.opteco.storage.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import me.playernguyen.opteco.core.exception.StorageException;
import me.playernguyen.opteco.core.model.BalanceChangeReason;
import me.playernguyen.opteco.core.model.LedgerEntry;
import me.playernguyen.opteco.core.model.Money;
import me.playernguyen.opteco.core.port.LedgerRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * SQLite and MySQL implementation of {@link LedgerRepository} for the configured
 * ledger table.
 *
 * <p>Amounts and balances cross the database boundary as scaled longs through
 * {@link Money#toStorage(java.math.BigDecimal)} and
 * {@link Money#fromStorage(long)}; reasons are stored by their
 * {@link BalanceChangeReason#name()}. The table name comes from configuration
 * and is already validated as an SQL identifier; values always travel through
 * prepared statements. Rows are only inserted and read, never updated or
 * deleted.</p>
 *
 * <p>{@link #append(Connection, LedgerEntry)} joins the caller's transaction
 * and never commits. The read methods borrow a connection from the
 * {@link ConnectionProvider} and translate {@link SQLException} into
 * {@link StorageException}. Every call is blocking and runs on the OptEco
 * database threads.</p>
 */
public final class JdbcLedgerRepository implements LedgerRepository {

    /** Largest page size accepted by {@link #findByPlayer(UUID, int, int)}. */
    private static final int MAX_LIMIT = 1000;

    /** Provider used by the read methods that own their connection. */
    private final ConnectionProvider provider;

    /** Validated ledger table name from configuration. */
    private final String table;

    /** Dialect captured from the provider, used for quoting. */
    private final SqlDialect dialect;

    /**
     * Creates a repository bound to a ledger table.
     *
     * @param provider the connection source and dialect owner
     * @param table the validated ledger table name
     */
    public JdbcLedgerRepository(ConnectionProvider provider, String table) {
        this.provider = provider;
        this.table = table;
        this.dialect = provider.dialect();
    }

    /**
     * Inserts one ledger row inside the caller's transaction.
     *
     * @param connection connection participating in the caller's transaction
     * @param entry entry to append
     * @throws SQLException if the insert fails
     */
    @Override
    public void append(@NotNull Connection connection, @NotNull LedgerEntry entry) throws SQLException {
        String sql = "INSERT INTO " + dialect.quote(table)
                + " (id, player, counterparty, amount, balance_before, balance_after, reason,"
                + " transaction_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            UUID counterparty = entry.counterparty();
            statement.setString(1, entry.id());
            statement.setString(2, entry.player().toString());
            setNullableString(statement, 3, counterparty == null ? null : counterparty.toString());
            statement.setLong(4, Money.toStorage(entry.amount()));
            statement.setLong(5, Money.toStorage(entry.balanceBefore()));
            statement.setLong(6, Money.toStorage(entry.balanceAfter()));
            statement.setString(7, entry.reason().name());
            setNullableString(statement, 8, entry.transactionId());
            statement.setLong(9, entry.createdAt());
            statement.executeUpdate();
        }
    }

    /**
     * Loads one page of a player's ledger, newest first.
     *
     * <p>The limit is clamped to {@code 1..1000}; a negative offset is treated as
     * zero. The creation time is the primary sort key and the id breaks ties so
     * pagination stays stable when several rows share a timestamp.</p>
     *
     * @param player player whose entries are read
     * @param limit maximum number of rows to return
     * @param offset number of newest rows to skip
     * @return entries ordered by creation time descending, possibly fewer than {@code limit}
     * @throws StorageException if the query fails or a row cannot be read
     */
    @Override
    @NotNull
    public List<LedgerEntry> findByPlayer(@NotNull UUID player, int limit, int offset) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        int safeOffset = Math.max(0, offset);
        String sql = "SELECT id, player, counterparty, amount, balance_before, balance_after, reason,"
                + " transaction_id, created_at FROM " + dialect.quote(table)
                + " WHERE player = ? ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?";
        List<LedgerEntry> entries = new ArrayList<>();
        try (Connection connection = provider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, player.toString());
            statement.setInt(2, safeLimit);
            statement.setInt(3, safeOffset);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(readEntry(resultSet));
                }
            }
        } catch (SQLException exception) {
            throw new StorageException("Could not load ledger history for " + player, exception);
        }
        return entries;
    }

    /**
     * Counts a player's ledger rows.
     *
     * @param player player whose entries are counted
     * @return total number of entries for the player
     * @throws StorageException if the query fails
     */
    @Override
    public long countByPlayer(@NotNull UUID player) {
        String sql = "SELECT COUNT(*) FROM " + dialect.quote(table) + " WHERE player = ?";
        try (Connection connection = provider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, player.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        } catch (SQLException exception) {
            throw new StorageException("Could not count ledger history for " + player, exception);
        }
    }

    /**
     * Binds a nullable string parameter.
     *
     * @param statement the prepared statement to update
     * @param index one-based parameter index
     * @param value the value to bind, or null
     * @throws SQLException if the parameter cannot be set
     */
    private void setNullableString(PreparedStatement statement, int index, @Nullable String value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    /**
     * Maps the current row to a domain ledger entry.
     *
     * @param resultSet a result set positioned on a ledger row
     * @return the parsed entry
     * @throws SQLException when a column cannot be read
     * @throws StorageException when a uuid, amount or reason value is invalid
     */
    private LedgerEntry readEntry(ResultSet resultSet) throws SQLException {
        try {
            String counterparty = resultSet.getString("counterparty");
            String transactionId = resultSet.getString("transaction_id");
            return new LedgerEntry(
                    resultSet.getString("id"),
                    UUID.fromString(resultSet.getString("player")),
                    counterparty == null ? null : UUID.fromString(counterparty),
                    Money.fromStorage(resultSet.getLong("amount")),
                    Money.fromStorage(resultSet.getLong("balance_before")),
                    Money.fromStorage(resultSet.getLong("balance_after")),
                    BalanceChangeReason.valueOf(resultSet.getString("reason")),
                    transactionId,
                    resultSet.getLong("created_at"));
        } catch (IllegalArgumentException exception) {
            throw new StorageException("Invalid ledger row in database", exception);
        }
    }
}
