package me.playernguyen.schedule;

import me.playernguyen.OptEco;
import me.playernguyen.OptEcoConfiguration;
import me.playernguyen.OptEcoLanguage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class OnTransactionSchedule extends BukkitRunnable {

    private UUID player;
    private UUID target;
    private OptEco plugin;
    private int ticker;

    public OnTransactionSchedule (OptEco plugin, UUID player, UUID target) {
        this.plugin = plugin;
        this.ticker
                = getPlugin().getConfigurationLoader().getInt(OptEcoConfiguration.PAYMENT_CONFIRM);
        this.player = player;
        this.target = target;
    }

    public OptEco getPlugin() {
        return plugin;
    }

    public UUID getPlayer() {
        return player;
    }

    public UUID getTarget() {
        return target;
    }

    public int getTicker() {
        return ticker;
    }

    @Override
    public void run() {
        ticker --;


        if (getPlugin().getConfigurationLoader().getBool(OptEcoConfiguration.COUNTDOWN_ENABLE)) {
            Player pla = Bukkit.getPlayer(getPlayer());
            if (pla != null) {
                if (getPlugin().getConfigurationLoader().getString(OptEcoConfiguration.COUNTDOWN_TYPE)
                        .equalsIgnoreCase("message")) {
                    pla.sendMessage(
                            getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.COUNTDOWN_FORMAT)
                                    .replace("%second%", String.valueOf(ticker))
                    );
                }
            }
        }

        // <= 0 for not being crashed
        if (getTicker() <= 0) {
            getPlugin().getTransactionManager().getTransaction(getPlayer()).cancel();
            Player player = Bukkit.getPlayer(getPlayer());
            if (player != null) {
                player.sendMessage(
                        getPlugin().getMessageFormat()
                                .format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.PAY_OUT_OF_TIME_CONFIRM))
                );
            } else {
                getPlugin().getLogger().severe("Cancel transaction because player are not online!");
                cancel();
            }

            cancel();
        }
    }
}
