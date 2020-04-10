package me.playernguyen.opteco;

import me.playernguyen.opteco.configuration.StorageType;

public enum OptEcoConfiguration {

    CHECK_FOR_UPDATE                ("checkForUpdate", true),
    START_BALANCE                   ("settings.startBalance", 0.0),
    MIN_BALANCE                     ("settings.minimumBalance", -15.0),
    PAYMENT_CONFIRM                 ("settings.timeToConfirmPayment", 15),
    CURRENCY_SYMBOL                 ("settings.currencySymbol", "points"),
    STORAGE_TYPE                    ("settings.storeType", StorageType.SQLITE.toString()),

    MYSQL_HOST                      ("settings.mysql.host", "localhost"),
    MYSQL_DATABASE                  ("settings.mysql.database", "dbname"),
    MYSQL_PORT                      ("settings.mysql.port", "3306"),
    MYSQL_USERNAME                  ("settings.mysql.username", "root"),
    MYSQL_PASSWORD                  ("settings.mysql.password", ""),

    SQLITE_FILE                     ("settings.sqlite.fileName", "account.sqlite"),

    SQL_ACCOUNT_TABLE_NAME          ("settings.sql.accountTable", "opteco"),
    SQL_TRANSACT_TABLE_NAME         ("settings.sql.transactTable", "opteco_transaction"),

    SQL_CLOSE_CONNECT_TIMEOUT       ("settings.sql.connectionPool.timeout", 5),

    COUNTDOWN_ENABLE                ("settings.countdown.enable", true),
    COUNTDOWN_TYPE                  ("settings.countdown.type", "message"),

    DEBUG                           ("debug", false),
    LANGUAGE_FILE                   ("language", "lang");

    private String path;
    private Object wh;

    OptEcoConfiguration(String path, Object wh) {
        this.path = path;
        this.wh = wh;
    }

    public String getPath() {
        return path;
    }

    public Object getDefaultSetting() {
        return wh;
    }
}
