package me.playernguyen.opteco.hook;

import java.math.BigDecimal;
import me.playernguyen.opteco.config.ConfigService;
import me.playernguyen.opteco.core.exception.StorageException;
import me.playernguyen.opteco.core.model.BalanceChangeReason;
import me.playernguyen.opteco.core.service.EconomyService;
import org.black_ixx.bossshop.pointsystem.BSPointsPlugin;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class BossShopProHook extends BSPointsPlugin {

    private final Plugin plugin;
    private final EconomyService economy;
    private final ConfigService config;

    public BossShopProHook(@NotNull Plugin plugin, @NotNull EconomyService economy, @NotNull ConfigService config) {
        super(plugin.getName(), "opteco");
        this.plugin = plugin;
        this.economy = economy;
        this.config = config;
    }

    @Override
    public double getPoints(OfflinePlayer player) {
        try {
            return economy.cachedBalance(player.getUniqueId())
                    .orElseGet(() -> economy.getBalance(player.getUniqueId()))
                    .doubleValue();
        } catch (StorageException exception) {
            plugin.getLogger().warning("BossShopPro balance lookup failed: " + exception.getMessage());
            return 0D;
        }
    }

    @Override
    public double setPoints(OfflinePlayer player, double points) {
        try {
            BigDecimal amount = BigDecimal.valueOf(points);
            economy.set(player.getUniqueId(), player.getName(), amount, BalanceChangeReason.API_SET);
            return points;
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("BossShopPro setPoints failed: " + exception.getMessage());
            return 0D;
        }
    }

    @Override
    public double takePoints(OfflinePlayer player, double points) {
        try {
            BigDecimal amount = BigDecimal.valueOf(points);
            return economy.take(player.getUniqueId(), player.getName(), amount, BalanceChangeReason.API_TAKE)
                    ? points
                    : 0D;
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("BossShopPro takePoints failed: " + exception.getMessage());
            return 0D;
        }
    }

    @Override
    public double givePoints(OfflinePlayer player, double points) {
        try {
            BigDecimal amount = BigDecimal.valueOf(points);
            economy.add(player.getUniqueId(), player.getName(), amount, BalanceChangeReason.API_ADD);
            return points;
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("BossShopPro givePoints failed: " + exception.getMessage());
            return 0D;
        }
    }

    @Override
    public boolean usesDoubleValues() {
        return config.current().useDouble();
    }
}
