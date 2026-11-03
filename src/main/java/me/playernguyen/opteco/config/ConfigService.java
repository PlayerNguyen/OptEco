package me.playernguyen.opteco.config;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;
import me.playernguyen.opteco.core.model.Money;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class ConfigService {

    private static final String HEADER =
            "OptEco configuration\n" + "Documentation: https://github.com/PlayerNguyen/OptEco/wiki\n";

    private final Plugin plugin;
    private final File file;
    private final AtomicReference<OptEcoConfig> current = new AtomicReference<>();

    public ConfigService(@NotNull Plugin plugin) {
        this.plugin = plugin;
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs() && !dataFolder.isDirectory()) {
            throw new ConfigException("Cannot create plugin data folder: " + dataFolder);
        }
        this.file = new File(dataFolder, "config.yml");
        reload();
    }

    @NotNull
    public OptEcoConfig current() {
        return current.get();
    }

    @NotNull
    public File file() {
        return file;
    }

    @NotNull
    public OptEcoConfig reload() {
        if (!file.exists()) {
            createDefaultFile();
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        boolean changed = applyDefaults(yaml);
        if (changed) {
            yaml.options().header(HEADER);
            save(yaml);
        }
        OptEcoConfig config = build(yaml);
        config.validate();
        current.set(config);
        return config;
    }

    private void createDefaultFile() {
        try {
            plugin.saveResource("config.yml", false);
        } catch (IllegalArgumentException ignored) {
            // The resource may be absent in a development checkout.
        }
        if (!file.exists()) {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.options().header(HEADER);
            applyDefaults(yaml);
            save(yaml);
        }
    }

    private boolean applyDefaults(YamlConfiguration yaml) {
        boolean changed = false;
        for (ConfigKey key : ConfigKey.values()) {
            if (!yaml.contains(key.path())) {
                yaml.set(key.path(), key.defaultValue());
                changed = true;
            }
        }
        return changed;
    }

    private OptEcoConfig build(YamlConfiguration yaml) {
        return OptEcoConfig.builder()
                .checkForUpdate(booleanValue(yaml, ConfigKey.CHECK_FOR_UPDATE))
                .debug(booleanValue(yaml, ConfigKey.DEBUG))
                .tabComplete(booleanValue(yaml, ConfigKey.TAB_COMPLETE))
                .useDouble(booleanValue(yaml, ConfigKey.USE_DOUBLE))
                .countdownEnabled(booleanValue(yaml, ConfigKey.COUNTDOWN_ENABLE))
                .titleEnabled(booleanValue(yaml, ConfigKey.TITLE_ENABLE))
                .countdownType(
                        stringValue(yaml, ConfigKey.COUNTDOWN_TYPE).trim().toLowerCase(java.util.Locale.ROOT))
                .languageFile(stringValue(yaml, ConfigKey.LANGUAGE_FILE))
                .currencySymbol(stringValue(yaml, ConfigKey.CURRENCY_SYMBOL))
                .startBalance(decimalValue(yaml, ConfigKey.START_BALANCE))
                .minBalance(decimalValue(yaml, ConfigKey.MIN_BALANCE))
                .topLimit(intValue(yaml, ConfigKey.TOP_LIMIT))
                .paymentDurationSeconds(intValue(yaml, ConfigKey.PAYMENT_DURATION))
                .titleStay(intValue(yaml, ConfigKey.TITLE_STAY))
                .titleFadeOut(intValue(yaml, ConfigKey.TITLE_FADE_OUT))
                .titleSpeed(intValue(yaml, ConfigKey.TITLE_SPEED))
                .titleLimit(intValue(yaml, ConfigKey.TITLE_LIMIT))
                .storageType(StorageType.fromString(stringValue(yaml, ConfigKey.STORAGE_TYPE)))
                .mysqlHost(stringValue(yaml, ConfigKey.MYSQL_HOST))
                .mysqlPort(intValue(yaml, ConfigKey.MYSQL_PORT))
                .mysqlDatabase(stringValue(yaml, ConfigKey.MYSQL_DATABASE))
                .mysqlUsername(stringValue(yaml, ConfigKey.MYSQL_USERNAME))
                .mysqlPassword(stringValue(yaml, ConfigKey.MYSQL_PASSWORD))
                .mysqlParameters(stringValue(yaml, ConfigKey.MYSQL_PARAMETER))
                .sqliteFile(stringValue(yaml, ConfigKey.SQLITE_FILE))
                .accountTable(stringValue(yaml, ConfigKey.ACCOUNT_TABLE))
                .transactionTable(stringValue(yaml, ConfigKey.TRANSACTION_TABLE))
                .build();
    }

    private String stringValue(YamlConfiguration yaml, ConfigKey key) {
        String value = yaml.getString(key.path());
        if (value == null) {
            throw new ConfigException("Missing configuration value: " + key.path());
        }
        return value;
    }

    private boolean booleanValue(YamlConfiguration yaml, ConfigKey key) {
        return yaml.getBoolean(key.path(), (Boolean) key.defaultValue());
    }

    private int intValue(YamlConfiguration yaml, ConfigKey key) {
        return yaml.getInt(key.path(), ((Number) key.defaultValue()).intValue());
    }

    private BigDecimal decimalValue(YamlConfiguration yaml, ConfigKey key) {
        Object raw = yaml.get(key.path());
        if (raw == null) {
            raw = key.defaultValue();
        }
        try {
            return Money.parse(String.valueOf(raw));
        } catch (RuntimeException exception) {
            throw new ConfigException("Invalid number at " + key.path() + ": " + raw, exception);
        }
    }

    private void save(YamlConfiguration yaml) {
        try {
            yaml.save(file);
        } catch (IOException exception) {
            throw new ConfigException("Cannot save " + file, exception);
        }
    }
}
