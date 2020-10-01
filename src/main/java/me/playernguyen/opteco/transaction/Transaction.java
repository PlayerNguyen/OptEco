package me.playernguyen.opteco.transaction;

import me.playernguyen.opteco.OptEcoImplementation;
import me.playernguyen.opteco.event.OptEcoPlayerPendingEvent;
import me.playernguyen.opteco.event.OptEcoPlayerReceivedEvent;
import me.playernguyen.opteco.event.OptEcoTransactionChangeStateEvent;
import me.playernguyen.opteco.utils.RandomString;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class Transaction extends OptEcoImplementation {

    private static final long DELAY = 20L;
    private static final long PERIOD = 20L;
    private static final int ID_RANDOM_LENGTH = 16;

    private final UUID player;
    private final UUID target;
    private final Double amount;
    private final BukkitRunnable runnable;
    private TransactionState state = TransactionState.PENDING;
    private final String id;
    private final long time;

    public Transaction(UUID player, UUID target, Double amount, BukkitRunnable runnable) {
        this.player = player;
        this.target = target;
        this.amount = amount;
        this.runnable = runnable;
        // Apply time
        this.time = System.currentTimeMillis();
        // Generate random id
        this.id = "i" + RandomString.rand(ID_RANDOM_LENGTH);
        // Init with pending state
        this.setState(TransactionState.PENDING);
        // Call an async task to count down
        this.runnable.runTaskTimerAsynchronously(getPlugin(), DELAY, PERIOD);
        // Call the event
        OptEcoPlayerPendingEvent e = new OptEcoPlayerPendingEvent(getPlayer(), this);
        Bukkit.getServer().getPluginManager().callEvent(e);
        // Debugger log out
        getPlugin().getDebugger().info(String.format("generated the transaction id #%s", getId()));
    }

    public String getId() {
        return id;
    }

    public UUID getTarget() {
        return target;
    }

    public BukkitRunnable getRunnable() {
        return runnable;
    }

    public UUID getPlayer() {
        return player;
    }

    public Double getAmount() {
        return amount;
    }

    public TransactionState getState() {
        return state;
    }

    public long getTime() {
        return time;
    }

    public long getTimeAsSecond() {
        return time / 1000;
    }

    public void setState(TransactionState state) {
        Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            this.state = state;
            // Call the event without async error
            Bukkit.getScheduler().runTask(getPlugin(),
                    () -> Bukkit.getPluginManager().callEvent(new OptEcoTransactionChangeStateEvent(this)));
        });
    }

    /**
     * Confirm the transaction
     *
     * @return Is confirm or not
     * @throws IllegalStateException Throw while the state are not pending
     */
    public boolean confirm() {
        Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            if (!getState().equals(TransactionState.PENDING)) {
                throw new IllegalStateException("Cannot accept non-pending transaction!");
            }
            // Call received listener
            OptEcoPlayerReceivedEvent event = new OptEcoPlayerReceivedEvent(this);
            Bukkit.getScheduler().runTask(getPlugin(), () -> Bukkit.getServer().getPluginManager().callEvent(event));
            // Change state
            this.setState(TransactionState.CONFIRMED);
            // Update the information into database
            this.getTransactionManager().getTransactionStorage().push(this);
        });
        // Return such as ...
        return getPlugin().getAccountDatabase().takeBalance(getPlayer(), getAmount())
                && getPlugin().getAccountDatabase().addBalance(getTarget(), getAmount())
                && this.clean();
    }

    /***
     * Set cancel to the transaction
     * @throws IllegalStateException Throw while the state are not pending
     * @return Is cancel (clean up or not)
     */
    public boolean cancel() {
        Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            if (!getState().equals(TransactionState.PENDING)) {
                throw new IllegalStateException("Cannot cancel non-pending transaction!");
            }
            // Change state
            this.setState(TransactionState.CANCELLED);
            // Update the information into database
            this.getTransactionManager().getTransactionStorage().push(this);
        });
        // Clean
        return this.clean();
    }

    /**
     * Clean up the transaction information
     *
     * @return Is clean up or not
     */
    public boolean clean() {
        // Cancel runnable
        this.runnable.cancel();
        // No memory leak here :D
        return getPlugin().getTransactionManager().removeTransaction(getPlayer());
    }
}
