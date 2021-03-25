package me.playernguyen.opteco.transaction.sqlite;

import me.playernguyen.opteco.OptEco;
import me.playernguyen.opteco.configuration.OptEcoConfiguration;
import me.playernguyen.opteco.sql.SQLiteEstablish;
import me.playernguyen.opteco.transaction.TransactionSQL;

import java.util.ArrayList;
import java.util.Arrays;

public class TransactionSQLiteStorage extends TransactionSQL {

    public static final String TABLE =
            OptEco.getInstance().getConfigurationLoader().getString(OptEcoConfiguration.SQL_TRANSACT_TABLE_NAME);

    public static final ArrayList<String> TRANSACTION_INIT =
            new ArrayList<>(
                    Arrays.asList(
                            "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT",
                            "transaction_id VARCHAR NOT NULL",
                            "sender VARCHAR NOT NULL",
                            "receiver VARCHAR NOT NULL",
                            "amount REAL NOT NULL",
                            "state VARCHAR NOT NULL",
                            "time VARCHAR NOT NULL"
                    )
            );

    public TransactionSQLiteStorage() {
        super(new SQLiteEstablish(TABLE, TRANSACTION_INIT));
    }


}
