package me.playernguyen.opteco.schedule;

import me.playernguyen.opteco.OptEcoConfiguration;
import me.playernguyen.opteco.event.OptEcoPointChangedEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;

public class PointChangedPerformance extends OptEcoRunnable {

    private final OptEcoPointChangedEvent event;
    private final Player player;
    private double pointCounter;

    public PointChangedPerformance(OptEcoPointChangedEvent event) {
        this.event = event;
        this.pointCounter = event.getOldPoint();
        this.player = Bukkit.getPlayer(event.getWho());

//        if (player == null) {
////            throw new NullPointerException("Player not found: " + event.getWho());
//        }
    }

    @Override
    public void run() {
        if (player == null) {
            this.cancel();
            return;
        }
        // Decrease case
        if (event.getType() == OptEcoPointChangedEvent.ModifyType.DECREASE) {
            double changeValue = Math.sqrt((pointCounter - event.getNewPoint()));
            pointCounter -= changeValue;
            // Send title
            String formattedPoint = new DecimalFormat("#0.00")
                    .format(Math.max(event.getNewPoint(), pointCounter));
            String formattedMinusPoint = new DecimalFormat("#0.00")
                    .format(Math.max((pointCounter - (event.getNewPoint())), 0));
            player.sendTitle(
                    ChatColor.translateAlternateColorCodes('&', String.format("&a- &8%s &a-", formattedPoint)),
                    ChatColor.translateAlternateColorCodes('&', String.format("&c^ &6%s &c^", formattedMinusPoint)),
                    0,
                    getInstance().getConfigurationLoader().getInt(OptEcoConfiguration.TITLE_STAY),
                    getInstance().getConfigurationLoader().getInt(OptEcoConfiguration.TITLE_FADE_OUT)
            );

            // Reach to the bottom
            if (pointCounter <= event.getNewPoint() || changeValue % 1 < 0.0001) {
                this.cancel();
            }
        }
        // Increase case
        if (event.getType() == OptEcoPointChangedEvent.ModifyType.INCREASE) {
            // Count up and push to the counter
            double changeValue = Math.sqrt((event.getNewPoint() - pointCounter));
            pointCounter += changeValue;
            // Send title
            String formattedPoint = new DecimalFormat("#0.00")
                    .format(Math.min(pointCounter, event.getNewPoint()));
            String formattedMinusPoint = new DecimalFormat("#0.00")
                    .format(Math.max((event.getNewPoint() - (pointCounter)), 0));

            player.sendTitle(ChatColor.translateAlternateColorCodes('&', String.format("&a+ &8%s &a+", formattedPoint)),
                    ChatColor.translateAlternateColorCodes('&', String.format("&c^ &6%s &c^", formattedMinusPoint)),
                    0,
                    getInstance().getConfigurationLoader().getInt(OptEcoConfiguration.TITLE_STAY),
                    getInstance().getConfigurationLoader().getInt(OptEcoConfiguration.TITLE_FADE_OUT)
            );
            // Whether the point reach top
            if (pointCounter >= event.getNewPoint() || changeValue % 1 < 0.0001) {
                this.cancel();
            }
        }
    }

}
