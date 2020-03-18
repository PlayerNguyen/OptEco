package me.playernguyen.logger;

import me.playernguyen.OptEco;
import me.playernguyen.OptEcoConfiguration;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class OptEcoDebugger implements Debugger {

    private OptEco plugin;
    private CommandSender sender;

    private String prefix;

    public OptEcoDebugger (OptEco plugin) {
        this.plugin = plugin;
        this.prefix = "[" + plugin.getDescription().getName() + "::Debug]";
        this.sender = getPlugin().getServer().getConsoleSender();
    }

    public OptEco getPlugin() {
        return plugin;
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
