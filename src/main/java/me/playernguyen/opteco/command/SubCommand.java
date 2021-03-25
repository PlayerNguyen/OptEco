package me.playernguyen.opteco.command;

import me.playernguyen.opteco.OptEco;
import me.playernguyen.opteco.language.OptEcoLanguage;
import me.playernguyen.opteco.utils.MessageFormat;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public abstract class SubCommand extends AbstractPermission {

    private String command;
    private String description;
    @Deprecated
    private String args;
    private MessageFormat messageFormat;
    private ArrayList<CommandArguments> arguments = new ArrayList<>();


    @Deprecated
    SubCommand(String command, String description, String args, OptEco plugin) {
        this.command = command;
        this.description = description;
        this.args = args;
        this.messageFormat = getPlugin().getMessageFormat();
    }

    SubCommand(String command, String description, OptEco plugin) {
        this.command = command;
        this.description = description;
        this.messageFormat = getPlugin().getMessageFormat();
    }

    public MessageFormat getMessageFormat() {
        return messageFormat;
    }

    public String getCommand() {
        return command;
    }

    public String getDescription() {
        return description;
    }

    @Deprecated
    public String getArgs() {
        return args;
    }

    public String getHelp() {
        return ChatColor.GOLD + this.command + " " + ChatColor.GRAY + this.argumentsAsString() + ChatColor.GOLD + ": " + ChatColor.GREEN + this.description;
    }

    public ArrayList<CommandArguments> getArguments() {
        return arguments;
    }

    public void addArgument(CommandArguments commandArguments) {
        this.getArguments().add(commandArguments);
    }

    public String argumentsAsString() {
        StringBuilder _builder = new StringBuilder();
        for (CommandArguments args :
                getArguments()) {
            _builder.append(args.asString(getPlugin()));
            if (!args.equals(getArguments().get(getArguments().size() - 1))) _builder.append(" ");
        }
        return _builder.toString();
    }

    public boolean onCommand(CommandSender commandSender, ArrayList<String> args) {
        this.getPlugin().getDebugger().info("Sender " + commandSender.getName() + " execute sub-command " + this.getCommand());
        if (!checkPermission(commandSender)) {
            commandSender.sendMessage(
                    getMessageFormat()
                            .format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.NO_PERMISSION))
            );
            return true;
        }
        // With player execute
        if (commandSender instanceof Player)
            return onPlayerCommand((Player) commandSender, args);
            // With console execute
        else if (commandSender instanceof ConsoleCommandSender)
            return onConsoleCommand(commandSender, args);
            // With remote console
        else if (commandSender instanceof RemoteConsoleCommandSender)
            return onRemoteConsoleCommand((RemoteConsoleCommandSender) commandSender, args);

        return true;
    }

    public List<String> onTabComplete(CommandSender commandSender, ArrayList<String> args) {
        if (!checkPermission(commandSender)) {
            return null;
        }
        return onTab(commandSender, args);
    }

    public abstract boolean onPlayerCommand(Player player, ArrayList<String> args);

    public abstract boolean onConsoleCommand(CommandSender sender, ArrayList<String> args);

    public abstract boolean onRemoteConsoleCommand(RemoteConsoleCommandSender sender, ArrayList<String> args);

    public abstract List<String> onTab(CommandSender commandSender, ArrayList<String> args);
}
