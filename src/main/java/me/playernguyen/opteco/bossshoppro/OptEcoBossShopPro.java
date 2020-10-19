package me.playernguyen.opteco.bossshoppro;

import me.playernguyen.opteco.OptEco;
import me.playernguyen.opteco.account.OptEcoCacheAccount;
import org.black_ixx.bossshop.pointsystem.BSPointsPlugin;
import org.bukkit.OfflinePlayer;


public class OptEcoBossShopPro extends BSPointsPlugin {

    private final OptEco optEco;

    public OptEcoBossShopPro(OptEco optEco) {
        super(OptEco.PLUGIN_NAME, "opteco");
        this.optEco = optEco;
    }

    @Override
    public double getPoints(OfflinePlayer offlinePlayer) {
        OptEcoCacheAccount optEcoCacheAccount = optEco.getAccountManager().get(offlinePlayer.getUniqueId());

//        return optEco.getAccountDatabase().getBalance(offlinePlayer.getUniqueId());
        return optEcoCacheAccount.getBalance();
    }

    @Override
    public double setPoints(OfflinePlayer offlinePlayer, double v) {
        if (optEco.getAccountDatabase().setBalance(offlinePlayer.getUniqueId(), v)) {
            return v;
        }
        return 0;
    }

    @Override
    public double takePoints(OfflinePlayer offlinePlayer, double v) {
        if (optEco.getAccountDatabase().takeBalance(offlinePlayer.getUniqueId(), v)) {
            return v;
        }
        return 0;
    }

    @Override
    public double givePoints(OfflinePlayer offlinePlayer, double v) {
        if (optEco.getAccountDatabase().addBalance(offlinePlayer.getUniqueId(), v)) {
            return v;
        }
        return 0;
    }

    @Override
    public boolean usesDoubleValues() {
        return true;
    }
}
