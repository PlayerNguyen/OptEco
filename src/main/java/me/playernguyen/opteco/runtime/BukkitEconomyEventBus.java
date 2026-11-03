package me.playernguyen.opteco.runtime;

import me.playernguyen.opteco.core.model.BalanceChange;
import me.playernguyen.opteco.core.model.Transaction;
import me.playernguyen.opteco.core.model.TransactionState;
import me.playernguyen.opteco.core.port.EconomyEventBus;
import me.playernguyen.opteco.event.OptEcoPointChangedEvent;
import me.playernguyen.opteco.event.OptEcoTransactionEvent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class BukkitEconomyEventBus implements EconomyEventBus {

    private final Plugin plugin;

    public BukkitEconomyEventBus(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void balanceChanged(BalanceChange change) {
        runMain(() -> Bukkit.getPluginManager().callEvent(new OptEcoPointChangedEvent(change)));
    }

    @Override
    public void transactionChanged(Transaction transaction, TransactionState previousState) {
        runMain(() -> Bukkit.getPluginManager().callEvent(new OptEcoTransactionEvent(transaction, previousState)));
    }

    private void runMain(Runnable runnable) {
        if (!plugin.isEnabled()) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            try {
                Bukkit.getScheduler().runTask(plugin, runnable);
            } catch (IllegalStateException ignored) {
                // Plugin is disabling.
            }
        }
    }
}
