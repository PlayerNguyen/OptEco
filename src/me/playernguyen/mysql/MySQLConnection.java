package me.playernguyen.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQLConnection {

    private Connection connection;
    private Statement statement;

    public MySQLConnection (String address, String port, String database, String username, String password)
            throws SQLException {
        this.connection = DriverManager.getConnection(
                buildURL(address, port, database),
                username, password
        );
        this.statement = getConnection().createStatement();
    }

    public Connection getConnection() {
        return connection;
    }

    public Statement getStatement() {
        return statement;
    }

    public  String buildURL(String address, String port, String database) {
        return String.format("jdbc:mysql://%s:%s/%s", address, port, database);
    }

}
