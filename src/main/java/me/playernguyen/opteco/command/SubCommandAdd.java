package me.playernguyen.opteco.command;

import me.playernguyen.opteco.OptEco;
import me.playernguyen.opteco.configuration.OptEcoConfiguration;
import me.playernguyen.opteco.language.OptEcoLanguage;
import me.playernguyen.opteco.permission.OptEcoPermission;
import me.playernguyen.opteco.utils.MathUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SubCommandAdd extends SubCommand {


    SubCommandAdd(String s, OptEco plugin) {
        super(s, plugin.getLanguageLoader().getLanguage(OptEcoLanguage.COMMAND_DESCRIBE_ADD), plugin);

        addArgument(CommandArguments.PLAYER);
        addArgument(CommandArguments.AMOUNT);

        addPermissions(OptEcoPermission.ADMIN);
        addPermissions(OptEcoPermission.EVERYTHING);
        addPermissions(OptEcoPermission.ADD);
    }

    @Override
    public boolean onPlayerCommand(Player player, ArrayList<String> args) {
        return this.execute(player, args);
    }

    @Override
    public boolean onConsoleCommand(CommandSender sender, ArrayList<String> args) {
        return this.execute(sender, args);
    }

    @Override
    public boolean onRemoteConsoleCommand(RemoteConsoleCommandSender sender, ArrayList<String> args) {
        return this.execute(sender, args);
    }

    private boolean execute(CommandSender sender, ArrayList<String> args) {
        if (args.size() < 2) {
            sender.sendMessage(getMessageFormat().format(getHelp()));
            return true;
        }
        String _target = args.get(0);
        OfflinePlayer target = Bukkit.getOfflinePlayer(_target);
        String _value = args.get(1);
        // If this offline player has never played before and is not online
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(
                    getMessageFormat()
                            .format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.VAR_PLAYER_NOT_FOUND))
                            .replace("%who%", _target)
            );
            return true;
        }
        if (MathUtils.isNotNumber(_value)) {
            sender.sendMessage(getMessageFormat().format(
                    getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.VAR_NOT_A_NUMBER)
                            .replace("%value%", _value)
            ));
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
        if (this.getPlugin().getAccountDatabase().addBalance(target.getUniqueId(), value)) {
            // Send success message
            sender.sendMessage(
                    getMessageFormat().format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.COMMAND_SUCCEEDED_ADD))
                            .replace("%value%", getMessageFormat().numberFormat(value))
                            .replace("%currency%", getPlugin().getConfigurationLoader().getString(OptEcoConfiguration.CURRENCY_SYMBOL))
                            .replace("%who%", args.get(0))
            );
            // Refresh the cache account
            if (target.isOnline())
                this.getAccountManager().refresh(target.getUniqueId());
        } else {
            sender.sendMessage(
                    getMessageFormat().format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.COMMAND_FAILED_ADD))
                            .replace("%value%", getMessageFormat().numberFormat(value))
                            .replace("%currency%", getPlugin().getConfigurationLoader().getString(OptEcoConfiguration.CURRENCY_SYMBOL))
                            .replace("%who%", args.get(0))
            );
        }
        return true;
    }

    @Override
    public List<String> onTab(CommandSender commandSender, ArrayList<String> args) {
        if (args.size() <= 0) {
            ArrayList<String> _players = new ArrayList<>();
            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                _players.add(player.getName());
            }
            return _players;
        }
        return new ArrayList<>();
    }
}
