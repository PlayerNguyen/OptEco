package me.playernguyen.account;

import me.playernguyen.OptEco;
import me.playernguyen.event.OptEcoPlayerPendingEvent;
import me.playernguyen.event.OptEcoPlayerReceivedEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class Transaction {

    private final long DELAY = 20L;
    private final long PERIOD = 20L;

    private final OptEco plugin;
    private final Player player;
    private final Player target;
    private final Double amount;
    private final BukkitRunnable runnable;

    public Transaction (OptEco plugin, Player player, Player target, Double amount, BukkitRunnable runnable) {
        this.plugin = plugin;
        this.player = player;
        this.target = target;
        this.amount = amount;
        this.runnable = runnable;

        this.runnable.runTaskTimer(getPlugin(), this.DELAY, this.PERIOD);

        OptEcoPlayerPendingEvent e =
                new OptEcoPlayerPendingEvent(getPlayer(), this);
        Bukkit.getServer().getPluginManager().callEvent(e);
    }

    public Player getTarget() {
        return target;
    }

    public OptEco getPlugin() {
        return plugin;
    }

    public BukkitRunnable getRunnable() {
        return runnable;
    }

    public Player getPlayer() {
        return player;
    }

    public Double getAmount() {
        return amount;
    }

    public boolean confirm() {
        // Call event listener
        OptEcoPlayerReceivedEvent event =
                new OptEcoPlayerReceivedEvent(getPlayer(), getTarget(), getAmount());
        Bukkit.getServer().getPluginManager().callEvent(event);
        // Return such as ...
        return
        getPlugin().getAccountLoader().takeBalance(player, getAmount())
        && getPlugin().getAccountLoader().addBalance(target, getAmount())
        && this.clean();
    }

    public boolean cancel() {
        return this.clean();
    }

    public boolean clean() {
        this.runnable.cancel();
        return getPlugin().getTransactionManager().removeTransaction(getPlayer());
    }
}
