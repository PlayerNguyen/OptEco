package me.playernguyen.opteco.hook;

import be.maximvdw.placeholderapi.PlaceholderAPI;
import be.maximvdw.placeholderapi.PlaceholderReplaceEvent;
import be.maximvdw.placeholderapi.PlaceholderReplacer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import me.playernguyen.opteco.config.ConfigService;
import me.playernguyen.opteco.core.model.Money;
import me.playernguyen.opteco.core.service.EconomyService;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class MvdwPlaceholderHook {

    private final Plugin plugin;
    private final EconomyService economy;
    private final ConfigService config;

    public MvdwPlaceholderHook(@NotNull Plugin plugin, @NotNull EconomyService economy, @NotNull ConfigService config) {
        this.plugin = plugin;
        this.economy = economy;
        this.config = config;
    }

    public void register() {
        PlaceholderReplacer points = event -> formatPoints(event, false);
        PlaceholderReplacer rounded = event -> formatPoints(event, true);
        PlaceholderAPI.registerPlaceholder(plugin, "opteco_points", points);
        PlaceholderAPI.registerPlaceholder(plugin, "opteco_points_rounded", rounded);
        PlaceholderAPI.registerPlaceholder(
                plugin, "opteco_currency", event -> config.current().currencySymbol());
        PlaceholderAPI.registerPlaceholder(
                plugin, "opteco_version", event -> plugin.getDescription().getVersion());
    }

    private String formatPoints(PlaceholderReplaceEvent event, boolean rounded) {
        if (event.getPlayer() == null) {
            return "0";
        }
        Optional<BigDecimal> balance = economy.cachedBalance(event.getPlayer().getUniqueId());
        if (!balance.isPresent()) {
            return "0";
        }
        if (rounded) {
            return balance.get().setScale(0, RoundingMode.HALF_UP).toPlainString();
        }
        return Money.format(balance.get());
    }
}
