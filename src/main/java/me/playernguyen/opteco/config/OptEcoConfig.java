package me.playernguyen.opteco.config;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.playernguyen.opteco.core.model.Money;

/**
 * Immutable snapshot of the validated plugin settings.
 *
 * <p>{@link ConfigService} builds an instance through the Lombok builder, calls
 * {@link #validate()}, and only then publishes it. Commands, services and hooks
 * read the snapshot through {@link ConfigService#current()} and
 * {@code /points reload} replaces the whole instance rather than mutating it.
 * Every field is final and money values are immutable {@link BigDecimal}
 * instances, so a snapshot can be shared between the main thread and the
 * asynchronous database threads without further synchronization.</p>
 */
@Getter
@Accessors(fluent = true)
@Builder
public final class OptEcoConfig {

    /** Accepted table names: letters, digits and underscores only. */
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9_]+");

    /** Accepted language file names; a simple name without path separators. */
    private static final Pattern LANGUAGE_FILE = Pattern.compile("[A-Za-z0-9_.-]+");

    /** Whether the startup check queries the GitHub releases API for a newer version. Defaults to {@code true}. */
    @Builder.Default
    private final boolean checkForUpdate = true;

    /**
     * Diagnostic flag exposed to integrations; no bundled code path currently branches on it. Defaults to
     * {@code false}.
     */
    @Builder.Default
    private final boolean debug = false;

    /**
     * Whether fractional balances and command amounts are allowed. When {@code false}, the start and minimum
     * balances and every command amount must be whole numbers. Defaults to {@code false}.
     */
    @Builder.Default
    private final boolean useDouble = false;

    /** Whether {@code /opteco} and its aliases offer tab completion. Defaults to {@code true}. */
    @Builder.Default
    private final boolean tabComplete = true;

    /** Whether pending payments receive a periodic countdown notice. Defaults to {@code true}. */
    @Builder.Default
    private final boolean countdownEnabled = true;

    /** Whether a committed balance change triggers the animated title effect. Defaults to {@code true}. */
    @Builder.Default
    private final boolean titleEnabled = true;

    /**
     * Delivery channel for the payment countdown, either {@code message} or {@code title}. Defaults to
     * {@code message}; {@link #validate()} accepts either value case-insensitively but does not rewrite the field.
     */
    @Builder.Default
    private final String countdownType = "message";

    /**
     * Language file name resolved inside the {@code language} data folder. Must be a simple file name such as
     * {@code en.yml}; defaults to {@code en.yml}.
     */
    @Builder.Default
    private final String languageFile = "en.yml";

    /** Text substituted for the {@code %currency%} placeholder in messages. Defaults to {@code points}. */
    @Builder.Default
    private final String currencySymbol = "points";

    /**
     * Balance granted when an account is created, normalized to {@link Money#SCALE} decimal places. Defaults to
     * zero and must be a whole number unless {@link #useDouble} is enabled.
     */
    @Builder.Default
    private final BigDecimal startBalance = BigDecimal.ZERO;

    /**
     * Lowest balance a debit may leave behind, also normalized to {@link Money#SCALE} decimal places. Defaults to
     * {@code -15} and must be a whole number unless {@link #useDouble} is enabled.
     */
    @Builder.Default
    private final BigDecimal minBalance = BigDecimal.valueOf(-15);

    /** Maximum number of leaderboard rows; validated to 1-1000. Defaults to {@code 5}. */
    @Builder.Default
    private final int topLimit = 5;

    /** Seconds a pending payment stays confirmable; validated to 1-3600. Defaults to {@code 15}. */
    @Builder.Default
    private final int paymentDurationSeconds = 15;

    /** Ticks the balance title stays fully visible; validated to 0-1000. Defaults to {@code 20}. */
    @Builder.Default
    private final int titleStay = 20;

    /** Ticks spent fading the balance title out; validated to 0-1000. Defaults to {@code 100}. */
    @Builder.Default
    private final int titleFadeOut = 100;

    /**
     * Divisor that shortens the title animation as it grows; validated to 1-100. The listener derives the animation
     * length from {@code 100 / speed}, clamped to 5-100 ticks. Defaults to {@code 5}.
     */
    @Builder.Default
    private final int titleSpeed = 5;

    /**
     * Largest balance delta the title animates, from {@code 0} to {@link Integer#MAX_VALUE}. Larger deltas start
     * the animation at a fixed distance from the final balance instead. Defaults to {@code 10000}.
     */
    @Builder.Default
    private final int titleLimit = 10000;

    /** Selected storage backend. Defaults to {@link StorageType#SQLITE}; a {@code null} value is rejected. */
    @Builder.Default
    private final StorageType storageType = StorageType.SQLITE;

    /** MySQL host used when the storage backend is {@link StorageType#MYSQL}. Defaults to {@code localhost}. */
    @Builder.Default
    private final String mysqlHost = "localhost";

    /** MySQL TCP port; validated to 1-65535. Defaults to {@code 3306}. */
    @Builder.Default
    private final int mysqlPort = 3306;

    /** MySQL database name; defaults to the {@code dbname} placeholder and must not be empty. */
    @Builder.Default
    private final String mysqlDatabase = "dbname";

    /** MySQL login user; defaults to {@code root} and must not be empty. */
    @Builder.Default
    private final String mysqlUsername = "root";

    /** MySQL login password. Defaults to the empty string, which means no password. */
    @Builder.Default
    private final String mysqlPassword = "";

    /**
     * Query string appended to the MySQL JDBC URL, for example the SSL, character set and time zone options shipped
     * in {@code config.yml}. Ignored for SQLite storage.
     */
    @Builder.Default
    private final String mysqlParameters =
            "useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8&serverTimezone=UTC";

    /** SQLite database file resolved against the plugin data folder. Defaults to {@code account.sqlite}. */
    @Builder.Default
    private final String sqliteFile = "account.sqlite";

    /**
     * Table that stores balances. Must match {@link #IDENTIFIER} and differ from {@link #transactionTable};
     * defaults to {@code opteco_accounts}.
     */
    @Builder.Default
    private final String accountTable = "opteco_accounts";

    /**
     * Table that stores payment transactions. Must match {@link #IDENTIFIER} and differ from {@link #accountTable};
     * defaults to {@code opteco_transactions}.
     */
    @Builder.Default
    private final String transactionTable = "opteco_transactions";

    /**
     * Validates the snapshot before it is published; the fields themselves are never modified.
     *
     * <p>Checks that the start and minimum balances can be normalized and are whole numbers when
     * {@link #useDouble} is off, that a storage backend and a valid language file are present, that numeric
     * settings fall inside their supported ranges, that required text settings are not blank, and that both table
     * names are plain identifiers with different values.</p>
     *
     * @throws ConfigException when any setting is missing, malformed or outside its accepted range
     */
    public void validate() {
        BigDecimal normalizedStart = normalizedMoney(startBalance, "start balance");
        BigDecimal normalizedMin = normalizedMoney(minBalance, "minimum balance");
        if (!useDouble && (!Money.isInteger(normalizedStart) || !Money.isInteger(normalizedMin))) {
            throw new ConfigException("BeginPoint and MinimumReachPoint must be integers when "
                    + "Settings.Preferences.UseDouble is false");
        }
        if (storageType == null) {
            throw new ConfigException("Storage type is missing");
        }
        normalizeCountdownType(countdownType);
        requireLanguageFile(languageFile);
        requireRange(topLimit, 1, 1000, "top limit");
        requireRange(paymentDurationSeconds, 1, 3600, "payment duration");
        requireRange(titleStay, 0, 1000, "title stay");
        requireRange(titleFadeOut, 0, 1000, "title fade out");
        requireRange(titleSpeed, 1, 100, "title speed");
        requireRange(titleLimit, 0, Integer.MAX_VALUE, "title limit");
        requireRange(mysqlPort, 1, 65535, "MySQL port");
        requireText(currencySymbol, "currency symbol");
        requireText(mysqlHost, "MySQL host");
        requireText(mysqlDatabase, "MySQL database");
        requireText(mysqlUsername, "MySQL username");
        requireText(sqliteFile, "SQLite file");
        String account = requireIdentifier(accountTable, "account table");
        String transaction = requireIdentifier(transactionTable, "transaction table");
        if (account.equalsIgnoreCase(transaction)) {
            throw new ConfigException("Account and transaction tables must be different");
        }
    }

    /**
     * Normalizes an amount so it can be checked against the money rules.
     *
     * @param value amount to normalize
     * @param label human-readable field name quoted in the failure message
     * @return the amount scaled to {@link Money#SCALE} decimal places
     * @throws ConfigException when the amount is {@code null} or has too many decimal places
     */
    private static BigDecimal normalizedMoney(BigDecimal value, String label) {
        try {
            return Money.normalize(value);
        } catch (IllegalArgumentException exception) {
            throw new ConfigException("Invalid " + label + ": " + exception.getMessage(), exception);
        }
    }

    /**
     * Checks that a numeric setting lies inside an inclusive range.
     *
     * @param value setting value
     * @param min lowest accepted value
     * @param max highest accepted value
     * @param label human-readable field name quoted in the failure message
     * @return the accepted value
     * @throws ConfigException when the value is outside the range
     */
    private static int requireRange(int value, int min, int max, String label) {
        if (value < min || value > max) {
            throw new ConfigException(label + " must be between " + min + " and " + max);
        }
        return value;
    }

    /**
     * Checks that a required text setting is present.
     *
     * @param value setting value
     * @param label human-readable field name quoted in the failure message
     * @return the value with surrounding whitespace removed
     * @throws ConfigException when the value is {@code null} or blank
     */
    private static String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new ConfigException(label + " cannot be empty");
        }
        return value.trim();
    }

    /**
     * Checks that a table name is safe to interpolate as a quoted SQL identifier.
     *
     * @param value configured table name
     * @param label human-readable field name quoted in the failure message
     * @return the trimmed table name
     * @throws ConfigException when the name is blank or contains characters outside {@link #IDENTIFIER}
     */
    private static String requireIdentifier(String value, String label) {
        String trimmed = requireText(value, label);
        if (!IDENTIFIER.matcher(trimmed).matches()) {
            throw new ConfigException(label + " must match " + IDENTIFIER.pattern());
        }
        return trimmed;
    }

    /**
     * Checks that the language setting is a plain file name.
     *
     * @param value configured language file name
     * @return the trimmed file name
     * @throws ConfigException when the name is blank, contains path separators, or tries to traverse directories
     */
    private static String requireLanguageFile(String value) {
        String trimmed = requireText(value, "language file");
        if (!LANGUAGE_FILE.matcher(trimmed).matches() || trimmed.contains("..")) {
            throw new ConfigException("language file must be a simple file name");
        }
        return trimmed;
    }

    /**
     * Validates and lower-cases the countdown channel.
     *
     * @param value configured channel, may be {@code null}
     * @return {@code message} or {@code title} in lower case
     * @throws ConfigException when the value is neither channel
     */
    private static String normalizeCountdownType(String value) {
        String normalized = value == null ? "message" : value.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals("message") && !normalized.equals("title")) {
            throw new ConfigException("Countdown type must be message or title");
        }
        return normalized;
    }
}
