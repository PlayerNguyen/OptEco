package me.playernguyen.sql;

import me.playernguyen.OptEco;
import me.playernguyen.configuration.StoreType;
import me.playernguyen.sql.mysql.MySQLAccount;
import me.playernguyen.sql.sqlite.SQLiteAccount;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class SQLEstablish {

    private String url;
    private Connection connection;
    public SQLEstablish(String url) {
        this.url = url;
    }
    public SQLEstablish () {
        this.url = "";
        this.connection = null;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public abstract void createConnection() throws SQLException, ClassNotFoundException;

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public void close() throws SQLException {
        if (getConnection() != null && !getConnection().isClosed()) {
            getConnection().close();
        }
    }

    public boolean tables (StoreType storeType, OptEco eco) throws SQLException, ClassNotFoundException {
        this.createConnection();
        switch (storeType) {
            case SQLITE: {
                return new SQLiteAccount(eco).createTable(SQLiteAccount.SETUP_TABLE_LIST);
            }
            case MYSQL: {
                return new MySQLAccount(eco).createTable(MySQLAccount.SETUP_TABLE_LIST);
            }
        }
        this.close();
        return false;
    }

}
