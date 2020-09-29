package me.playernguyen.opteco.command;

import me.playernguyen.opteco.manager.ManagerSet;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;

import java.util.HashSet;

public class CommandManager extends ManagerSet<OptEcoAbstractCommand> {

    public CommandManager() {
        super(new HashSet<>());
    }

    @Override
    public void add(OptEcoAbstractCommand item) {
        // Still add
        super.add(item);

        // Register new one
        PluginCommand pluginCommand = Bukkit.getServer().getPluginCommand(item.getCommand());
        if (pluginCommand == null) {
            throw new IllegalStateException(String.format(
                    "Developer: You must register %s command in plugin.yml",
                    item.getCommand()
            ));
        }
    }
}
