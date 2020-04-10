package me.playernguyen.opteco.schedule;

import me.playernguyen.opteco.OptEco;
import org.bukkit.scheduler.BukkitRunnable;

public abstract class OptEcoRunnable extends BukkitRunnable {

    public OptEco getInstance() {
        return OptEco.getInstance();
    }

}
