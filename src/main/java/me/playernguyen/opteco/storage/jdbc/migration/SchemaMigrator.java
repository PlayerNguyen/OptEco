package me.playernguyen.opteco.storage.jdbc.migration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import me.playernguyen.opteco.config.OptEcoConfig;
import me.playernguyen.opteco.core.exception.StorageException;
import me.playernguyen.opteco.storage.jdbc.ConnectionProvider;
import me.playernguyen.opteco.storage.jdbc.SqlDialect;

/**
 * Applies the resource-backed schema migrations exactly once, in order.
 *
 * <p>Scripts are discovered by {@link MigrationScriptLoader}, compared with the
 * rows in {@link MigrationHistory}, and the pending ones are executed inside one
 * transaction on SQLite. MySQL keeps auto-commit because its DDL implicitly
 * commits; its scripts are written idempotently instead. The legacy
 * account/transaction import runs after the schema through
 * {@link LegacyImporter}.</p>
 *
 * <p>Runs once during plugin startup, before any repository is used. Calls are
 * blocking and the instance is not thread-safe.</p>
 */
public final class SchemaMigrator {

    /** Connection source for the migration run. */
    private final ConnectionProvider provider;

    /** Startup configuration snapshot holding the table names. */
    private final OptEcoConfig config;

    /** Plugin logger that reports applied scripts and imported data. */
    private final Logger logger;

    /** Dialect of the backing database. */
    private final SqlDialect dialect;

    /** Loader that supplies the validated script list. */
    private final MigrationScriptLoader loader;

    /**
     * Creates a migrator for one backend and configuration snapshot.
     *
     * @param provider the connection source and dialect owner
     * @param config the startup configuration holding the table names
     * @param logger the plugin logger used for migration progress
     */
    public SchemaMigrator(ConnectionProvider provider, OptEcoConfig config, Logger logger) {
        this(provider, config, logger, MigrationScriptLoader.forDialect(provider.dialect()));
    }

    /**
     * Creates a migrator with an explicit script loader, used by tests.
     *
     * @param provider the connection source and dialect owner
     * @param config the startup configuration holding the table names
     * @param logger the plugin logger used for migration progress
     * @param loader the loader that supplies the validated scripts
     */
    SchemaMigrator(ConnectionProvider provider, OptEcoConfig config, Logger logger, MigrationScriptLoader loader) {
        this.provider = provider;
        this.config = config;
        this.logger = logger;
        this.dialect = provider.dialect();
        this.loader = loader;
    }

    /**
     * Brings the database to the latest schema version and imports legacy data.
     *
     * @throws StorageException when a script is malformed, the database is newer
     *         than this build, or a statement fails
     */
    public void migrate() {
        List<MigrationScript> scripts = loader.load();
        Map<String, String> placeholders = SqlPlaceholders.forConfig(config);
        try (Connection connection = provider.getConnection()) {
            MigrationHistory history = new MigrationHistory(connection, dialect);
            history.ensureTable();
            int adopted = history.adoptLegacy(scripts);
            if (adopted > 0) {
                logger.fine("Adopted legacy schema version " + adopted);
            }

            Map<Integer, AppliedMigration> applied = history.loadApplied();
            validateApplied(applied, scripts);

            List<MigrationScript> pending = pending(scripts, applied);
            if (pending.isEmpty()) {
                logger.fine("Schema up to date at version " + latest(scripts));
            } else {
                runScripts(connection, history, pending, placeholders);
            }
            runLegacyImport(connection);
        } catch (SQLException exception) {
            throw new StorageException("Could not migrate the " + provider.description() + " schema", exception);
        }
    }

    /**
     * Verifies that recorded history still matches the bundled scripts.
     *
     * @param applied recorded rows keyed by version
     * @param scripts the discovered scripts, contiguous from version 1
     * @throws StorageException when a version is unknown, a file was renamed, a checksum changed, or history has a gap
     */
    private void validateApplied(Map<Integer, AppliedMigration> applied, List<MigrationScript> scripts) {
        int latestVersion = latest(scripts);
        Map<Integer, MigrationScript> byVersion = new HashMap<>();
        for (MigrationScript script : scripts) {
            byVersion.put(script.version(), script);
        }
        for (AppliedMigration row : applied.values()) {
            MigrationScript script = byVersion.get(row.version());
            if (script == null) {
                throw new StorageException("Database has schema version " + row.version() + " but this build only has "
                        + latestVersion + "; upgrade the plugin");
            }
            if (!script.fileName().equals(row.script())) {
                throw new StorageException("Migration v" + row.version() + " was applied as " + row.script()
                        + " but is now named " + script.fileName());
            }
            if (row.checksum() != null && !row.checksum().equals(script.checksum())) {
                throw new StorageException("Migration " + script.fileName() + " changed after it was applied (stored "
                        + row.checksum() + ", current " + script.checksum() + ")");
            }
        }
        if (!applied.isEmpty()) {
            int highest = 0;
            for (Integer version : applied.keySet()) {
                highest = Math.max(highest, version);
            }
            if (applied.size() != highest) {
                throw new StorageException("Migration history has a gap: " + applied.size()
                        + " applied version(s) but the highest is " + highest + "; repair the history table");
            }
        }
    }

    /**
     * Returns the scripts that are not recorded yet, preserving version order.
     *
     * @param scripts the discovered scripts
     * @param applied recorded rows keyed by version
     * @return the pending scripts
     */
    private List<MigrationScript> pending(List<MigrationScript> scripts, Map<Integer, AppliedMigration> applied) {
        List<MigrationScript> pending = new ArrayList<>();
        for (MigrationScript script : scripts) {
            if (!applied.containsKey(script.version())) {
                pending.add(script);
            }
        }
        return pending;
    }

    /**
     * Executes the pending scripts and records each one.
     *
     * <p>On SQLite the rename preparation, every script and the history rows are
     * one transaction. On MySQL the statements auto-commit and the history row is
     * written afterwards, which is why scripts must be idempotent.</p>
     *
     * @param connection the migration connection
     * @param history the history store for the same connection
     * @param pending the scripts to apply
     * @param placeholders expanded placeholder values
     * @throws SQLException when preparation, a statement or a history write fails
     */
    private void runScripts(
            Connection connection,
            MigrationHistory history,
            List<MigrationScript> pending,
            Map<String, String> placeholders)
            throws SQLException {
        boolean transactional = dialect.supportsTransactionalDdl();
        boolean previousAutoCommit = connection.getAutoCommit();
        if (transactional) {
            connection.setAutoCommit(false);
        }
        try {
            new LegacyImporter(connection, config, dialect, logger).prepare();
            for (MigrationScript script : pending) {
                long started = System.nanoTime();
                List<String> statements = script.statements(placeholders);
                for (int index = 0; index < statements.size(); index++) {
                    execute(connection, script, statements.get(index), index, statements.size());
                }
                long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
                history.record(script, elapsedMs);
                logger.info("Applied migration " + script.resourcePath() + " (" + elapsedMs + " ms)");
            }
            if (transactional) {
                connection.commit();
            }
        } catch (SQLException | RuntimeException exception) {
            rollbackQuietly(connection, transactional);
            throw exception;
        } finally {
            if (transactional) {
                restoreAutoCommit(connection, previousAutoCommit);
            }
        }
    }

    /**
     * Runs one statement and annotates failures with the script and position.
     *
     * @param connection the migration connection
     * @param script the script being applied
     * @param sql the statement text
     * @param index zero-based statement index
     * @param total number of statements in the script
     * @throws SQLException when the statement fails
     */
    private void execute(Connection connection, MigrationScript script, String sql, int index, int total)
            throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException failure) {
            throw new SQLException(
                    script.resourcePath() + " failed at statement " + (index + 1) + "/" + total,
                    failure.getSQLState(),
                    failure.getErrorCode(),
                    failure);
        }
    }

    /**
     * Imports legacy rows in their own transaction after the schema is in place.
     *
     * @param connection the migration connection
     * @throws SQLException when the import fails
     */
    private void runLegacyImport(Connection connection) throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            new LegacyImporter(connection, config, dialect, logger).importLegacy();
            connection.commit();
        } catch (SQLException exception) {
            rollbackQuietly(connection, true);
            throw exception;
        } catch (RuntimeException exception) {
            rollbackQuietly(connection, true);
            throw exception;
        } finally {
            restoreAutoCommit(connection, previousAutoCommit);
        }
    }

    /**
     * Rolls back when a transaction is active, preserving the original failure.
     *
     * @param connection the connection to roll back
     * @param transactional whether this run opened a transaction
     */
    private void rollbackQuietly(Connection connection, boolean transactional) {
        if (!transactional) {
            return;
        }
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // Preserve the original failure.
        }
    }

    /**
     * Restores the caller's auto-commit setting without masking a failure.
     *
     * @param connection the connection to restore
     * @param autoCommit the value to restore
     */
    private void restoreAutoCommit(Connection connection, boolean autoCommit) {
        if (!autoCommit) {
            return;
        }
        try {
            connection.setAutoCommit(true);
        } catch (SQLException ignored) {
            // The connection is about to be returned to the pool.
        }
    }

    /**
     * Returns the highest version in a validated, non-empty script list.
     *
     * @param scripts the discovered scripts
     * @return the latest version
     */
    private int latest(List<MigrationScript> scripts) {
        return scripts.get(scripts.size() - 1).version();
    }
}
