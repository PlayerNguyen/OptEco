package me.playernguyen.opteco.hook;

import java.math.BigDecimal;
import me.playernguyen.opteco.core.model.BalanceChangeReason;
import me.playernguyen.opteco.core.service.EconomyService;
import net.brcdev.shopgui.ShopGuiPlusApi;
import net.brcdev.shopgui.provider.economy.EconomyProvider;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Optional ShopGUIPlus economy provider backed by {@link EconomyService}.
 *
 * <p>The plugin registers this provider one tick after enable, because
 * ShopGUIPlus must finish its own startup first, and only when that plugin is
 * present. Balances are read from the OptEco cache; deposits and withdrawals go
 * through the economy service, so they are blocking and publish the usual balance
 * events. The integration is optional and must stay null-safe: failures are
 * logged and swallowed rather than thrown into ShopGUIPlus, whose API has no
 * error channel.
 */
public final class ShopGuiPlusHook extends EconomyProvider {

    /** Plugin used for logging failed shop operations. */
    private final Plugin plugin;

    /** Economy service that owns balances and cache updates. */
    private final EconomyService economy;

    /**
     * Creates the provider. Call {@link #register()} once ShopGUIPlus is ready.
     *
     * @param plugin the owning plugin, used for the provider name and logging
     * @param economy the economy service backing this provider
     */
    public ShopGuiPlusHook(@NotNull Plugin plugin, @NotNull EconomyService economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    /**
     * Registers this provider with the ShopGUIPlus API.
     */
    public void register() {
        ShopGuiPlusApi.registerEconomyProvider(this);
    }

    /**
     * Returns the provider name shown by ShopGUIPlus.
     *
     * @return the owning plugin name
     */
    @Override
    public String getName() {
        return plugin.getName();
    }

    /**
     * Reads a player's balance from the account cache.
     *
     * @param player the player whose balance is requested
     * @return the cached balance, or zero when the player is not cached
     */
    @Override
    public double getBalance(@NotNull Player player) {
        return economy.cachedBalance(player.getUniqueId())
                .orElse(BigDecimal.ZERO)
                .doubleValue();
    }

    /**
     * Credits a player, for example when a shop sells an item.
     *
     * @param player the player receiving the money
     * @param amount the amount to add
     */
    @Override
    public void deposit(@NotNull Player player, double amount) {
        try {
            economy.add(
                    player.getUniqueId(), player.getName(), BigDecimal.valueOf(amount), BalanceChangeReason.API_ADD);
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("ShopGUIPlus deposit failed: " + exception.getMessage());
        }
    }

    /**
     * Charges a player, for example when a shop buys an item.
     *
     * <p>An insufficient balance is logged as a warning because the API method
     * cannot report failure to the caller.
     *
     * @param player the player paying
     * @param amount the amount to remove
     */
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
