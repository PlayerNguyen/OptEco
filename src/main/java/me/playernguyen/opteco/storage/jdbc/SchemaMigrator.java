package me.playernguyen.opteco.storage.jdbc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import me.playernguyen.opteco.config.OptEcoConfig;
import me.playernguyen.opteco.core.exception.StorageException;
import me.playernguyen.opteco.core.model.Money;
import me.playernguyen.opteco.core.model.TransactionState;

/**
 * Creates the current schema and imports the pre-refactor account/transaction
 * tables when they are found. Legacy tables are left in place as a backup.
 *
 * <p>The current account table (the configured name, {@code opteco_accounts} by
 * default) stores {@code uuid VARCHAR(36)} as primary key plus {@code player},
 * {@code balance BIGINT} and {@code updated_at BIGINT}. The transaction table
 * (the configured name, {@code opteco_transactions} by default) stores
 * {@code id VARCHAR(36)} as primary key plus {@code sender},
 * {@code receiver}, {@code amount BIGINT}, {@code state VARCHAR(16)},
 * {@code created_at BIGINT} and {@code expires_at BIGINT}. A one-row
 * {@code opteco_schema} table records the schema version.
 *
 * <p>Rows are imported from the configured names or from the historical
 * {@code opteco} and {@code opteco_transaction} tables. Legacy balances are
 * {@code DOUBLE} values and are rounded to {@link Money#SCALE} decimals, legacy
 * timestamps may be seconds or milliseconds, and still-pending legacy payments
 * become expired because their confirmation window is gone. The migration is
 * idempotent, skips unparsable rows instead of failing, and can resume when a
 * previous run was interrupted between renaming and copying. It runs once
 * during startup before any repository is used; calls are blocking and the class
 * is not thread-safe.
 */
public final class SchemaMigrator {

    /** Bookkeeping table holding the single schema version row. */
    private static final String SCHEMA_TABLE = "opteco_schema";

    /** Account table used by the pre-refactor plugin. */
    private static final String LEGACY_ACCOUNT_TABLE = "opteco";

    /** Transaction table used by the pre-refactor plugin. */
    private static final String LEGACY_TRANSACTION_TABLE = "opteco_transaction";

    /** Connection source for every migration step. */
    private final ConnectionProvider provider;

    /** Startup configuration snapshot holding the table names. */
    private final OptEcoConfig config;

    /** Dialect used for quoting and upserts during the migration. */
    private final SqlDialect dialect;

    /** Plugin logger that reports imported, renamed and skipped data. */
    private final Logger logger;

    /**
     * Creates a migrator for one backend and configuration snapshot.
     *
     * @param provider the connection source and dialect owner
     * @param config the startup configuration holding the table names
     * @param logger the plugin logger used for migration progress
     */
    public SchemaMigrator(ConnectionProvider provider, OptEcoConfig config, Logger logger) {
        this.provider = provider;
        this.config = config;
        this.dialect = provider.dialect();
        this.logger = logger;
    }

    /**
     * Creates the schema table, both data tables, their indexes and the schema
     * version, importing legacy rows on the way.
     *
     * <p>Runs on a single auto-commit connection; each step is individually
     * idempotent rather than wrapped in one long transaction.
     *
     * @throws StorageException when a schema statement fails
     */
    public void migrate() {
        try (Connection connection = provider.getConnection()) {
            connection.setAutoCommit(true);
            createSchemaTable(connection);
            Set<String> tables = tableNames(connection);
            migrateAccounts(connection, tables);
            migrateTransactions(connection, tables);
            createIndexes(connection);
            writeSchemaVersion(connection);
        } catch (SQLException exception) {
            throw new StorageException("Could not migrate the " + provider.description() + " schema", exception);
        }
    }

    /**
     * Ensures the account table exists and copies legacy accounts into it when
     * needed.
     *
     * <p>A source table with an {@code updated_at} column already uses the
     * current layout, so it is copied only when it sits under a legacy name or
     * when it finishes an interrupted migration. A source without that column is
     * the legacy layout: when it already carries the configured name it is first
     * renamed to {@code <name>_legacy} so the new table can take its place.
     *
     * @param connection the migration connection
     * @param tables lower-cased table names already present in the database
     * @throws SQLException when a statement fails
     */
    private void migrateAccounts(Connection connection, Set<String> tables) throws SQLException {
        String configured = config.accountTable();
        String source = resolveSource(tables, configured, LEGACY_ACCOUNT_TABLE);
        if (source == null) {
            createAccounts(connection);
            return;
        }

        if (hasColumn(connection, source, "updated_at")) {
            if (!source.equalsIgnoreCase(configured)) {
                createAccounts(connection);
                copyAccounts(connection, source);
            } else {
                String interrupted = findInterruptedMigration(tables, configured, LEGACY_ACCOUNT_TABLE);
                if (interrupted != null && countRows(connection, configured) == 0) {
                    copyAccounts(connection, interrupted);
                }
            }
            return;
        }

        if (source.equalsIgnoreCase(configured)) {
            String legacy = source + "_legacy";
            renameTable(connection, source, legacy);
            createAccounts(connection);
            copyAccounts(connection, legacy);
        } else {
            createAccounts(connection);
            copyAccounts(connection, source);
        }
    }

    /**
     * Ensures the transaction table exists and copies legacy payments into it
     * when needed.
     *
     * <p>A source table with an {@code expires_at} column already uses the
     * current layout, so it is copied only when it sits under a legacy name or
     * when it finishes an interrupted migration. A source without that column is
     * the legacy layout and is renamed to {@code <name>_legacy} first when it
     * already carries the configured name.
     *
     * @param connection the migration connection
     * @param tables lower-cased table names already present in the database
     * @throws SQLException when a statement fails
     */
    private void migrateTransactions(Connection connection, Set<String> tables) throws SQLException {
        String configured = config.transactionTable();
        String source = resolveSource(tables, configured, LEGACY_TRANSACTION_TABLE);
        if (source == null) {
            createTransactions(connection);
            return;
        }

        if (hasColumn(connection, source, "expires_at")) {
            if (!source.equalsIgnoreCase(configured)) {
                createTransactions(connection);
                copyTransactions(connection, source);
            } else {
                String interrupted = findInterruptedMigration(tables, configured, LEGACY_TRANSACTION_TABLE);
                if (interrupted != null && countRows(connection, configured) == 0) {
                    copyTransactions(connection, interrupted);
                }
            }
            return;
        }

        if (source.equalsIgnoreCase(configured)) {
            String legacy = source + "_legacy";
            renameTable(connection, source, legacy);
            createTransactions(connection);
            copyTransactions(connection, legacy);
        } else {
            createTransactions(connection);
            copyTransactions(connection, source);
        }
    }

    /**
     * Picks the table to import from: the configured name when present,
     * otherwise the historical default.
     *
     * @param tables lower-cased table names present in the database
     * @param configured the configured table name
     * @param legacy the historical default table name
     * @return the existing source table, or {@code null} when neither exists
     */
    private String resolveSource(Set<String> tables, String configured, String legacy) {
        if (tables.contains(configured.toLowerCase(Locale.ROOT))) {
            return configured;
        }
        if (tables.contains(legacy)) {
            return legacy;
        }
        return null;
    }

    /**
     * Detects a {@code <name>_legacy} or historical table left behind by an
     * interrupted migration.
     *
     * @param tables lower-cased table names present in the database
     * @param configured the configured table name
     * @param legacyDefault the historical default table name
     * @return the leftover table to resume from, or {@code null} when there is none
     */
    private String findInterruptedMigration(Set<String> tables, String configured, String legacyDefault) {
        String localLegacy = configured + "_legacy";
        if (tables.contains(localLegacy.toLowerCase(Locale.ROOT))) {
            return localLegacy;
        }
        if (tables.contains(legacyDefault)) {
            return legacyDefault;
        }
        return null;
    }

    /**
     * Counts the rows of a table, used to avoid copying into a populated target.
     *
     * @param connection the migration connection
     * @param table the table to count, already a trusted name
     * @return the row count, or {@code 0} when nothing is returned
     * @throws SQLException when the count query fails
     */
    private int countRows(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + dialect.quote(table))) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    /**
     * Creates the current account table when absent.
     *
     * @param connection the migration connection
     * @throws SQLException when the table cannot be created
     */
    private void createAccounts(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + dialect.quote(config.accountTable()) + " ("
                + "uuid VARCHAR(36) NOT NULL PRIMARY KEY, "
                + "player VARCHAR(64) NOT NULL, "
                + "balance BIGINT NOT NULL, "
                + "updated_at BIGINT NOT NULL)";
        execute(connection, sql);
    }

    /**
     * Creates the current transaction table when absent.
     *
     * @param connection the migration connection
     * @throws SQLException when the table cannot be created
     */
    private void createTransactions(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + dialect.quote(config.transactionTable()) + " ("
                + "id VARCHAR(36) NOT NULL PRIMARY KEY, "
                + "sender VARCHAR(36) NOT NULL, "
                + "receiver VARCHAR(36) NOT NULL, "
                + "amount BIGINT NOT NULL, "
                + "state VARCHAR(16) NOT NULL, "
                + "created_at BIGINT NOT NULL, "
                + "expires_at BIGINT NOT NULL)";
        execute(connection, sql);
    }

    /**
     * Copies legacy accounts into the current table.
     *
     * <p>Balances arrive as {@code DOUBLE} values and are rounded with
     * {@link #legacyAmount(double)}. Duplicate uuids cannot exist in the target,
     * so the highest balance per uuid wins; rows with an unparsable uuid are
     * logged and skipped. {@code updated_at} is stamped with the migration time.
     *
     * @param connection the migration connection
     * @param source the legacy table to read
     * @throws SQLException when reading or writing fails
     */
    private void copyAccounts(Connection connection, String source) throws SQLException {
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
                if (current == null || current.balance.compareTo(balance) < 0) {
                    accounts.put(uuid, new LegacyAccount(resultSet.getString("player"), balance));
                }
            }
        }

        String insert = dialect.setAccountSql(config.accountTable());
        try (PreparedStatement statement = connection.prepareStatement(insert)) {
            for (Map.Entry<UUID, LegacyAccount> entry : accounts.entrySet()) {
                statement.setString(1, entry.getKey().toString());
                statement.setString(2, entry.getValue().name == null ? "" : entry.getValue().name);
                statement.setLong(3, Money.toStorage(entry.getValue().balance));
                statement.setLong(4, System.currentTimeMillis());
                statement.addBatch();
            }
            statement.executeBatch();
        }
        logger.info("Migrated " + accounts.size() + " account(s) from " + source);
    }

    /**
     * Copies legacy payments into the current table, skipping rows whose uuid,
     * amount or state cannot be parsed.
     *
     * @param connection the migration connection
     * @param source the legacy transaction table to read
     * @throws SQLException when reading or writing fails
     */
    private void copyTransactions(Connection connection, String source) throws SQLException {
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
                    insertLegacyTransaction(connection, id, sender, receiver, amount, state, createdAt);
                    migrated++;
                } catch (RuntimeException exception) {
                    logger.warning("Skipping invalid transaction row in " + source);
                }
            }
        }
        logger.info("Migrated " + migrated + " transaction(s) from " + source);
    }

    /**
     * Inserts one legacy payment through the duplicate-safe insert, so an
     * already migrated id is ignored. The legacy table has no expiry, so
     * {@code created_at} is also used for {@code expires_at}.
     *
     * @param connection the migration connection
     * @param id the legacy transaction id
     * @param sender the paying player
     * @param receiver the receiving player
     * @param amount the legacy amount
     * @param state the mapped state
     * @param createdAt the created time in epoch milliseconds
     * @throws SQLException when the insert fails
     */
    private void insertLegacyTransaction(
            Connection connection,
            String id,
            UUID sender,
            UUID receiver,
            BigDecimal amount,
            TransactionState state,
            long createdAt)
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
     * <p>Legacy {@code PENDING} rows become {@link TransactionState#EXPIRED}
     * because their confirmation window did not survive the migration; unknown
     * or missing values become {@link TransactionState#CANCELLED}.
     *
     * @param raw the legacy state string, possibly {@code null}
     * @return the mapped state
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
     * Creates the leaderboard and payment-state indexes when they are missing.
     *
     * @param connection the migration connection
     * @throws SQLException when the metadata lookups fail
     */
    private void createIndexes(Connection connection) throws SQLException {
        createIndex(connection, config.accountTable(), "idx_" + config.accountTable() + "_balance", "balance DESC");
        createIndex(connection, config.transactionTable(), "idx_" + config.transactionTable() + "_state", "state");
    }

    /**
     * Creates one index if it does not exist yet. A failure is logged as a
     * warning instead of aborting startup, because indexes are only an
     * optimization.
     *
     * @param connection the migration connection
     * @param table the table to index
     * @param index the index name
     * @param column the column expression to index
     * @throws SQLException when the existence check fails
     */
    private void createIndex(Connection connection, String table, String index, String column) throws SQLException {
        if (indexExists(connection, table, index)) {
            return;
        }
        try {
            execute(
                    connection,
                    "CREATE INDEX " + dialect.quote(index) + " ON " + dialect.quote(table) + " (" + column + ")");
        } catch (SQLException exception) {
            logger.warning("Could not create index " + index + ": " + exception.getMessage());
        }
    }

    /**
     * Creates the schema bookkeeping table when absent.
     *
     * @param connection the migration connection
     * @throws SQLException when the table cannot be created
     */
    private void createSchemaTable(Connection connection) throws SQLException {
        execute(connection, "CREATE TABLE IF NOT EXISTS " + dialect.quote(SCHEMA_TABLE) + " (version INT NOT NULL)");
    }

    /**
     * Replaces the contents of the schema table with the current version,
     * {@code 1}.
     *
     * @param connection the migration connection
     * @throws SQLException when the delete or insert fails
     */
    private void writeSchemaVersion(Connection connection) throws SQLException {
        execute(connection, "DELETE FROM " + dialect.quote(SCHEMA_TABLE));
        execute(connection, "INSERT INTO " + dialect.quote(SCHEMA_TABLE) + " (version) VALUES (1)");
    }

    /**
     * Renames a legacy table so the current table can reuse its name while the
     * old data stays behind as a backup.
     *
     * @param connection the migration connection
     * @param from the existing table name
     * @param to the new table name
     * @throws SQLException when the rename fails
     */
    private void renameTable(Connection connection, String from, String to) throws SQLException {
        execute(connection, "ALTER TABLE " + dialect.quote(from) + " RENAME TO " + dialect.quote(to));
        logger.info("Renamed legacy table " + from + " to " + to);
    }

    /**
     * Checks whether a table already has a column; legacy tables are recognised
     * by missing {@code updated_at} or {@code expires_at}.
     *
     * @param connection the migration connection
     * @param table the table to inspect
     * @param column the column name to look for, compared case-insensitively
     * @return {@code true} when the column exists
     * @throws SQLException when the metadata lookup fails
     */
    private boolean hasColumn(Connection connection, String table, String column) throws SQLException {
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
     * Checks whether an index already exists.
     *
     * @param connection the migration connection
     * @param table the indexed table
     * @param index the index name, compared case-insensitively
     * @return {@code true} when the index exists
     * @throws SQLException when the metadata lookup fails
     */
    private boolean indexExists(Connection connection, String table, String index) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet resultSet = metadata.getIndexInfo(connection.getCatalog(), null, table, false, false)) {
            while (resultSet.next()) {
                if (index.equalsIgnoreCase(resultSet.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Collects the lower-cased names of all regular tables in the database.
     *
     * @param connection the migration connection
     * @return the table names, lower-cased for case-insensitive matching
     * @throws SQLException when the metadata lookup fails
     */
    private Set<String> tableNames(Connection connection) throws SQLException {
        Set<String> names = new HashSet<>();
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet resultSet = metadata.getTables(connection.getCatalog(), null, "%", new String[] {"TABLE"})) {
            while (resultSet.next()) {
                names.add(resultSet.getString("TABLE_NAME").toLowerCase(Locale.ROOT));
            }
        }
        return names;
    }

    /**
     * Runs a DDL statement.
     *
     * @param connection the migration connection
     * @param sql the statement to execute
     * @throws SQLException when the statement fails
     */
    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    /**
     * Deduplication holder for one account read from a legacy table.
     */
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
