package me.playernguyen.opteco.storage.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import me.playernguyen.opteco.config.OptEcoConfig;
import me.playernguyen.opteco.core.exception.StorageException;

public final class MysqlConnectionProvider implements ConnectionProvider {

    private final HikariDataSource dataSource;

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

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public SqlDialect dialect() {
        return SqlDialect.MYSQL;
    }

    @Override
    public String description() {
        return "MySQL";
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
