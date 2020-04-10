package me.playernguyen.opteco.sql;

import me.playernguyen.opteco.OptEcoConfiguration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

public class SQLiteEstablish extends SQLEstablish {


    public SQLiteEstablish (String tableName, ArrayList<String> init) {
        super(tableName, init);
    }

    @Override public Connection openConnect() throws SQLException, ClassNotFoundException {
        setURL("jdbc:sqlite:" + getConfigurationLoader().getString(OptEcoConfiguration.SQLITE_FILE));
        // If null
        if (getURL() == null) {
            throw new NullPointerException("The url mustn't be null");
        }
        // Import class
        Class.forName("org.sqlite.JDBC");
        // Open connect
        return DriverManager.getConnection(getURL());
    }
}
