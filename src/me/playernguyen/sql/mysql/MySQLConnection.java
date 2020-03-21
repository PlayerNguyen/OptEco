package me.playernguyen.sql.mysql;

import me.playernguyen.sql.SQLConnection;

import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLConnection extends SQLConnection {


    public MySQLConnection (String address, String port, String database, String username, String password)
            throws SQLException {
        super(DriverManager.getConnection(String.format("jdbc:mysql://%s:%s/%s", address, port, database), username, password));
    }


}
