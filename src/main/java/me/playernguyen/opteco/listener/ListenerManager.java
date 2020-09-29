package me.playernguyen.opteco.listener;

import me.playernguyen.opteco.OptEco;
import me.playernguyen.opteco.manager.ManagerSet;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;

import java.util.HashSet;

public class ListenerManager extends ManagerSet<OptEcoListener> {

    private final OptEco optEco;

    public ListenerManager(OptEco optEco) {
        super(new HashSet<>());
        this.optEco = optEco;
    }


    @Override
    public void add(OptEcoListener item) {
        // Still add item inside
        super.add(item);

        // Register item
        Bukkit.getServer().getPluginManager().registerEvents(item, optEco);
    }

    public void unregisterAll() {
        for (OptEcoListener optEcoListener : getContainer()) {
            HandlerList.unregisterAll(optEcoListener);
        }
    }
}
