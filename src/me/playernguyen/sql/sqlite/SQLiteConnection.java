package me.playernguyen.sql.sqlite;

import me.playernguyen.sql.SQLConnection;

import java.io.File;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLiteConnection extends SQLConnection {

    public SQLiteConnection(File file) throws SQLException {
        super(DriverManager.getConnection("jdbc:sqlite:" + file));
    }

}
