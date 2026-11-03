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

public final class MessageService {

    private final Plugin plugin;
    private final ConcurrentMap<MessageKey, String> rawCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<MessageKey, String> coloredCache = new ConcurrentHashMap<>();
    private File file;
    private volatile YamlConfiguration configuration;

    public MessageService(@NotNull Plugin plugin, @NotNull OptEcoConfig config) {
        this.plugin = plugin;
        reload(config);
    }

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

    private void save() {
        try {
            configuration.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Cannot save language file " + file + ": " + exception.getMessage());
        }
    }

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

    @NotNull
    public String color(@NotNull String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    @NotNull
    public String colored(@NotNull MessageKey key) {
        return coloredCache.computeIfAbsent(key, ignored -> color(raw(key)));
    }

    @NotNull
    public String format(@NotNull MessageKey key, Object... placeholders) {
        return colored(MessageKey.PREFIX) + " " + ChatColor.RESET + formatPlain(key, placeholders);
    }

    @NotNull
    public String formatPlain(@NotNull MessageKey key, Object... placeholders) {
        String value = colored(key);
        for (int index = 0; index + 1 < placeholders.length; index += 2) {
            value = value.replace("%" + placeholders[index] + "%", String.valueOf(placeholders[index + 1]));
        }
        return value;
    }

    public void send(@NotNull CommandSender sender, @NotNull MessageKey key, Object... placeholders) {
        sender.sendMessage(format(key, placeholders));
    }

    public void sendPlain(@NotNull CommandSender sender, @NotNull MessageKey key, Object... placeholders) {
        sender.sendMessage(formatPlain(key, placeholders));
    }

    @NotNull
    public String number(@NotNull BigDecimal value) {
        return Money.format(value);
    }
}
