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

    @Override
    public void run() {
        ticker --;

        if (ticker == 0) {
            getPlugin().getTransactionManager().getTransaction(player).cancel();

            Player player = Bukkit.getPlayer(getPlayer());
            if (player != null) {
                player.sendMessage(
                        getPlugin().getMessageFormat()
                                .format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.PAY_OUT_OF_TIME_CONFIRM))
                );
            } else {
                getPlugin().getLogger().severe("Cancel transaction because player not found!");
                cancel();
            }

            cancel();
        }
    }
}
