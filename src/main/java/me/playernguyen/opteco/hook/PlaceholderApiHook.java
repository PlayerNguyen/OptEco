package me.playernguyen.opteco.hook;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.playernguyen.opteco.config.ConfigService;
import me.playernguyen.opteco.core.model.Money;
import me.playernguyen.opteco.core.service.EconomyService;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PlaceholderApiHook extends PlaceholderExpansion {

    private final Plugin plugin;
    private final EconomyService economy;
    private final ConfigService config;

    public PlaceholderApiHook(@NotNull Plugin plugin, @NotNull EconomyService economy, @NotNull ConfigService config) {
        this.plugin = plugin;
        this.economy = economy;
        this.config = config;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "opteco";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    @Nullable
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        Optional<BigDecimal> cached = economy.cachedBalance(player.getUniqueId());
        if ("points".equalsIgnoreCase(params) || "balance".equalsIgnoreCase(params)) {
            return cached.map(Money::format).orElse("0");
        }
        if ("points_rounded".equalsIgnoreCase(params)) {
            return cached.map(value -> value.setScale(0, RoundingMode.HALF_UP).toPlainString())
                    .orElse("0");
        }
        if ("currency".equalsIgnoreCase(params)) {
            return config.current().currencySymbol();
        }
        if ("version".equalsIgnoreCase(params)) {
            return plugin.getDescription().getVersion();
        }
        return null;
    }
}
