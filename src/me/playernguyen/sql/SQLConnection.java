package me.playernguyen.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class SQLConnection{

    private String url;
    private Connection connection;
    private Statement statement;
    public SQLConnection(String url, Connection connection) {
        this.url = url;
        this.connection = connection;

        try {
            this.statement = connection.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public SQLConnection(Connection connection) {
        this.url = "";
        this.connection = connection;

        try {
            this.statement = connection.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getUrl() {
        return url;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Statement getStatement() {
        return statement;
    }
}
