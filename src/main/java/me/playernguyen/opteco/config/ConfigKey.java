package me.playernguyen.opteco.config;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Declarative catalogue of the settings stored in {@code config.yml}.
 *
 * <p>Each constant pairs a dotted YAML path with the value used when that path
 * is absent. {@link ConfigService} copies the defaults into the file on first
 * start and after upgrades, then reads every path back into an immutable
 * {@link OptEcoConfig} snapshot. Because the enum is immutable and its fields
 * are final, constants can be shared freely between the main thread and the
 * asynchronous database threads.</p>
 */
@Getter
@Accessors(fluent = true)
public enum ConfigKey {
    /** Enables the asynchronous GitHub release check performed during startup. */
    CHECK_FOR_UPDATE("Update.CheckForUpdate", Boolean.TRUE),
    /** Diagnostic flag carried into the config snapshot; no current code path branches on it. */
    DEBUG("Debug", Boolean.FALSE),

    /** File name, relative to the {@code language} data folder, that holds the user-facing messages. */
    LANGUAGE_FILE("Settings.Preferences.Language", "en.yml"),
    /** Backend selected for account and payment storage: {@code sqlite} or {@code mysql}. */
    STORAGE_TYPE("Settings.Storage.Type", "sqlite"),
    /** Balance credited to a player the first time an account is created, parsed as a decimal amount. */
    START_BALANCE("Settings.Preferences.BeginPoint", "0"),
    /** Lowest balance a debit may leave behind; also parsed as a decimal amount. */
    MIN_BALANCE("Settings.Preferences.MinimumReachPoint", "-15"),
    /** Allows fractional balances and command amounts; when {@code false} amounts must be whole numbers. */
    USE_DOUBLE("Settings.Preferences.UseDouble", Boolean.FALSE),
    /** Text substituted for the {@code %currency%} placeholder in messages. */
    CURRENCY_SYMBOL("Settings.CurrencySymbol", "points"),
    /** Maximum number of entries rendered by the leaderboard; validated to the range 1-1000. */
    TOP_LIMIT("Settings.Preferences.LimitTopCommand", 5),
    /** Enables tab completion for {@code /opteco} and its aliases. */
    TAB_COMPLETE("Settings.Preferences.TabExecutor", Boolean.TRUE),

    /** Seconds a pending payment stays confirmable before it expires; validated to the range 1-3600. */
    PAYMENT_DURATION("Settings.Payment.Pending.Duration", 15),
    /** Enables the once-per-second countdown notice for pending payments. */
    COUNTDOWN_ENABLE("Settings.Payment.Pending.Countdown.Enable", Boolean.TRUE),
    /** Countdown delivery channel, {@code message} or {@code title}. */
    COUNTDOWN_TYPE("Settings.Payment.Pending.Countdown.Type", "message"),

    /** Enables the animated title shown to a player after their balance changes. */
    TITLE_ENABLE("Settings.Effect.TitleCounter.Enable", Boolean.TRUE),
    /** Ticks the balance title remains fully visible; validated to the range 0-1000. */
    TITLE_STAY("Settings.Effect.TitleCounter.Stay", 20),
    /** Ticks used to fade the balance title out; validated to the range 0-1000. */
    TITLE_FADE_OUT("Settings.Effect.TitleCounter.FadeOut", 100),
    /** Divisor that shortens the title animation as it grows; validated to the range 1-100. */
    TITLE_SPEED("Settings.Effect.TitleCounter.Speed", 5),
    /** Largest balance delta animated by the title; larger deltas are clamped, range 0-{@link Integer#MAX_VALUE}. */
    TITLE_LIMIT("Settings.Effect.TitleCounter.Limit", 10000),

    /** Host name of the MySQL server used when {@link #STORAGE_TYPE} is {@code mysql}. */
    MYSQL_HOST("Settings.SQL.MySQL.Host", "localhost"),
    /** MySQL TCP port; validated to the range 1-65535. */
    MYSQL_PORT("Settings.SQL.MySQL.Port", 3306),
    /** Database name that contains the OptEco tables; defaults to the {@code dbname} placeholder. */
    MYSQL_DATABASE("Settings.SQL.MySQL.Database", "dbname"),
    /** MySQL login user. */
    MYSQL_USERNAME("Settings.SQL.MySQL.Username", "root"),
    /** MySQL login password; the empty string means no password. */
    MYSQL_PASSWORD("Settings.SQL.MySQL.Password", ""),
    /**
     * Query string appended to the MySQL JDBC URL, for example the SSL, character set and time zone
     * options shipped in {@code config.yml}. Ignored for SQLite storage.
     */
    MYSQL_PARAMETER(
            "Settings.SQL.MySQL.Parameter",
            "useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8&serverTimezone=UTC"),
    /** SQLite database file, resolved against the plugin data folder. */
    SQLITE_FILE("Settings.SQL.SQLite.File", "account.sqlite"),

    /** Table holding balances; must be a plain identifier and differ from {@link #TRANSACTION_TABLE}. */
    ACCOUNT_TABLE("Settings.SQL.Table.Accounts", "opteco_accounts"),
    /** Table holding payment transactions; must be a plain identifier and differ from {@link #ACCOUNT_TABLE}. */
    TRANSACTION_TABLE("Settings.SQL.Table.Transactions", "opteco_transactions");

    /** Dotted YAML path this key is stored under. */
    private final String path;

    /** Value written when the path is missing; its runtime type drives the YAML getter used later. */
    private final Object defaultValue;

    /**
     * Creates a configuration key.
     *
     * @param path dotted YAML path under which the value is stored
     * @param defaultValue value written when the path is absent; must match the YAML type expected by the reader
     */
    ConfigKey(String path, Object defaultValue) {
        this.path = path;
        this.defaultValue = defaultValue;
    }
}
