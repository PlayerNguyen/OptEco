package me.playernguyen.opteco.message;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import me.playernguyen.opteco.config.OptEcoConfig;
import me.playernguyen.opteco.core.model.Money;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Loads language files and renders the messages used by commands, hooks and listeners.
 *
 * <p>The language file is resolved inside the {@code language} folder of the
 * plugin data folder, using the configured {@code languageFile} name. On
 * construction and on every {@link #reload} the service creates the file when
 * necessary, merges missing {@link MessageKey} defaults, and clears both caches
 * so edits take effect immediately. Raw and colour-translated values are cached
 * in {@link ConcurrentMap}s, while the current YAML document is held in a
 * volatile field that is replaced under the {@code reload} monitor. Formatting
 * and sending are therefore safe from asynchronous callbacks once a reload has
 * completed.</p>
 */
public final class MessageService {

    /** Plugin used for the data folder and warning logging. */
    private final Plugin plugin;

    /** Resolved text with colour codes and placeholders intact. */
    private final ConcurrentMap<MessageKey, String> rawCache = new ConcurrentHashMap<>();

    /** Colour-translated text; cleared whenever the language file changes. */
    private final ConcurrentMap<MessageKey, String> coloredCache = new ConcurrentHashMap<>();

    /** Current language file, always under the {@code language} data folder. */
    private File file;

    /** Parsed language document swapped as a whole on reload. */
    private volatile YamlConfiguration configuration;

    /**
     * Creates the service and loads the language file selected by the snapshot.
     *
     * @param plugin owning plugin, used for the data folder and warning logging
     * @param config snapshot whose {@code languageFile} setting selects the file
     * @throws IllegalStateException when the {@code language} folder cannot be created
     */
    public MessageService(@NotNull Plugin plugin, @NotNull OptEcoConfig config) {
        this.plugin = plugin;
        reload(config);
    }

    /**
     * Switches to the language file named by the snapshot, merging missing defaults and clearing caches.
     *
     * <p>Existing files keep their translated values; only paths that are absent are added. The method is
     * synchronized because it replaces the shared YAML document and both caches.</p>
     *
     * @param config snapshot whose {@code languageFile} setting selects the file
     * @throws IllegalStateException when the {@code language} folder cannot be created
     */
    public synchronized void reload(OptEcoConfig config) {
        File languageFolder = new File(plugin.getDataFolder(), "language");
        if (!languageFolder.exists() && !languageFolder.mkdirs() && !languageFolder.isDirectory()) {
            throw new IllegalStateException("Cannot create language folder: " + languageFolder);
        }
        this.file = new File(languageFolder, config.languageFile());
        if (!file.exists()) {
            this.configuration = new YamlConfiguration();
            for (MessageKey key : MessageKey.values()) {
                configuration.set(key.path(), key.defaultValue());
            }
            save();
        } else {
            this.configuration = YamlConfiguration.loadConfiguration(file);
            boolean changed = false;
            for (MessageKey key : MessageKey.values()) {
                if (!configuration.contains(key.path())) {
                    configuration.set(key.path(), key.defaultValue());
                    changed = true;
                }
            }
            if (changed) {
                save();
            }
        }
        rawCache.clear();
        coloredCache.clear();
    }

    /**
     * Persists the current language document, logging instead of throwing so a read-only folder cannot break a
     * command or startup.
     */
    private void save() {
        try {
            configuration.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Cannot save language file " + file + ": " + exception.getMessage());
        }
    }

    /**
     * Returns the configured text without translating colour codes or placeholders.
     *
     * @param key message to resolve
     * @return configured text, or the key default when the path is missing
     */
    @NotNull
    public String raw(@NotNull MessageKey key) {
        String value = rawCache.get(key);
        if (value != null) {
            return value;
        }
        String configured = configuration.getString(key.path());
        String resolved = configured == null ? key.defaultValue() : configured;
        rawCache.put(key, resolved);
        return resolved;
    }

    /**
     * Translates ampersand colour codes into section-sign codes.
     *
     * @param value text that may contain ampersand colour markers
     * @return text with Bukkit colour codes
     */
    @NotNull
    public String color(@NotNull String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    /**
     * Returns the message with colour codes applied, caching the result.
     *
     * @param key message to resolve
     * @return colour-translated text without the {@link MessageKey#PREFIX} prefix
     */
    @NotNull
    public String colored(@NotNull MessageKey key) {
        return coloredCache.computeIfAbsent(key, ignored -> color(raw(key)));
    }

    /**
     * Renders a message with the plugin prefix followed by a colour reset.
     *
     * @param key message to render
     * @param placeholders alternating placeholder names and values; a trailing name without a value is ignored
     * @return the prefixed, colour-translated message
     */
    @NotNull
    public String format(@NotNull MessageKey key, Object... placeholders) {
        return colored(MessageKey.PREFIX) + " " + ChatColor.RESET + formatPlain(key, placeholders);
    }

    /**
     * Renders a message without the plugin prefix.
     *
     * @param key message to render
     * @param placeholders alternating placeholder names and values, substituted as {@code %name%}
     * @return the colour-translated message
     */
    @NotNull
    public String formatPlain(@NotNull MessageKey key, Object... placeholders) {
        String value = colored(key);
        for (int index = 0; index + 1 < placeholders.length; index += 2) {
            value = value.replace("%" + placeholders[index] + "%", String.valueOf(placeholders[index + 1]));
        }
        return value;
    }

    /**
     * Sends a prefixed message to a command sender.
     *
     * @param sender recipient, such as a player or the console
     * @param key message to render
     * @param placeholders alternating placeholder names and values
     */
    public void send(@NotNull CommandSender sender, @NotNull MessageKey key, Object... placeholders) {
        sender.sendMessage(format(key, placeholders));
    }

    /**
     * Sends a message without the plugin prefix.
     *
     * @param sender recipient, such as a player or the console
     * @param key message to render
     * @param placeholders alternating placeholder names and values
     */
    public void sendPlain(@NotNull CommandSender sender, @NotNull MessageKey key, Object... placeholders) {
        sender.sendMessage(formatPlain(key, placeholders));
    }

    /**
     * Renders an amount for chat output.
     *
     * @param value balance or delta to display
     * @return plain decimal text with trailing zeros removed
     */
    @NotNull
    public String number(@NotNull BigDecimal value) {
        return Money.format(value);
    }
}
