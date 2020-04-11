package me.playernguyen.opteco.account.sqlite;

import me.playernguyen.opteco.OptEco;
import me.playernguyen.opteco.OptEcoConfiguration;
import me.playernguyen.opteco.sql.SQLAccountManager;
import me.playernguyen.opteco.sql.SQLiteEstablish;

import java.util.ArrayList;
import java.util.Arrays;

public class SQLiteAccountManager extends SQLAccountManager {

    public static final String ACCOUNT_TABLE = OptEco.getInstance()
            .getConfigurationLoader().getString(OptEcoConfiguration.SQL_ACCOUNT_TABLE_NAME);

    public static final ArrayList<String> SETUP_TABLE_LIST =
            new ArrayList<>(
                    Arrays.asList("id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT",
                            "player CHAR(255) NOT NULL",
                            "balance REAL NOT NULL",
                            "uuid CHAR(255) NOT NULL"
                    )
            );

    public SQLiteAccountManager() {
        super(new SQLiteEstablish(ACCOUNT_TABLE, SETUP_TABLE_LIST));
    }
}
