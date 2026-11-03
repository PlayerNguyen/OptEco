package me.playernguyen.opteco.hook;

import java.math.BigDecimal;
import me.playernguyen.opteco.core.model.BalanceChangeReason;
import me.playernguyen.opteco.core.service.EconomyService;
import net.brcdev.shopgui.ShopGuiPlusApi;
import net.brcdev.shopgui.provider.economy.EconomyProvider;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class ShopGuiPlusHook extends EconomyProvider {

    private final Plugin plugin;
    private final EconomyService economy;

    public ShopGuiPlusHook(@NotNull Plugin plugin, @NotNull EconomyService economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    public void register() {
        ShopGuiPlusApi.registerEconomyProvider(this);
    }

    @Override
    public String getName() {
        return plugin.getName();
    }

    @Override
    public double getBalance(@NotNull Player player) {
        return economy.cachedBalance(player.getUniqueId())
                .orElse(BigDecimal.ZERO)
                .doubleValue();
    }

    @Override
    public void deposit(@NotNull Player player, double amount) {
        try {
            economy.add(
                    player.getUniqueId(), player.getName(), BigDecimal.valueOf(amount), BalanceChangeReason.API_ADD);
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("ShopGUIPlus deposit failed: " + exception.getMessage());
        }
    }

    @Override
    public void withdraw(@NotNull Player player, double amount) {
        try {
            boolean taken = economy.take(
                    player.getUniqueId(), player.getName(), BigDecimal.valueOf(amount), BalanceChangeReason.API_TAKE);
            if (!taken) {
                plugin.getLogger().warning("ShopGUIPlus withdraw failed for " + player.getName());
            }
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("ShopGUIPlus withdraw failed: " + exception.getMessage());
        }
    }
}
