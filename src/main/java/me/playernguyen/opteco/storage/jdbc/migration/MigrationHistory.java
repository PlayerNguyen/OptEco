package me.playernguyen.opteco.storage.jdbc.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import me.playernguyen.opteco.core.exception.StorageException;
import me.playernguyen.opteco.storage.jdbc.SqlDialect;

/**
 * Reads and writes the migration bookkeeping table.
 *
 * <p>{@code opteco_schema_history} holds one row per applied script. The table
 * itself is created by code because it must exist before the first script can
 * be recorded. The legacy {@code opteco_schema(version)} table is only read,
 * once, to adopt databases produced by the previous Java migrator; its rows get
 * a {@code null} checksum so they are exempt from comparison.</p>
 *
 * <p>All methods are blocking and expect a connection owned by the caller. The
 * class is not thread-safe; {@link SchemaMigrator} uses one instance per run.</p>
 */
public final class MigrationHistory {

    /** History table, deliberately not configurable. */
    static final String TABLE = "opteco_schema_history";

    /** Version marker written by the pre-resource migrator. */
    static final String LEGACY_TABLE = "opteco_schema";

    /** DDL executed before any read. */
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS %s ("
            + "version INT NOT NULL PRIMARY KEY, "
            + "script VARCHAR(255) NOT NULL, "
            + "checksum VARCHAR(64) NULL, "
            + "applied_at BIGINT NOT NULL, "
            + "execution_ms BIGINT NOT NULL)";

    /** Connection used for every statement. */
    private final Connection connection;

    /** Dialect used to quote the fixed table name. */
    private final SqlDialect dialect;

    /**
     * Creates a history reader for one migration run.
     *
     * @param connection the migration connection
     * @param dialect the backend dialect
     */
    public MigrationHistory(Connection connection, SqlDialect dialect) {
        this.connection = connection;
        this.dialect = dialect;
    }

    /**
     * Creates the history table when it does not exist yet.
     *
     * @throws SQLException when the DDL fails
     */
    public void ensureTable() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(String.format(CREATE_TABLE, dialect.quote(TABLE)));
        }
    }

    /**
     * Reports whether any version has been recorded.
     *
     * @return {@code true} when the history table is empty
     * @throws SQLException when the count fails
     */
    public boolean isEmpty() throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + dialect.quote(TABLE))) {
            return !resultSet.next() || resultSet.getInt(1) == 0;
        }
    }

    /**
     * Loads every applied version, ordered by version.
     *
     * @return applied rows keyed by version, in ascending order
     * @throws SQLException when the query fails
     */
    public Map<Integer, AppliedMigration> loadApplied() throws SQLException {
        Map<Integer, AppliedMigration> applied = new LinkedHashMap<>();
        String sql = "SELECT version, script, checksum FROM " + dialect.quote(TABLE) + " ORDER BY version";
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                int version = resultSet.getInt("version");
                applied.put(
                        version,
                        new AppliedMigration(version, resultSet.getString("script"), resultSet.getString("checksum")));
            }
        }
        return applied;
    }

    /**
     * Records one successfully applied script.
     *
     * @param script the applied migration
     * @param executionMs wall-clock duration used for the history row
     * @throws SQLException when the insert fails
     */
    public void record(MigrationScript script, long executionMs) throws SQLException {
        String sql = "INSERT INTO " + dialect.quote(TABLE)
                + " (version, script, checksum, applied_at, execution_ms) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, script.version());
            statement.setString(2, script.fileName());
            statement.setString(3, script.checksum());
            statement.setLong(4, System.currentTimeMillis());
            statement.setLong(5, executionMs);
            statement.executeUpdate();
        }
    }

    /**
     * Adopts the version marker written by the previous Java migrator.
     *
     * <p>Only runs when the history table is empty and {@code opteco_schema}
     * exists with a positive version. Every script up to that marker is inserted
     * with a {@code null} checksum, because the SQL text that produced that
     * schema no longer exists in file form.</p>
     *
     * @param scripts the discovered scripts, contiguous from version 1
     * @return the number of adopted versions, or zero when nothing was adopted
     * @throws SQLException when the marker cannot be read or a row cannot be written
     * @throws StorageException when the database is newer than this build
     */
    public int adoptLegacy(List<MigrationScript> scripts) throws SQLException {
        if (!isEmpty() || !tableExists(LEGACY_TABLE)) {
            return 0;
        }
        int legacy = maxVersion(LEGACY_TABLE);
        if (legacy <= 0) {
            return 0;
        }
        int latest = scripts.get(scripts.size() - 1).version();
        if (legacy > latest) {
            throw new StorageException("OptEco database schema is at version " + legacy + " but this build only has "
                    + latest + "; upgrade the plugin");
        }
        String sql = "INSERT INTO " + dialect.quote(TABLE)
                + " (version, script, checksum, applied_at, execution_ms) VALUES (?, ?, ?, ?, ?)";
        long now = System.currentTimeMillis();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int version = 1; version <= legacy; version++) {
                statement.setInt(1, version);
                statement.setString(2, scripts.get(version - 1).fileName());
                statement.setNull(3, Types.VARCHAR);
                statement.setLong(4, now);
                statement.setLong(5, 0L);
                statement.addBatch();
            }
            statement.executeBatch();
        }
        return legacy;
    }

    /**
     * Reads the highest version stored in one table.
     *
     * @param table the table to read, already a trusted name
     * @return the maximum version, or zero when the table has no rows
     * @throws SQLException when the query fails
     */
    private int maxVersion(String table) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT MAX(version) FROM " + dialect.quote(table))) {
            if (resultSet.next()) {
                int value = resultSet.getInt(1);
                return resultSet.wasNull() ? 0 : value;
            }
        }
        return 0;
    }

    /**
     * Checks whether a table exists, comparing names case-insensitively.
     *
     * @param table the table name to look for
     * @return {@code true} when the table is present
     * @throws SQLException when the metadata lookup fails
     */
    boolean tableExists(String table) throws SQLException {
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
}
