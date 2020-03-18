package me.playernguyen.api;

import me.playernguyen.OptEco;
import org.bukkit.entity.Player;

public class OptEcoAPI {

    private final Player player;

    public OptEcoAPI (Player player) {
        this.player = player;
    }

    private OptEco getPlugin() {
        return OptEco.getPlugin();
    }

    public Player getPlayer() {
        return player;
    }

    public boolean hasAccountInDatabase() {
        return getPlugin().getAccountLoader().hasAccount(getPlayer());
    }

    public double getBalance() {
        return getPlugin().getAccountLoader().getBalance(getPlayer());
    }

    public boolean setBalance(double amount) {
        return getPlugin().getAccountLoader().setBalance(getPlayer(), amount);
    }

    public boolean resetBalance() {
        return this.setBalance(0);
    }

}
