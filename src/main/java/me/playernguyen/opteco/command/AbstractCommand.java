package me.playernguyen.opteco.command;

import me.playernguyen.opteco.OptEcoLanguage;
import me.playernguyen.opteco.utils.MessageFormat;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.stream.Collectors;

public abstract class AbstractCommand extends AbstractPermission
        implements TabCompleter, CommandExecutor {

    private final String command;
    private final MessageFormat messageFormat;
    private final ArrayList<SubCommand> subCommands = new ArrayList<>();

    public AbstractCommand (String command) {
        this.command = command;
        this.messageFormat = getPlugin().getMessageFormat();
    }

    public String getCommand() {
        return command;
    }

    public MessageFormat getMessageFormat() {
        return messageFormat;
    }

    public boolean addSubCommand(SubCommand subCommand) {
        return subCommands.add(subCommand);
    }

    public ArrayList<SubCommand> getSubCommands() {
        return subCommands;
    }

    public SubCommand getSubCommand(String name) {
        for (SubCommand s : this.getSubCommands()) {
            if (s.getCommand().equalsIgnoreCase(name)) return s;
        }
        return null;
    }

    public ArrayList<String> getSubAsString() {
        return subCommands.stream().map(
                SubCommand::getCommand).collect(Collectors.toCollection(ArrayList::new)
        );
    }

    public ArrayList<String> getSubAsHelp(CommandSender sender) {
        ArrayList<String> _arr = new ArrayList<>();
        for (SubCommand subCommand: getSubCommands()) {
            if (subCommand.checkPermission(sender))
            {
                _arr.add(subCommand.getHelp());
            }
        }
        return _arr;
    }

    public ArrayList<String> separator(String[] args, int startAt) {
        ArrayList<String> _args = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            if (i > startAt) {
                _args.add(args[i]);
            }
        }
        return _args;
    }


    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!checkPermission(commandSender)) {
            commandSender.sendMessage(
                    getMessageFormat().format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.NO_PERMISSION))
            );
            return true;
        }
        if (commandSender instanceof Player) {
            return onPlayerCommand((Player) commandSender, command, s, strings);
        } else if (commandSender instanceof ConsoleCommandSender) {
            return onConsoleCommand((ConsoleCommandSender) commandSender, command, s, strings);
        } else if (commandSender instanceof RemoteConsoleCommandSender) {
            return onRemoteConsole((RemoteConsoleCommandSender) commandSender, command, s, strings);
        }
        return onAny(commandSender, command, s, strings);
    }


    public abstract boolean onPlayerCommand(Player player, Command command, String s, String[] args);
    public abstract boolean onConsoleCommand(ConsoleCommandSender sender, Command command, String s, String[] args);
    public abstract boolean onRemoteConsole(RemoteConsoleCommandSender sender, Command command, String s, String[] args);
    public abstract boolean onAny(CommandSender sender, Command command, String s, String[] args);
}
