package me.playernguyen.opteco;

import me.playernguyen.opteco.configuration.StorageType;

public enum OptEcoConfiguration {

    CHECK_FOR_UPDATE("Update.CheckForUpdate", true),
    START_BALANCE("Settings.Preferences.BeginPoint", 0.0),
    MIN_BALANCE("Settings.Preferences.MinimumReachPoint", -15.0),
    PAYMENT_CONFIRM("Settings.Payment.Pending.Duration", 15),
    CURRENCY_SYMBOL("Settings.CurrencySymbol", "ps"),
    MINIMUM_TRANSACT_VALUE("Settings.Preferences.MinimumCanTransact", 0.1),
    STORAGE_TYPE("Settings.Storage.Type", StorageType.SQLITE.toString().toLowerCase()),

    USE_DOUBLE("Settings.Preferences.UseDouble", false),

    MYSQL_HOST("Settings.SQL.MySQL.Host", "localhost"),
    MYSQL_DATABASE("Settings.SQL.MySQL.Database", "dbname"),
    MYSQL_PORT("Settings.SQL.MySQL.Port", "3306"),
    MYSQL_USERNAME("Settings.SQL.MySQL.Username", "root"),
    MYSQL_PASSWORD("Settings.SQL.MySQL.Password", ""),

    SQLITE_FILE("Settings.SQL.SQLite.File", "account.sqlite"),

    SQL_ACCOUNT_TABLE_NAME("Settings.SQL.Table.Accounts", "opteco"),
    SQL_TRANSACT_TABLE_NAME("Settings.SQL.Table.Transactions", "opteco_transaction"),

    COMMAND_LIMIT_TOP("Settings.Preferences.LimitTopCommand", 5),
    COUNTDOWN_ENABLE("Settings.Payment.Pending.Countdown.Enable", true),
    COUNTDOWN_TYPE("Settings.Payment.Pending.Countdown.Type", "message"),

    REFRESH_TIME("Settings.Preferences.Cache.RefreshTime", 5),

    DEBUG("Debug", false),
    LANGUAGE_FILE("Settings.Preferences.Language", "language.yml"),

    TITLE_ENABLE("Settings.Effect.TittleCounter.Enable", true),
    TITLE_STAY("Settings.Effect.TittleCounter.Stay", 20),
    TITLE_FADE_OUT("Settings.Effect.TittleCounter.FadeOut", 100),
    TITLE_SPEED("Settings.Effect.TittleCounter.Speed", 5),

    TITLE_LIMIT_VALUE("Settings.Effect.TitleCounter.Limit", 10000),

    TAB_EXECUTOR("Settings.Preferences.TabExecutor", true)

    ;

    private final String path;
    private final Object wh;

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
