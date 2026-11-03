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
 */
public final class SchemaMigrator {

    private static final String SCHEMA_TABLE = "opteco_schema";
    private static final String LEGACY_ACCOUNT_TABLE = "opteco";
    private static final String LEGACY_TRANSACTION_TABLE = "opteco_transaction";

    private final ConnectionProvider provider;
    private final OptEcoConfig config;
    private final SqlDialect dialect;
    private final Logger logger;

    public SchemaMigrator(ConnectionProvider provider, OptEcoConfig config, Logger logger) {
        this.provider = provider;
        this.config = config;
        this.dialect = provider.dialect();
        this.logger = logger;
    }

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

    private String resolveSource(Set<String> tables, String configured, String legacy) {
        if (tables.contains(configured.toLowerCase(Locale.ROOT))) {
            return configured;
        }
        if (tables.contains(legacy)) {
            return legacy;
        }
        return null;
    }

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

    private int countRows(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + dialect.quote(table))) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    private void createAccounts(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + dialect.quote(config.accountTable()) + " ("
                + "uuid VARCHAR(36) NOT NULL PRIMARY KEY, "
                + "player VARCHAR(64) NOT NULL, "
                + "balance BIGINT NOT NULL, "
                + "updated_at BIGINT NOT NULL)";
        execute(connection, sql);
    }

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

    private BigDecimal legacyAmount(double raw) {
        return BigDecimal.valueOf(raw).setScale(Money.SCALE, RoundingMode.HALF_UP);
    }

    private void createIndexes(Connection connection) throws SQLException {
        createIndex(connection, config.accountTable(), "idx_" + config.accountTable() + "_balance", "balance DESC");
        createIndex(connection, config.transactionTable(), "idx_" + config.transactionTable() + "_state", "state");
    }

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

    private void createSchemaTable(Connection connection) throws SQLException {
        execute(connection, "CREATE TABLE IF NOT EXISTS " + dialect.quote(SCHEMA_TABLE) + " (version INT NOT NULL)");
    }

    private void writeSchemaVersion(Connection connection) throws SQLException {
        execute(connection, "DELETE FROM " + dialect.quote(SCHEMA_TABLE));
        execute(connection, "INSERT INTO " + dialect.quote(SCHEMA_TABLE) + " (version) VALUES (1)");
    }

    private void renameTable(Connection connection, String from, String to) throws SQLException {
        execute(connection, "ALTER TABLE " + dialect.quote(from) + " RENAME TO " + dialect.quote(to));
        logger.info("Renamed legacy table " + from + " to " + to);
    }

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

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static final class LegacyAccount {
        private final String name;
        private final BigDecimal balance;

        private LegacyAccount(String name, BigDecimal balance) {
            this.name = name;
            this.balance = balance;
        }
    }
}
