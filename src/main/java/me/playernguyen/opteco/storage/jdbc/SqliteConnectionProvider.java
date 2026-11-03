package me.playernguyen.opteco.storage.jdbc;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import me.playernguyen.opteco.core.exception.StorageException;

public final class SqliteConnectionProvider implements ConnectionProvider {

    private final File file;

    public SqliteConnectionProvider(File file) {
        this.file = file;
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException exception) {
            throw new StorageException("SQLite JDBC driver is missing from the OptEco jar", exception);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        configure(connection);
        return connection;
    }

    private void configure(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout = 5000");
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA synchronous = NORMAL");
            statement.execute("PRAGMA foreign_keys = ON");
        }
    }

    @Override
    public SqlDialect dialect() {
        return SqlDialect.SQLITE;
    }

    @Override
    public String description() {
        return "SQLite (" + file.getName() + ")";
    }

    @Override
    public void close() {
        // DriverManager connections are closed by their callers.
    }
}
