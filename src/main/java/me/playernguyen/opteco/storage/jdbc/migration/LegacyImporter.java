package me.playernguyen.opteco.storage.jdbc.migration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.playernguyen.opteco.config.OptEcoConfig;
import me.playernguyen.opteco.core.exception.StorageException;
import me.playernguyen.opteco.core.model.Money;
import me.playernguyen.opteco.core.model.TransactionState;
import me.playernguyen.opteco.storage.jdbc.SqlDialect;

/**
 * Imports the pre-refactor account and transaction tables.
 *
 * <p>This is the one migration step that stays in Java: legacy balances are
 * {@code DOUBLE} values that must be rounded to {@link Money#SCALE} decimals,
 * uuids and states need validation, and timestamps may be seconds or
 * milliseconds. Expressing that twice in SQL would be both longer and
 * dialect-divergent.</p>
 *
 * <p>{@link #prepare()} runs before the baseline script and renames an
 * old-layout table that occupies a configured name so the script can create the
 * current table. {@link #importLegacy()} runs after the scripts and copies data
 * only when the target table exists and is still empty, which keeps repeated
 * startups harmless.</p>
 *
 * <p>Calls are blocking and expect a caller-owned connection. The class is not
 * thread-safe.</p>
 */
public final class LegacyImporter {

    /** Historical account table used by the old plugin. */
    private static final String LEGACY_ACCOUNT_TABLE = "opteco";

    /** Historical transaction table used by the old plugin. */
    private static final String LEGACY_TRANSACTION_TABLE = "opteco_transaction";

    /** Column that identifies the current account layout. */
    private static final String ACCOUNT_MARKER_COLUMN = "updated_at";

    /** Column that identifies the current transaction layout. */
    private static final String TRANSACTION_MARKER_COLUMN = "expires_at";

    /** Connection used for every statement. */
    private final Connection connection;

    /** Configuration holding the configured table names. */
    private final OptEcoConfig config;

    /** Dialect used for quoting and upserts. */
    private final SqlDialect dialect;

    /** Logger that reports renamed, imported and skipped data. */
    private final Logger logger;

    /**
     * Creates an importer for one migration connection.
     *
     * @param connection the migration connection
     * @param config the configuration holding the table names
     * @param dialect the backend dialect
     * @param logger the plugin logger
     */
    public LegacyImporter(Connection connection, OptEcoConfig config, SqlDialect dialect, Logger logger) {
        this.connection = connection;
        this.config = config;
        this.dialect = dialect;
        this.logger = logger;
    }

    /**
     * Renames an old-layout table that occupies a configured table name.
     *
     * <p>Must run before the baseline script so {@code CREATE TABLE IF NOT
     * EXISTS} creates the current layout instead of silently keeping the old
     * one. A table that already has the marker column is left alone.</p>
     *
     * @throws SQLException when the metadata lookup or rename fails
     * @throws StorageException when the {@code _legacy} name is already taken
     */
    public void prepare() throws SQLException {
        renameOutdated(config.accountTable(), ACCOUNT_MARKER_COLUMN);
        renameOutdated(config.transactionTable(), TRANSACTION_MARKER_COLUMN);
    }

    /**
     * Copies legacy rows into the current tables when they are still empty.
     *
     * @throws SQLException when a read or write fails
     */
    public void importLegacy() throws SQLException {
        String accounts = findSource(config.accountTable(), LEGACY_ACCOUNT_TABLE);
        if (accounts != null) {
            copyAccounts(accounts);
        }
        String transactions = findSource(config.transactionTable(), LEGACY_TRANSACTION_TABLE);
        if (transactions != null) {
            copyTransactions(transactions);
        }
    }

    /**
     * Renames one configured table when it still uses the old layout.
     *
     * @param table the configured table name
     * @param markerColumn the column that only the current layout has
     * @throws SQLException when the metadata lookup or rename fails
     */
    private void renameOutdated(String table, String markerColumn) throws SQLException {
        if (!tableExists(table) || hasColumn(table, markerColumn)) {
            return;
        }
        String legacy = table + "_legacy";
        if (tableExists(legacy)) {
            throw new StorageException("Cannot rename legacy table " + table + ": " + legacy + " already exists");
        }
        execute("ALTER TABLE " + dialect.quote(table) + " RENAME TO " + dialect.quote(legacy));
        logger.info("Renamed legacy table " + table + " to " + legacy);
    }

    /**
     * Picks a legacy source table for an empty target table.
     *
     * @param target the configured table that receives the rows
     * @param historical the historical default table name
     * @return the source table to read, or {@code null} when nothing should be imported
     * @throws SQLException when the metadata lookup fails
     */
    private String findSource(String target, String historical) throws SQLException {
        if (!tableExists(target)) {
            logger.warning("Table " + target + " is missing; skipping legacy import");
            return null;
        }
        if (countRows(target) > 0) {
            logger.fine("Table " + target + " is already populated; skipping legacy import");
            return null;
        }
        String localLegacy = target + "_legacy";
        if (tableExists(localLegacy)) {
            return localLegacy;
        }
        if (tableExists(historical)) {
            return historical;
        }
        return null;
    }

    /**
     * Copies legacy accounts into the current account table.
     *
     * <p>Balances arrive as {@code DOUBLE} values and are rounded with
     * {@link #legacyAmount(double)}. Duplicate uuids cannot exist in the target,
     * so the highest balance per uuid wins; rows with an unparsable uuid are
     * logged and skipped. {@code updated_at} is stamped with the migration time.</p>
     *
     * @param source the legacy table to read
     * @throws SQLException when reading or writing fails
     */
    private void copyAccounts(String source) throws SQLException {
        Map<UUID, LegacyAccount> accounts = new HashMap<>();
        String select = "SELECT player, balance, uuid FROM " + dialect.quote(source);
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(select)) {
            while (resultSet.next()) {
                String rawUuid = resultSet.getString("uuid");
                UUID uuid;
                try {
                    uuid = UUID.fromString(rawUuid);
                } catch (RuntimeException exception) {
                    logger.warning("Skipping invalid account UUID in " + source + ": " + rawUuid);
                    continue;
                }
                BigDecimal balance = legacyAmount(resultSet.getDouble("balance"));
                LegacyAccount current = accounts.get(uuid);
                if (current == null || current.balance().compareTo(balance) < 0) {
                    accounts.put(uuid, new LegacyAccount(resultSet.getString("player"), balance));
                }
            }
        }

        String insert = dialect.setAccountSql(config.accountTable());
        try (PreparedStatement statement = connection.prepareStatement(insert)) {
            for (Map.Entry<UUID, LegacyAccount> entry : accounts.entrySet()) {
                statement.setString(1, entry.getKey().toString());
                statement.setString(
                        2,
                        entry.getValue().name() == null ? "" : entry.getValue().name());
                statement.setLong(3, Money.toStorage(entry.getValue().balance()));
                statement.setLong(4, System.currentTimeMillis());
                statement.addBatch();
            }
            statement.executeBatch();
        }
        if (!accounts.isEmpty()) {
            logger.info("Migrated " + accounts.size() + " account(s) from " + source);
        }
    }

    /**
     * Copies legacy payments into the current transaction table, skipping rows
     * whose uuid, amount or state cannot be parsed.
     *
     * @param source the legacy transaction table to read
     * @throws SQLException when reading or writing fails
     */
    private void copyTransactions(String source) throws SQLException {
        String select = "SELECT transaction_id, sender, receiver, amount, state, time FROM " + dialect.quote(source);
        int migrated = 0;
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(select)) {
            while (resultSet.next()) {
                try {
                    String id = resultSet.getString("transaction_id");
                    UUID sender = UUID.fromString(resultSet.getString("sender"));
                    UUID receiver = UUID.fromString(resultSet.getString("receiver"));
                    BigDecimal amount = legacyAmount(resultSet.getDouble("amount"));
                    TransactionState state = legacyState(resultSet.getString("state"));
                    long createdAt = legacyTime(resultSet.getString("time"));
                    insertLegacyTransaction(id, sender, receiver, amount, state, createdAt);
                    migrated++;
                } catch (RuntimeException exception) {
                    logger.warning("Skipping invalid transaction row in " + source);
                }
            }
        }
        if (migrated > 0) {
            logger.info("Migrated " + migrated + " transaction(s) from " + source);
        }
    }

    /**
     * Inserts one legacy payment through the duplicate-safe insert. The legacy
     * table has no expiry, so {@code created_at} is also used for
     * {@code expires_at}.
     *
     * @param id the legacy transaction id
     * @param sender the paying player
     * @param receiver the receiving player
     * @param amount the legacy amount
     * @param state the mapped state
     * @param createdAt the created time in epoch milliseconds
     * @throws SQLException when the insert fails
     */
    private void insertLegacyTransaction(
            String id, UUID sender, UUID receiver, BigDecimal amount, TransactionState state, long createdAt)
            throws SQLException {
        String sql = dialect.insertTransactionIgnoreSql(config.transactionTable());
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setString(2, sender.toString());
            statement.setString(3, receiver.toString());
            statement.setLong(4, Money.toStorage(amount));
            statement.setString(5, state.name());
            statement.setLong(6, createdAt);
            statement.setLong(7, createdAt);
            statement.executeUpdate();
        }
    }

    /**
     * Maps a legacy state name to a current state.
     *
     * @param raw the legacy state string, possibly {@code null}
     * @return the mapped state; pending payments become expired
     */
    private TransactionState legacyState(String raw) {
        if (raw == null) {
            return TransactionState.CANCELLED;
        }
        try {
            TransactionState state = TransactionState.valueOf(raw.toUpperCase(Locale.ROOT));
            return state == TransactionState.PENDING ? TransactionState.EXPIRED : state;
        } catch (IllegalArgumentException exception) {
            return TransactionState.CANCELLED;
        }
    }

    /**
     * Parses a legacy timestamp string. Values above ten billion are treated as
     * epoch milliseconds, smaller values as epoch seconds; missing or unparsable
     * values fall back to the current time.
     *
     * @param raw the legacy timestamp string, possibly {@code null}
     * @return the timestamp in epoch milliseconds
     */
    private long legacyTime(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return System.currentTimeMillis();
        }
        try {
            long value = Long.parseLong(raw.trim());
            return value > 10_000_000_000L ? value : value * 1000L;
        } catch (NumberFormatException exception) {
            return System.currentTimeMillis();
        }
    }

    /**
     * Converts a legacy floating point balance to the fixed scale used by
     * {@link Money}.
     *
     * @param raw the legacy {@code DOUBLE} amount
     * @return the amount rounded half-up to {@link Money#SCALE} decimals
     */
    private BigDecimal legacyAmount(double raw) {
        return BigDecimal.valueOf(raw).setScale(Money.SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Checks whether a table already has a column; legacy tables are recognised
     * by missing {@code updated_at} or {@code expires_at}.
     *
     * @param table the table to inspect
     * @param column the column name to look for, compared case-insensitively
     * @return {@code true} when the column exists
     * @throws SQLException when the metadata lookup fails
     */
    private boolean hasColumn(String table, String column) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet resultSet = metadata.getColumns(connection.getCatalog(), null, table, null)) {
            while (resultSet.next()) {
                if (column.equalsIgnoreCase(resultSet.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks whether a table exists, comparing names case-insensitively.
     *
     * @param table the table name to look for
     * @return {@code true} when the table is present
     * @throws SQLException when the metadata lookup fails
     */
    private boolean tableExists(String table) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet resultSet = metadata.getTables(connection.getCatalog(), null, "%", new String[] {"TABLE"})) {
            while (resultSet.next()) {
                String name = resultSet.getString("TABLE_NAME");
                if (name != null && name.toLowerCase(Locale.ROOT).equals(table.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Counts the rows of a table, used to avoid copying into a populated target.
     *
     * @param table the table to count, already a trusted name
     * @return the row count, or {@code 0} when nothing is returned
     * @throws SQLException when the count query fails
     */
    private int countRows(String table) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + dialect.quote(table))) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    /**
     * Runs one DDL statement.
     *
     * @param sql the statement to execute
     * @throws SQLException when the statement fails
     */
    private void execute(String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    /**
     * Deduplication holder for one account read from a legacy table.
     */
    @Getter
    @Accessors(fluent = true)
    private static final class LegacyAccount {

        /** Player name as stored by the legacy plugin, possibly {@code null}. */
        private final String name;

        /** Balance rounded to {@link Money#SCALE} decimals. */
        private final BigDecimal balance;

        /**
         * Creates a holder for one legacy account.
         *
         * @param name the stored player name, possibly {@code null}
         * @param balance the rounded balance
         */
        private LegacyAccount(String name, BigDecimal balance) {
            this.name = name;
            this.balance = balance;
        }
    }
}
