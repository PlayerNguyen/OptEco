package me.playernguyen.command;

import me.playernguyen.OptEco;
import me.playernguyen.OptEcoLanguage;
import me.playernguyen.utils.MessageFormat;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.stream.Collectors;

public abstract class AbstractCommand extends AbstractPermission implements TabCompleter, CommandExecutor {

    private MessageFormat messageFormat;

    public AbstractCommand (OptEco plugin) {
        super(plugin);
        this.messageFormat = new MessageFormat(getPlugin());
    }

    public MessageFormat getMessageFormat() {
        return messageFormat;
    }

    private ArrayList<SubCommand> subCommands = new ArrayList<>();

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
        }
        return onConsoleCommand((ConsoleCommandSender) commandSender, command, s, strings);
    }


    public abstract boolean onPlayerCommand(Player player, Command command, String s, String[] args);
    public abstract boolean onConsoleCommand(ConsoleCommandSender sender, Command command, String s, String[] args);
}
