package me.playernguyen.account;

import me.playernguyen.OptEco;
import me.playernguyen.event.OptEcoPlayerPendingEvent;
import me.playernguyen.event.OptEcoPlayerReceivedEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class Transaction {

    private final long DELAY = 20L;
    private final long PERIOD = 20L;

    private final OptEco plugin;
    private final UUID player;
    private final UUID target;
    private final Double amount;
    private final BukkitRunnable runnable;

    public Transaction (OptEco plugin, Player player, Player target, Double amount, BukkitRunnable runnable) {
        this.plugin = plugin;
        this.player = player.getUniqueId();
        this.target = target.getUniqueId();
        this.amount = amount;
        this.runnable = runnable;

        this.runnable.runTaskTimer(getPlugin(), this.DELAY, this.PERIOD);

        OptEcoPlayerPendingEvent e =
                new OptEcoPlayerPendingEvent(getPlayer(), this);
        Bukkit.getServer().getPluginManager().callEvent(e);
    }

    public Player getTarget() {
        return Bukkit.getPlayer(target);
    }

    public OptEco getPlugin() {
        return plugin;
    }

    public BukkitRunnable getRunnable() {
        return runnable;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(player);
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
        getPlugin().getAccountLoader().takeBalance(getPlayer(), getAmount())
        && getPlugin().getAccountLoader().addBalance(getTarget(), getAmount())
        && this.clean();
    }

    public boolean cancel() {
        return this.clean();
    }

    public boolean clean() {
        // Cancel runnable
        this.runnable.cancel();
        // Trying to know what the f memory leak?
        return getPlugin().getTransactionManager().removeTransaction(getPlayer());
    }
}
