package me.playernguyen.opteco.command;

import me.playernguyen.opteco.OptEcoLanguage;
import me.playernguyen.opteco.permission.OptEcoPermission;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class OptEcoCommand extends AbstractCommand {

    public OptEcoCommand () {
        for (OptEcoPermission perm :
                OptEcoPermission.values()) {
            addPermissions(perm);
        }
        addSubCommand(new SubCommandAdd("add", getPlugin()));
        addSubCommand(new SubCommandTake("take", getPlugin()));
        addSubCommand(new SubCommandSet("set", getPlugin()));
        addSubCommand(new SubCommandCheck("check", getPlugin()));
        addSubCommand(new SubCommandMe("me", getPlugin()));
        addSubCommand(new SubCommandPay("pay", getPlugin()));
        addSubCommand(new SubCommandPayConfirm("confirm", getPlugin()));
        addSubCommand(new SubCommandPayCancel("cancel", getPlugin()));
        addSubCommand(new SubCommandReload("reload", getPlugin()));
    }

    @Override
    public boolean onPlayerCommand(Player player, Command command, String s, String[] args) {
        return execute(player, command, s, args);
    }

    @Override
    public boolean onConsoleCommand(ConsoleCommandSender sender, Command command, String s, String[] args) {
        return execute(sender, command, s, args);
    }

    private boolean execute (CommandSender sender, Command command, String s, String[] args) {
        if (args.length < 1) {
            getMessageFormat().sendCuteList(sender, getSubAsHelp(sender), ChatColor.GRAY);
            return true;
        }

        String sub = args[0];
        if (!getSubAsString().contains(sub)) {
            sender.sendMessage(getMessageFormat().format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.COMMAND_NOT_FOUND)));
            getMessageFormat().sendCuteList(sender, getSubAsHelp(sender), ChatColor.GRAY);
            return true;
        }
        SubCommand subCommand = getSubCommand(sub);
        subCommand.onCommand(sender, separator(args, 0));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if (strings.length < 2) {
            ArrayList<String> allowCommand = new ArrayList<>();
            for (SubCommand subCommand: getSubCommands()) {
                if (subCommand.checkPermission(commandSender)) allowCommand.add(subCommand.getCommand());
            }
            return allowCommand;
        } else {

            SubCommand subCommand = getSubCommand(strings[0]);
            if (subCommand == null) return null;
            else return subCommand.onTabComplete(commandSender, separator(strings, 1));
        }
    }


}
