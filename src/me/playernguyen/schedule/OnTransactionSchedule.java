package me.playernguyen.schedule;

import me.playernguyen.OptEco;
import me.playernguyen.OptEcoConfiguration;
import me.playernguyen.OptEcoLanguage;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class OnTransactionSchedule extends BukkitRunnable {

    private Player player;
    private Player target;
    private OptEco plugin;
    private int ticker;

    public OnTransactionSchedule (OptEco plugin, Player player, Player target) {
        this.plugin = plugin;
        this.ticker
                = getPlugin().getConfigurationLoader().getInt(OptEcoConfiguration.PAYMENT_CONFIRM);
        this.player = player;
        this.target = target;
    }

    public OptEco getPlugin() {
        return plugin;
    }

    public Player getPlayer() {
        return player;
    }

    public Player getTarget() {
        return target;
    }

    @Override
    public void run() {
        ticker --;

        if (ticker == 0) {
            getPlugin().getTransactionManager().getTransaction(player).cancel();

            player.sendMessage(
                    getPlugin().getMessageFormat()
                            .format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.PAY_OUT_OF_TIME_CONFIRM))
            );
            cancel();
        }
    }
}
