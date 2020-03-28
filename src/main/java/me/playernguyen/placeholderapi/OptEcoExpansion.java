package me.playernguyen.placeholderapi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.playernguyen.OptEco;
import me.playernguyen.OptEcoConfiguration;
import org.bukkit.entity.Player;

public class OptEcoExpansion extends PlaceholderExpansion {

    private OptEco plugin;

    public OptEcoExpansion(OptEco plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return this.plugin.getName();
    }

    @Override
    public String getAuthor() {
        return this.plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return this.plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player p, String params) {
        if (p == null) {
            return "";
        }
        // %opteco_points%
        if (params.equalsIgnoreCase("points")) {
            return this.plugin.getMessageFormat().numberFormat(this.plugin.getAccountLoader().getBalance(p.getUniqueId()));
        }
        // %opteco_version%
        if (params.equalsIgnoreCase("version")) {
            return this.plugin.getDescription().getVersion();
        }
        // %opteco_currency%
        if (params.equalsIgnoreCase("currency")) {
            return this.plugin.getConfigurationLoader().getString(OptEcoConfiguration.CURRENCY_SYMBOL);
        }
        return null;
    }
}
