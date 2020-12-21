package me.playernguyen.opteco.api.shopguiplus;

import me.playernguyen.opteco.OptEco;
import net.brcdev.shopgui.ShopGuiPlusApi;
import net.brcdev.shopgui.provider.economy.EconomyProvider;
import org.bukkit.entity.Player;

public class OptEcoShopGuiPlusEconomyProvider extends EconomyProvider {

    private final OptEco optEco;

    public OptEcoShopGuiPlusEconomyProvider(OptEco optEco) {
        this.optEco = optEco;

    }

    public void register() {
        // Register it
        ShopGuiPlusApi.registerEconomyProvider(this);
    }

    @Override
    public String getName() {
        return optEco.getName();
    }

    @Override
    public double getBalance(Player player) {
        return optEco.getAccountManager().get(player.getUniqueId()).getBalance();
    }

    @Override
    public void deposit(Player player, double amount) {
        // Add balance
        optEco.getAccountDatabase().addBalance(player.getUniqueId(), amount);
        // Request update
        optEco.getAccountManager().refresh(player.getUniqueId());
    }

    @Override
    public void withdraw(Player player, double amount) {
        // Take balance
        optEco.getAccountDatabase().takeBalance(player.getUniqueId(), amount);
        // Request update
        optEco.getAccountManager().refresh(player.getUniqueId());
    }
}
