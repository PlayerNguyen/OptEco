package me.playernguyen.opteco.storage.jdbc.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;
import me.playernguyen.opteco.TestConfigs;
import me.playernguyen.opteco.config.OptEcoConfig;
import me.playernguyen.opteco.core.exception.StorageException;
import me.playernguyen.opteco.storage.jdbc.SqliteConnectionProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SchemaMigratorTest {

    private static final String HISTORY = "opteco_schema_history";

    @TempDir
    Path tempDir;

    @Test
    void shouldCreateSchemaAndHistoryWhenDatabaseIsFresh() throws Exception {
        // Arrange
        SqliteConnectionProvider provider = provider("fresh.db");
        OptEcoConfig config = TestConfigs.config();

        // Act
        migrate(provider, config);

        // Assert
        assertTrue(tableExists(provider, config.accountTable()));
        assertTrue(tableExists(provider, config.transactionTable()));
        assertEquals(1, count(provider, "SELECT COUNT(*) FROM " + HISTORY));
        provider.close();
    }

    @Test
    void shouldDoNothingWhenMigrateRunsTwice() throws Exception {
        // Arrange
        SqliteConnectionProvider provider = provider("twice.db");
        OptEcoConfig config = TestConfigs.config();

        // Act
        migrate(provider, config);
        migrate(provider, config);

        // Assert
        assertEquals(1, count(provider, "SELECT COUNT(*) FROM " + HISTORY));
        provider.close();
    }

    @Test
    void shouldFailWhenAppliedScriptChecksumChanged() throws Exception {
        // Arrange
        SqliteConnectionProvider provider = provider("checksum.db");
        OptEcoConfig config = TestConfigs.config();
        migrate(provider, config);
        execute(provider, "UPDATE " + HISTORY + " SET checksum = 'deadbeef'");

        // Act and Assert
        assertThrows(StorageException.class, () -> migrate(provider, config));
        provider.close();
    }

    @Test
    void shouldFailWhenAppliedScriptWasRenamed() throws Exception {
        // Arrange
        SqliteConnectionProvider provider = provider("renamed.db");
        OptEcoConfig config = TestConfigs.config();
        migrate(provider, config);
        execute(provider, "UPDATE " + HISTORY + " SET script = 'old_name.sql'");

        // Act and Assert
        assertThrows(StorageException.class, () -> migrate(provider, config));
        provider.close();
    }

    @Test
    void shouldFailWhenDatabaseVersionIsNewerThanBuild() throws Exception {
        // Arrange
        SqliteConnectionProvider provider = provider("downgrade.db");
        OptEcoConfig config = TestConfigs.config();
        migrate(provider, config);
        execute(
                provider,
                "INSERT INTO " + HISTORY
                        + " (version, script, checksum, applied_at, execution_ms) VALUES (99, 'future.sql', NULL, 0, 0)");

        // Act and Assert
        assertThrows(StorageException.class, () -> migrate(provider, config));
        provider.close();
    }

    @Test
    void shouldAdoptLegacyMarkerWhenHistoryIsEmpty() throws Exception {
        // Arrange
        SqliteConnectionProvider provider = provider("adopted.db");
        OptEcoConfig config = TestConfigs.config();
        migrate(provider, config);
        execute(
                provider,
                "INSERT INTO " + config.accountTable()
                        + " (uuid, player, balance, updated_at) VALUES ('"
                        + java.util.UUID.randomUUID() + "', 'Alice', 500000000, 1)");
        execute(provider, "DELETE FROM " + HISTORY);
        execute(provider, "CREATE TABLE opteco_schema (version INT NOT NULL)");
        execute(provider, "INSERT INTO opteco_schema (version) VALUES (1)");

        // Act
        migrate(provider, config);

        // Assert
        assertEquals(1, count(provider, "SELECT COUNT(*) FROM " + HISTORY));
        assertEquals(1, count(provider, "SELECT COUNT(*) FROM " + HISTORY + " WHERE checksum IS NULL"));
        assertEquals(1, count(provider, "SELECT COUNT(*) FROM " + config.accountTable()));
        provider.close();
    }

    @Test
    void shouldCreateTablesWithCustomNamesWhenPlaceholdersAreConfigured() throws Exception {
        // Arrange
        SqliteConnectionProvider provider = provider("custom.db");
        OptEcoConfig config = OptEcoConfig.builder()
                .useDouble(true)
                .startBalance(BigDecimal.valueOf(100))
                .minBalance(BigDecimal.valueOf(-10))
                .paymentDurationSeconds(15)
                .accountTable("custom_accounts")
                .transactionTable("custom_transactions")
                .build();
        config.validate();

        // Act
        migrate(provider, config);

        // Assert
        assertTrue(tableExists(provider, "custom_accounts"));
        assertTrue(tableExists(provider, "custom_transactions"));
        assertTrue(indexExists(provider, "custom_accounts", "idx_custom_accounts_balance"));
        provider.close();
    }

    @Test
    void shouldRollBackWhenScriptFails() throws Exception {
        // Arrange
        SqliteConnectionProvider provider = provider("rollback.db");
        OptEcoConfig config = TestConfigs.config();
        MigrationScriptLoader loader =
                new MigrationScriptLoader("migration-test/sqlite", getClass().getClassLoader(), null);

        // Act and Assert
        assertThrows(
                StorageException.class,
                () -> new SchemaMigrator(provider, config, Logger.getLogger("test"), loader).migrate());
        assertFalse(tableExists(provider, "rollback_probe"));
        assertEquals(0, count(provider, "SELECT COUNT(*) FROM " + HISTORY));
        provider.close();
    }

    @Test
    void shouldApplyOnlyNewScriptsWhenSecondVersionIsAdded() throws Exception {
        // Arrange
        SqliteConnectionProvider provider = provider("upgrade.db");
        OptEcoConfig config = TestConfigs.config();
        Path root = tempDir.resolve("scripts");
        Path folder = root.resolve("migration/sqlite");
        Files.createDirectories(folder);
        write(folder.resolve("0001_probe.sql"), "CREATE TABLE upgrade_probe (id INTEGER PRIMARY KEY);");
        migrateWithLoader(provider, config, loader(root));
        write(folder.resolve("0002_extra.sql"), "ALTER TABLE upgrade_probe ADD COLUMN extra INT;");

        // Act
        migrateWithLoader(provider, config, loader(root));

        // Assert
        assertEquals(2, count(provider, "SELECT COUNT(*) FROM " + HISTORY));
        assertTrue(columnExists(provider, "upgrade_probe", "extra"));
        provider.close();
    }

    private SqliteConnectionProvider provider(String name) {
        return new SqliteConnectionProvider(tempDir.resolve(name).toFile());
    }

    private void migrate(SqliteConnectionProvider provider, OptEcoConfig config) {
        new SchemaMigrator(provider, config, Logger.getLogger("test")).migrate();
    }

    private void migrateWithLoader(
            SqliteConnectionProvider provider, OptEcoConfig config, MigrationScriptLoader loader) {
        new SchemaMigrator(provider, config, Logger.getLogger("test"), loader).migrate();
    }

    private MigrationScriptLoader loader(Path root) throws IOException {
        URL url = root.toUri().toURL();
        return new MigrationScriptLoader("migration/sqlite", new URLClassLoader(new URL[] {url}, null), url);
    }

    private void write(Path file, String content) throws IOException {
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }

    private void execute(SqliteConnectionProvider provider, String sql) throws SQLException {
        try (Connection connection = provider.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private int count(SqliteConnectionProvider provider, String sql) throws SQLException {
        try (Connection connection = provider.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    private boolean tableExists(SqliteConnectionProvider provider, String table) throws SQLException {
        try (Connection connection = provider.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet resultSet = metadata.getTables(connection.getCatalog(), null, "%", new String[] {"TABLE"})) {
                while (resultSet.next()) {
                    if (table.equalsIgnoreCase(resultSet.getString("TABLE_NAME"))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean columnExists(SqliteConnectionProvider provider, String table, String column) throws SQLException {
        try (Connection connection = provider.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet resultSet = metadata.getColumns(connection.getCatalog(), null, table, null)) {
                while (resultSet.next()) {
                    if (column.equalsIgnoreCase(resultSet.getString("COLUMN_NAME"))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean indexExists(SqliteConnectionProvider provider, String table, String index) throws SQLException {
        try (Connection connection = provider.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet resultSet = metadata.getIndexInfo(connection.getCatalog(), null, table, false, false)) {
                while (resultSet.next()) {
                    if (index.equalsIgnoreCase(resultSet.getString("INDEX_NAME"))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
