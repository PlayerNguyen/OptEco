package me.playernguyen.opteco.config;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.playernguyen.opteco.core.model.Money;

@Getter
@Accessors(fluent = true)
@Builder
public final class OptEcoConfig {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9_]+");
    private static final Pattern LANGUAGE_FILE = Pattern.compile("[A-Za-z0-9_.-]+");

    @Builder.Default
    private final boolean checkForUpdate = true;

    @Builder.Default
    private final boolean debug = false;

    @Builder.Default
    private final boolean useDouble = false;

    @Builder.Default
    private final boolean tabComplete = true;

    @Builder.Default
    private final boolean countdownEnabled = true;

    @Builder.Default
    private final boolean titleEnabled = true;

    @Builder.Default
    private final String countdownType = "message";

    @Builder.Default
    private final String languageFile = "en.yml";

    @Builder.Default
    private final String currencySymbol = "points";

    @Builder.Default
    private final BigDecimal startBalance = BigDecimal.ZERO;

    @Builder.Default
    private final BigDecimal minBalance = BigDecimal.valueOf(-15);

    @Builder.Default
    private final int topLimit = 5;

    @Builder.Default
    private final int paymentDurationSeconds = 15;

    @Builder.Default
    private final int titleStay = 20;

    @Builder.Default
    private final int titleFadeOut = 100;

    @Builder.Default
    private final int titleSpeed = 5;

    @Builder.Default
    private final int titleLimit = 10000;

    @Builder.Default
    private final StorageType storageType = StorageType.SQLITE;

    @Builder.Default
    private final String mysqlHost = "localhost";

    @Builder.Default
    private final int mysqlPort = 3306;

    @Builder.Default
    private final String mysqlDatabase = "dbname";

    @Builder.Default
    private final String mysqlUsername = "root";

    @Builder.Default
    private final String mysqlPassword = "";

    @Builder.Default
    private final String mysqlParameters =
            "useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8&serverTimezone=UTC";

    @Builder.Default
    private final String sqliteFile = "account.sqlite";

    @Builder.Default
    private final String accountTable = "opteco_accounts";

    @Builder.Default
    private final String transactionTable = "opteco_transactions";

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

    private static BigDecimal normalizedMoney(BigDecimal value, String label) {
        try {
            return Money.normalize(value);
        } catch (IllegalArgumentException exception) {
            throw new ConfigException("Invalid " + label + ": " + exception.getMessage(), exception);
        }
    }

    private static int requireRange(int value, int min, int max, String label) {
        if (value < min || value > max) {
            throw new ConfigException(label + " must be between " + min + " and " + max);
        }
        return value;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new ConfigException(label + " cannot be empty");
        }
        return value.trim();
    }

    private static String requireIdentifier(String value, String label) {
        String trimmed = requireText(value, label);
        if (!IDENTIFIER.matcher(trimmed).matches()) {
            throw new ConfigException(label + " must match " + IDENTIFIER.pattern());
        }
        return trimmed;
    }

    private static String requireLanguageFile(String value) {
        String trimmed = requireText(value, "language file");
        if (!LANGUAGE_FILE.matcher(trimmed).matches() || trimmed.contains("..")) {
            throw new ConfigException("language file must be a simple file name");
        }
        return trimmed;
    }

    private static String normalizeCountdownType(String value) {
        String normalized = value == null ? "message" : value.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals("message") && !normalized.equals("title")) {
            throw new ConfigException("Countdown type must be message or title");
        }
        return normalized;
    }
}
