package me.playernguyen.sql.sqlite;

import me.playernguyen.sql.SQLEstablish;

import java.io.File;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLiteEstablish extends SQLEstablish {


    public SQLiteEstablish(File file) {
        super();
        setUrl("jdbc:sqlite:" + file);
    }

    @Override
    public void createConnection() throws SQLException, ClassNotFoundException {
        if (getConnection() != null && !getConnection().isClosed()) return;
        synchronized (this) {
            Class.forName("org.sqlite.JDBC");
            this.setConnection(DriverManager.getConnection(getUrl()));
        }
    }

}
