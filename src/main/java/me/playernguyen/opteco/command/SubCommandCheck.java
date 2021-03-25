package me.playernguyen.opteco.command;

import me.playernguyen.opteco.OptEco;
import me.playernguyen.opteco.configuration.OptEcoConfiguration;
import me.playernguyen.opteco.language.OptEcoLanguage;
import me.playernguyen.opteco.account.OptEcoCacheAccount;
import me.playernguyen.opteco.permission.OptEcoPermission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SubCommandCheck extends SubCommand {

    public SubCommandCheck(String command, OptEco optEco) {
        super(command, optEco.getLanguageLoader().getLanguage(OptEcoLanguage.COMMAND_DESCRIBE_CHECK), optEco);

        addArgument(CommandArguments.PLAYER);

        addPermissions(OptEcoPermission.EVERYTHING);
        addPermissions(OptEcoPermission.CHECK);
        addPermissions(OptEcoPermission.ADMIN);
    }

    @Override
    public boolean onPlayerCommand(Player player, ArrayList<String> args) {
        if (args.size() < 1) {
            OptEcoCacheAccount optEcoCacheAccount = getAccountManager().get(player.getUniqueId());
            double val = optEcoCacheAccount.getBalance();
            player.sendMessage(
                    getMessageFormat().format(
                            getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.CHECK_SELF)
                                    .replace("%value%", getMessageFormat().numberFormat(val))
                                    .replace("%currency%", getPlugin().getConfigurationLoader().getString(OptEcoConfiguration.CURRENCY_SYMBOL))
                    )
            );
            return true;
        }
        return this.exec(player, args);
    }

    @Override
    public boolean onConsoleCommand(CommandSender sender, ArrayList<String> args) {
        return this.exec(sender, args);
    }

    @Override
    public boolean onRemoteConsoleCommand(RemoteConsoleCommandSender sender, ArrayList<String> args) {
        return this.exec(sender, args);
    }

    private boolean exec(CommandSender sender, ArrayList<String> args) {
        // The argument are missing
        if (args.size() < 1) {
            sender.sendMessage(getMessageFormat().format(getHelp()));
            return true;
        }
        String _target = args.get(0);
        OfflinePlayer target = Bukkit.getOfflinePlayer(_target);
        // If this offline player has never played before
        if (!target.hasPlayedBefore()) {
            sender.sendMessage(
                    getMessageFormat()
                            .format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.VAR_PLAYER_NOT_FOUND))
                            .replace("%who%", _target)
            );
            return true;
        }

        double val;
        // Player is online
        if (target.isOnline()) {
            OptEcoCacheAccount optEcoCacheAccount = getPlugin().getAccountManager().get(target.getUniqueId());
            if (optEcoCacheAccount == null)
                throw new NullPointerException("Cache player not found: " + target.getUniqueId());
            val = optEcoCacheAccount.getBalance();
        }
        //Player is not online
        else {
            val = getPlugin().getAccountDatabase().getBalance(target.getUniqueId());
        }
        // Send the message to check
        sender.sendMessage(
                getMessageFormat().format(
                        getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.CHECK_ANOTHER)
                                .replace("%who%", _target)
                                .replace("%value%", getMessageFormat().numberFormat(val))
                                .replace("%currency%", getPlugin().getConfigurationLoader()
                                        .getString(OptEcoConfiguration.CURRENCY_SYMBOL)
                                )
                )
        );
        return true;
    }

    @Override
    public List<String> onTab(CommandSender commandSender, ArrayList<String> args) {
        return null;
    }
}
