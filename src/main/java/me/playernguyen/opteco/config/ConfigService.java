package me.playernguyen.opteco.config;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.playernguyen.opteco.core.model.Money;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Creates, loads and reloads the plugin's {@code config.yml}.
 *
 * <p>The service owns the file inside the plugin data folder, writes any
 * missing {@link ConfigKey} defaults back to disk, builds an immutable
 * {@link OptEcoConfig}, and validates it before publishing it through an
 * {@link AtomicReference}. Readers call {@link #current()} and never observe a
 * partially populated snapshot because a reload builds the replacement first
 * and swaps it in with a single atomic write. The constructor performs the
 * initial load, so an invalid configuration fails plugin startup instead of
 * surfacing on the first balance operation. Reloading cannot change the
 * storage backend; {@link me.playernguyen.opteco.OptEco#reloadConfiguration()}
 * reports that case so the sender can be told a restart is required.</p>
 */
@Accessors(fluent = true)
public final class ConfigService {

    /** Header written to generated configuration files. */
    private static final String HEADER =
            "OptEco configuration\n" + "Documentation: https://github.com/PlayerNguyen/OptEco/wiki\n";

    /** Plugin used for the data folder, the bundled resource and logging. */
    private final Plugin plugin;

    /** {@code config.yml} inside the plugin data folder. */
    @Getter
    @NotNull
    private final File file;

    /** Most recently validated snapshot; replaced as a whole on reload. */
    private final AtomicReference<OptEcoConfig> current = new AtomicReference<>();

    /**
     * Creates the service and performs the initial configuration load.
     *
     * @param plugin owning plugin, used to locate the data folder and copy the bundled {@code config.yml}
     * @throws ConfigException when the data folder cannot be created, the file cannot be saved, or a value fails
     *     validation
     */
    public ConfigService(@NotNull Plugin plugin) {
        this.plugin = plugin;
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs() && !dataFolder.isDirectory()) {
            throw new ConfigException("Cannot create plugin data folder: " + dataFolder);
        }
        this.file = new File(dataFolder, "config.yml");
        reload();
    }

    /**
     * Returns the currently published configuration snapshot.
     *
     * @return the validated snapshot currently in use
     */
    @NotNull
    public OptEcoConfig current() {
        return current.get();
    }

    /**
     * Re-reads the file, fills in missing defaults, validates the result and atomically publishes it.
     *
     * @return the new validated snapshot
     * @throws ConfigException when the file cannot be saved or a value is missing, malformed or out of range
     */
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

    /**
     * Copies the bundled {@code config.yml} or synthesizes one from the key defaults.
     *
     * <p>The bundled resource may be absent in a development checkout, in which case the file is generated
     * from {@link ConfigKey} defaults instead.</p>
     */
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

    /**
     * Adds every {@link ConfigKey} that is missing from the parsed file.
     *
     * @param yaml parsed configuration to update in memory
     * @return {@code true} when at least one default was added
     */
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

    /**
     * Maps the parsed YAML values onto a builder snapshot. The result is not validated yet.
     *
     * @param yaml parsed configuration containing every key
     * @return an unvalidated snapshot
     * @throws ConfigException when a required string is missing, a monetary value cannot be parsed, or the storage
     *     type is unknown
     */
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

    /**
     * Reads a required string value.
     *
     * @param yaml parsed configuration
     * @param key key to read
     * @return configured text
     * @throws ConfigException when the path is missing or not a string
     */
    private String stringValue(YamlConfiguration yaml, ConfigKey key) {
        String value = yaml.getString(key.path());
        if (value == null) {
            throw new ConfigException("Missing configuration value: " + key.path());
        }
        return value;
    }

    /**
     * Reads a boolean value, falling back to the key default.
     *
     * @param yaml parsed configuration
     * @param key boolean key to read
     * @return configured value, or the key default when absent
     */
    private boolean booleanValue(YamlConfiguration yaml, ConfigKey key) {
        return yaml.getBoolean(key.path(), (Boolean) key.defaultValue());
    }

    /**
     * Reads an integer value, falling back to the key default.
     *
     * @param yaml parsed configuration
     * @param key numeric key to read
     * @return configured value, or the key default when absent
     */
    private int intValue(YamlConfiguration yaml, ConfigKey key) {
        return yaml.getInt(key.path(), ((Number) key.defaultValue()).intValue());
    }

    /**
     * Reads a monetary value and normalizes it to the storage scale.
     *
     * @param yaml parsed configuration
     * @param key monetary key to read
     * @return the parsed amount scaled to {@link Money#SCALE} decimal places
     * @throws ConfigException when the stored text is not a valid amount
     */
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

    /**
     * Writes the YAML document to {@link #file}.
     *
     * @param yaml configuration to persist
     * @throws ConfigException when the file cannot be written
     */
    private void save(YamlConfiguration yaml) {
        try {
            yaml.save(file);
        } catch (IOException exception) {
            throw new ConfigException("Cannot save " + file, exception);
        }
    }
}
