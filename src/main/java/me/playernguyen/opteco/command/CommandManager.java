package me.playernguyen.opteco.command;

import me.playernguyen.opteco.OptEco;
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

        OptEco.getInstance().getDebugger().info("Trying to register command: " + item.getCommand());
        // Register new one
        PluginCommand pluginCommand = Bukkit.getServer().getPluginCommand(item.getCommand());
        if (pluginCommand == null) {
            OptEco.getInstance().getDebugger().error("Failed to register command: " + item.getCommand());
            throw new IllegalStateException(String.format(
                    "Developer: You must register %s command in plugin.yml",
                    item.getCommand()
            ));
        }
        pluginCommand.setExecutor(item);
        OptEco.getInstance().getDebugger().info("Succeed to register command: " + item.getCommand());
    }
}
