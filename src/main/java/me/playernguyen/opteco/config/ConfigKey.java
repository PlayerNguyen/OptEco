package me.playernguyen.opteco.config;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum ConfigKey {
    CHECK_FOR_UPDATE("Update.CheckForUpdate", Boolean.TRUE),
    DEBUG("Debug", Boolean.FALSE),

    LANGUAGE_FILE("Settings.Preferences.Language", "en.yml"),
    STORAGE_TYPE("Settings.Storage.Type", "sqlite"),
    START_BALANCE("Settings.Preferences.BeginPoint", "0"),
    MIN_BALANCE("Settings.Preferences.MinimumReachPoint", "-15"),
    USE_DOUBLE("Settings.Preferences.UseDouble", Boolean.FALSE),
    CURRENCY_SYMBOL("Settings.CurrencySymbol", "points"),
    TOP_LIMIT("Settings.Preferences.LimitTopCommand", 5),
    TAB_COMPLETE("Settings.Preferences.TabExecutor", Boolean.TRUE),

    PAYMENT_DURATION("Settings.Payment.Pending.Duration", 15),
    COUNTDOWN_ENABLE("Settings.Payment.Pending.Countdown.Enable", Boolean.TRUE),
    COUNTDOWN_TYPE("Settings.Payment.Pending.Countdown.Type", "message"),

    TITLE_ENABLE("Settings.Effect.TitleCounter.Enable", Boolean.TRUE),
    TITLE_STAY("Settings.Effect.TitleCounter.Stay", 20),
    TITLE_FADE_OUT("Settings.Effect.TitleCounter.FadeOut", 100),
    TITLE_SPEED("Settings.Effect.TitleCounter.Speed", 5),
    TITLE_LIMIT("Settings.Effect.TitleCounter.Limit", 10000),

    MYSQL_HOST("Settings.SQL.MySQL.Host", "localhost"),
    MYSQL_PORT("Settings.SQL.MySQL.Port", 3306),
    MYSQL_DATABASE("Settings.SQL.MySQL.Database", "dbname"),
    MYSQL_USERNAME("Settings.SQL.MySQL.Username", "root"),
    MYSQL_PASSWORD("Settings.SQL.MySQL.Password", ""),
    MYSQL_PARAMETER(
            "Settings.SQL.MySQL.Parameter",
            "useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8&serverTimezone=UTC"),
    SQLITE_FILE("Settings.SQL.SQLite.File", "account.sqlite"),

    ACCOUNT_TABLE("Settings.SQL.Table.Accounts", "opteco_accounts"),
    TRANSACTION_TABLE("Settings.SQL.Table.Transactions", "opteco_transactions");

    private final String path;
    private final Object defaultValue;

    ConfigKey(String path, Object defaultValue) {
        this.path = path;
        this.defaultValue = defaultValue;
    }
}
