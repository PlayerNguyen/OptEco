package me.playernguyen.opteco.runtime;

import me.playernguyen.opteco.core.model.BalanceChange;
import me.playernguyen.opteco.core.model.Transaction;
import me.playernguyen.opteco.core.model.TransactionState;
import me.playernguyen.opteco.core.port.EconomyEventBus;
import me.playernguyen.opteco.event.OptEcoPointChangedEvent;
import me.playernguyen.opteco.event.OptEcoTransactionEvent;
import me.playernguyen.opteco.util.ThreadUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Bukkit-backed {@link EconomyEventBus} that re-raises core model changes as
 * plugin events.
 *
 * <p>Core services are not allowed to import Bukkit, so they publish here and
 * this bridge calls {@link org.bukkit.plugin.PluginManager#callEvent}. Events
 * always fire on the main thread: the call is executed inline when already on it
 * and scheduled otherwise. Events are dropped once the plugin is disabled, which
 * avoids scheduler errors during shutdown. Holds no mutable state and is
 * safe to call from any thread.
 */
public final class BukkitEconomyEventBus implements EconomyEventBus {

    /** Plugin used for scheduler access and enablement checks. */
    private final Plugin plugin;

    /**
     * Creates the bridge.
     *
     * @param plugin the owning plugin, used for the main-thread scheduler
     */
    public BukkitEconomyEventBus(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Fires an {@link OptEcoPointChangedEvent} for a balance mutation.
     *
     * @param change the before/after snapshot of the affected account
     */
    @Override
    public void balanceChanged(@NotNull BalanceChange change) {
        ThreadUtils.run(plugin, () -> Bukkit.getPluginManager().callEvent(new OptEcoPointChangedEvent(change)));
    }

    /**
     * Fires an {@link OptEcoTransactionEvent} for a payment state transition.
     *
     * @param transaction the transaction in its new state
     * @param previousState the state it had before the transition
     */
    @Override
    public void transactionChanged(@NotNull Transaction transaction, @Nullable TransactionState previousState) {
        ThreadUtils.run(plugin, () -> Bukkit.getPluginManager()
                .callEvent(new OptEcoTransactionEvent(transaction, previousState)));
    }
}
