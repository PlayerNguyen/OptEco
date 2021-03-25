package me.playernguyen.opteco.schedule;

import me.playernguyen.opteco.configuration.OptEcoConfiguration;
import me.playernguyen.opteco.language.OptEcoLanguage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TransactionCountRunnable extends OptEcoRunnable {

    private final UUID player;
    private final UUID target;
    private int ticker;

    public TransactionCountRunnable(UUID player, UUID target) {
        this.ticker
                = getInstance().getConfigurationLoader().getInt(OptEcoConfiguration.PAYMENT_CONFIRM);
        this.player = player;
        this.target = target;
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
        ticker--;

        if (getInstance().getConfigurationLoader().getBool(OptEcoConfiguration.COUNTDOWN_ENABLE)) {
            Player pla = Bukkit.getPlayer(getPlayer());
            if (pla != null) {
                if (getInstance().getConfigurationLoader().getString(OptEcoConfiguration.COUNTDOWN_TYPE)
                        .equalsIgnoreCase("message")) {
                    pla.sendMessage(
                            getInstance().getLanguageLoader().getLanguage(OptEcoLanguage.COUNTDOWN_FORMAT)
                                    .replace("%second%", String.valueOf(ticker))
                    );
                } else if (getInstance().getConfigurationLoader().getString(OptEcoConfiguration.COUNTDOWN_TYPE)
                        .equalsIgnoreCase("title")) {
                    pla.sendTitle(
                            getInstance().getLanguageLoader().getLanguage(OptEcoLanguage.COUNTDOWN_FORMAT)
                                    .replace("%second%", String.valueOf(ticker)),
                            ""
                    );
                }
            }
        }

        // <= 0 for not being crashed
        if (getTicker() <= 0) {
            getInstance().getTransactionManager().getTransaction(getPlayer()).cancel();
            Player player = Bukkit.getPlayer(getPlayer());
            if (player != null) {
                player.sendMessage(
                        getInstance().getMessageFormat()
                                .format(getInstance().getLanguageLoader().getLanguage(OptEcoLanguage.PAY_OUT_OF_TIME_CONFIRM))
                );
            } else {
                getInstance().getLogger().severe("Cancel transaction because player are not online!");
            }
            cancel();
        }
    }
}
