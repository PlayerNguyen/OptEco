package me.playernguyen.sql.sqlite;

import me.playernguyen.OptEco;
import me.playernguyen.sql.SQLAccount;

import java.util.ArrayList;
import java.util.Arrays;

public class SQLiteAccount extends SQLAccount {

    public static final ArrayList<String> SETUP_TABLE_LIST =
            new ArrayList<>(
                    Arrays.asList("`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT",
                            "`player` CHAR(255) NOT NULL",
                            "`balance` REAL NOT NULL",
                            "`uuid` CHAR(255) NOT NULL"
                    )
            );

    public SQLiteAccount(OptEco plugin) {
        super(plugin, plugin.getSQLEstablish(), "opteco_points");
    }
}
