package me.playernguyen.opteco.storage.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import me.playernguyen.opteco.config.OptEcoConfig;
import me.playernguyen.opteco.core.exception.StorageException;

/**
 * MySQL {@link ConnectionProvider} backed by a HikariCP pool.
 *
 * <p>The constructor reads the host, port, database, credentials and optional
 * JDBC parameters from the startup {@link OptEcoConfig} snapshot, then builds a
 * pool with at most five connections, one idle connection and a five second
 * connection timeout. Closing a pooled connection returns it to the pool rather
 * than closing the socket. Transaction boundaries remain with
 * {@link JdbcUnitOfWork}. The pool is created once during enable, so changing
 * MySQL settings requires a restart. Calls are blocking and run on the OptEco
 * database threads.
 */
public final class MysqlConnectionProvider implements ConnectionProvider {

    /** HikariCP pool that owns every MySQL connection. */
    private final HikariDataSource dataSource;

    /**
     * Loads the bundled MySQL driver and builds the connection pool from the
     * supplied configuration snapshot.
     *
     * @param config the startup configuration holding the MySQL settings
     * @throws StorageException when the shaded MySQL driver is missing from the
     *         plugin jar
     */
    public MysqlConnectionProvider(OptEcoConfig config) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new StorageException("MySQL JDBC driver is missing from the OptEco jar", exception);
        }
        String parameters = config.mysqlParameters();
        String url = "jdbc:mysql://" + config.mysqlHost() + ':' + config.mysqlPort() + '/' + config.mysqlDatabase();
        if (!parameters.isEmpty()) {
            url += '?' + parameters;
        }

        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(url);
        hikari.setUsername(config.mysqlUsername());
        hikari.setPassword(config.mysqlPassword());
        hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikari.setMaximumPoolSize(5);
        hikari.setMinimumIdle(1);
        hikari.setConnectionTimeout(5000L);
        hikari.setValidationTimeout(3000L);
        hikari.setPoolName("OptEco-MySQL");
        this.dataSource = new HikariDataSource(hikari);
    }

    /**
     * Borrows a connection from the pool.
     *
     * @return a pooled connection that returns to the pool when closed
     * @throws SQLException when no connection becomes available within the pool
     *         timeout
     */
    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Returns the MySQL dialect.
     *
     * @return always {@link SqlDialect#MYSQL}
     */
    @Override
    public SqlDialect dialect() {
        return SqlDialect.MYSQL;
    }

    /**
     * Describes the backend for log output.
     *
     * @return the constant label {@code MySQL}
     */
    @Override
    public String description() {
        return "MySQL";
    }

    /**
     * Closes the pool and its idle connections. Connections still checked out
     * are closed when their owners return them.
     */
    @Override
    public void close() {
        dataSource.close();
    }
}
