package me.playernguyen.opteco.account.mysql;

import me.playernguyen.opteco.OptEco;
import me.playernguyen.opteco.OptEcoConfiguration;
import me.playernguyen.opteco.account.SQLAccountDatabase;
import me.playernguyen.opteco.sql.MySQLEstablish;

import java.util.ArrayList;
import java.util.Arrays;

public class MySQLAccountDatabase extends SQLAccountDatabase {

    public static final String ACCOUNT_TABLE = OptEco.getInstance()
            .getConfigurationLoader().getString(OptEcoConfiguration.SQL_ACCOUNT_TABLE_NAME);

    public static final ArrayList<String> SETUP_TABLE_LIST =
            new ArrayList<>(
                    Arrays.asList("`id` INT(32) NOT NULL AUTO_INCREMENT PRIMARY KEY",
                            "`player` VARCHAR(255) NOT NULL",
                            "`balance` REAL NOT NULL",
                            "`uuid` VARCHAR(255) NOT NULL"
                    )
            );

    public MySQLAccountDatabase() {
        super(new MySQLEstablish(ACCOUNT_TABLE, SETUP_TABLE_LIST));
    }

}
