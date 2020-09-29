package me.playernguyen.opteco.logger;

import me.playernguyen.opteco.OptEco;
import me.playernguyen.opteco.OptEcoConfiguration;
import me.playernguyen.opteco.OptEcoImplementation;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class OptEcoDebugger extends OptEcoImplementation implements Debugger {

    private CommandSender sender;

    private String prefix;

    public OptEcoDebugger(OptEco plugin) {
        this.prefix = "[" + plugin.getDescription().getName() + "::Debug]";
        this.sender = getPlugin().getServer().getConsoleSender();
    }

    public void log(String msg) {
        if (getPlugin().getConfigurationLoader().getBool(OptEcoConfiguration.DEBUG)) {
            sender.sendMessage(prefix + " " + msg);
        }
    }

    public void info(String s) {
        log(ChatColor.DARK_GRAY + s);
    }

    public void error(String s) {
        log(ChatColor.RED + s);
    }

    public void warn(String s) {
        log(ChatColor.YELLOW + s);
    }

    @Override
    public void printException(Exception exception) {
        this.error(exception.getClass().getSimpleName() + " -> " + exception.getMessage());
        exception.printStackTrace();
    }
}
