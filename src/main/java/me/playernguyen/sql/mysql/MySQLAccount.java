package me.playernguyen.sql.mysql;

import me.playernguyen.OptEco;
import me.playernguyen.OptEcoConfiguration;
import me.playernguyen.sql.SQLAccount;

import java.util.ArrayList;
import java.util.Arrays;

public class MySQLAccount extends SQLAccount {

    public static final ArrayList<String> SETUP_TABLE_LIST =
            new ArrayList<>(
                    Arrays.asList("`id` INT(32) NOT NULL AUTO_INCREMENT PRIMARY KEY",
                            "`player` VARCHAR(255) NOT NULL",
                            "`balance` REAL NOT NULL",
                            "`uuid` VARCHAR(255) NOT NULL"
                    )
            );

    public MySQLAccount(OptEco plugin) {
        super(
                plugin,
                plugin.getSQLEstablish(),
                plugin.getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_TABLE_NAME)
        );
    }

}
