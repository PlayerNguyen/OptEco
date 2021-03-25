package me.playernguyen.opteco.command;

import me.playernguyen.opteco.configuration.OptEcoConfiguration;
import me.playernguyen.opteco.language.OptEcoLanguage;
import me.playernguyen.opteco.permission.OptEcoPermission;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class OptEcoCommand extends OptEcoAbstractCommand {

    public OptEcoCommand() {
        super("opteco");
        for (OptEcoPermission perm :
                OptEcoPermission.values()) {
            addPermissions(perm);
        }
        // Append new command
        addSubCommand(new SubCommandAdd("add", getPlugin()));
        addSubCommand(new SubCommandTake("take", getPlugin()));
        addSubCommand(new SubCommandSet("set", getPlugin()));
        addSubCommand(new SubCommandCheck("check", getPlugin()));
        addSubCommand(new SubCommandMe("me", getPlugin()));
        addSubCommand(new SubCommandPay("pay", getPlugin()));
        addSubCommand(new SubCommandPayConfirm("confirm", getPlugin()));
        addSubCommand(new SubCommandPayCancel("cancel", getPlugin()));
        addSubCommand(new SubCommandReload("reload", getPlugin()));
        addSubCommand(new SubCommandTop("top", getPlugin()));
    }

    @Override
    public boolean onPlayerCommand(Player player, Command command, String s, String[] args) {
        return execute(player, command, s, args);
    }

    @Override
    public boolean onConsoleCommand(ConsoleCommandSender sender, Command command, String s, String[] args) {
        return execute(sender, command, s, args);
    }

    @Override
    public boolean onRemoteConsole(RemoteConsoleCommandSender sender, Command command, String s, String[] args) {
        return execute(sender, command, s, args);
    }

    @Override
    public boolean onAny(CommandSender sender, Command command, String s, String[] args) {
        return execute(sender, command, s, args);
    }

    private boolean execute(CommandSender sender, Command command, String s, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.GRAY_BAR));
            getMessageFormat().sendCuteList(sender, getSubAsHelp(sender), ChatColor.GRAY);
            return true;
        }

        String sub = args[0];
        if (!getSubAsString().contains(sub)) {
            sender.sendMessage(
                    getMessageFormat().format(getPlugin()
                            .getLanguageLoader()
                            .getLanguage(OptEcoLanguage.COMMAND_NOT_FOUND
                            )
                    )
            );
            getMessageFormat().sendCuteList(sender, getSubAsHelp(sender), ChatColor.GRAY);
            return true;
        }
        SubCommand subCommand = getSubCommand(sub);
        subCommand.onCommand(sender, separator(args, 0));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender,
                                      @NotNull Command command,
                                      @NotNull String s,
                                      String[] strings) {
        // Whether the tab executor disabled
        if (!getConfigurationLoader().getBool(OptEcoConfiguration.TAB_EXECUTOR)) {
            return null;
        }
        // Whether enabled
        if (strings.length < 2) {
            ArrayList<String> allowCommand = new ArrayList<>();
            for (SubCommand subCommand : getSubCommands()) {
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
