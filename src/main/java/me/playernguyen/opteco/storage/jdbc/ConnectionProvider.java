package me.playernguyen.opteco.storage.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionProvider extends AutoCloseable {

    Connection getConnection() throws SQLException;

    SqlDialect dialect();

    String description();

    @Override
    void close();
}
