package me.playernguyen.command;

import me.playernguyen.OptEco;
import me.playernguyen.OptEcoConfiguration;
import me.playernguyen.OptEcoLanguage;
import me.playernguyen.permission.OptEcoPermission;
import me.playernguyen.utils.ValidationChecker;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SubCommandTake extends SubCommand {

    SubCommandTake(String command, OptEco plugin) {
        super(command, plugin.getLanguageLoader().getLanguage(OptEcoLanguage.COMMAND_DESCRIBE_TAKE), plugin);

        addArgument(CommandArguments.PLAYER);
        addArgument(CommandArguments.AMOUNT);

        addPermissions(OptEcoPermission.EVERYTHING);
        addPermissions(OptEcoPermission.ADMIN);
        addPermissions(OptEcoPermission.TAKE);
    }

    @Override
    public boolean onPlayerCommand(Player player, ArrayList<String> args) {
        return this.execute(player, args);
    }

    @Override
    public boolean onConsoleCommand(CommandSender sender, ArrayList<String> args) {
        return this.execute(sender, args);
    }

    private boolean execute (CommandSender sender, ArrayList<String> args) {

        if (args.size() < 2) {
            sender.sendMessage(getMessageFormat().format(getHelp()));
            return true;
        }

        String _target = args.get(0); Player target =  Bukkit.getServer().getPlayerExact(_target);
        String _value = args.get(1);
        if (target == null) {
            sender.sendMessage(
                    getMessageFormat()
                    .format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.VAR_PLAYER_NOT_FOUND))
                    .replace("%who%", _target)
            );
            return true;
        }
        if (!ValidationChecker.isNumber(_value)) {
            sender.sendMessage(
                    getMessageFormat()
                    .format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.VAR_NOT_A_NUMBER))
                    .replace("%value%", _value)
            );
            return true;
        }
        double value = Double.parseDouble(_value);
        if (value < 0) {
            sender.sendMessage(
                    getMessageFormat()
                            .format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.VALUE_CANNOT_BE_NEGATIVE))
            );
            return true;
        }

        if (getPlugin().getAccountLoader().takeBalance(target, value)) {
            sender.sendMessage(
                    getMessageFormat()
                    .format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.TAKE_SUCCESS))
                            .replace("%value%", _value)
                            .replace("%who%", target.getName())
                            .replace("%currency%", getPlugin().getConfigurationLoader().getString(OptEcoConfiguration.CURRENCY_SYMBOL))
            );
        } else {
            sender.sendMessage(
                    getMessageFormat()
                            .format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.TAKE_FAIL))
                            .replace("%value%", _value)
                            .replace("%who%", target.getName())
                            .replace("%currency%", getPlugin().getConfigurationLoader().getString(OptEcoConfiguration.CURRENCY_SYMBOL))
            );
        }

        return true;
    }

    @Override
    public List<String> onTab(CommandSender commandSender, ArrayList<String> args) {
        if (args.size() <= 0) {
            ArrayList<String> _players = new ArrayList<>();
            for (Player player: Bukkit.getServer().getOnlinePlayers()) {
                _players.add(player.getName());
            }
            return _players;
        }
        return new ArrayList<>();
    }
}
