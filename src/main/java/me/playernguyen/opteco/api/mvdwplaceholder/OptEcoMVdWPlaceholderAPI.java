package me.playernguyen.opteco.api.mvdwplaceholder;

import be.maximvdw.placeholderapi.PlaceholderAPI;
import be.maximvdw.placeholderapi.PlaceholderReplacer;
import me.playernguyen.opteco.OptEco;
import me.playernguyen.opteco.OptEcoConfiguration;
import me.playernguyen.opteco.account.OptEcoCacheAccount;

import java.util.Map;
import java.util.TreeMap;

public class OptEcoMVdWPlaceholderAPI {

    private final OptEco plugin;

    public OptEcoMVdWPlaceholderAPI(OptEco plugin) {
        this.plugin = plugin;
    }

    public void register() {
        // Registering provider
        plugin.getLogger().info("Initiating the provider...");
        // {opteco_points}
        Map<String, PlaceholderReplacer> replacerMap = new TreeMap<>();
        replacerMap.put("opteco_points", (event) -> {
            // Get account
            OptEcoCacheAccount account = plugin.getAccountManager().get(event.getPlayer().getUniqueId());
            // Whether null, return NaN
            if (account == null) {
                return "NaN";
            }
            // Return the value
            return String.valueOf(account.getBalance());
        });
        // {opteco_points_rounded}
        replacerMap.put("opteco_points_rounded", (event) -> {
            // Get account
            OptEcoCacheAccount account = plugin.getAccountManager().get(event.getPlayer().getUniqueId());
            // Whether null, return NaN
            if (account == null) {
                return "NaN";
            }
            // Return the rounded value
            return String.valueOf(Math.round(account.getBalance()));
        });

        // {opteco_currency}
        replacerMap.put("opteco_currency", (event) -> {
            // Return the currency symbol
            return plugin.getConfigurationLoader().getString(OptEcoConfiguration.CURRENCY_SYMBOL);
        });

        // {opteco_version}
        replacerMap.put("opteco_version", (event) -> {
            // Return the currency symbol
            return plugin.getDescription().getVersion();
        });
        // Registering above declaration
        replacerMap.forEach((name, replacer) -> {
            PlaceholderAPI.registerPlaceholder(plugin, name, replacer);
        });
    }

}
