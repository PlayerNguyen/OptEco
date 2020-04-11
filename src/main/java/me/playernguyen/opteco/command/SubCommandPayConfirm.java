package me.playernguyen.opteco.command;

import me.playernguyen.opteco.OptEco;
import me.playernguyen.opteco.OptEcoConfiguration;
import me.playernguyen.opteco.OptEcoLanguage;
import me.playernguyen.opteco.permission.OptEcoPermission;
import me.playernguyen.opteco.transaction.Transaction;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SubCommandPayConfirm extends SubCommand {
    SubCommandPayConfirm(String command, OptEco plugin) {
        super(command, plugin.getLanguageLoader().getLanguage(OptEcoLanguage.COMMAND_DESCRIBE_CONFIRM), plugin);

        addPermissions(OptEcoPermission.EVERYTHING);
        addPermissions(OptEcoPermission.ADMIN);
        addPermissions(OptEcoPermission.USER);

        addPermissions(OptEcoPermission.CONFIRM);
    }

    @Override
    public boolean onPlayerCommand(Player player, ArrayList<String> args) {
        // If don't have any transaction
        if ( ! getPlugin().getTransactionManager().hasTransaction(player.getUniqueId()) ) {
            player.sendMessage(
                    getMessageFormat()
                            .format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.PAY_TRANSACTION_NOT_EXIST))
            );
            return true;
        }

        Transaction transaction = getPlugin().getTransactionManager().getTransaction(player.getUniqueId());
        // Call accept method
        if (transaction.confirm()) {
            Player tranSender = Bukkit.getPlayer(transaction.getPlayer());
            Player tranReceiver = Bukkit.getPlayer(transaction.getTarget());
            if (tranReceiver != null) {
                tranSender.sendMessage(
                        getMessageFormat().format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.PAY_SUCCESS))
                                .replace("%who%", tranReceiver.getName())
                                .replace("%value%", getMessageFormat().numberFormat(transaction.getAmount()))
                                .replace("%currency%", getPlugin().getConfigurationLoader()
                                        .getString(OptEcoConfiguration.CURRENCY_SYMBOL)
                                )
                );
            }
            if (tranReceiver != null) {
                tranReceiver.sendMessage(
                        getMessageFormat().format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.PAY_SUCCESS_TARGET))
                                .replace("%who%", tranSender.getName())
                                .replace("%value%", getMessageFormat().numberFormat(transaction.getAmount()))
                                .replace("%currency%", getPlugin().getConfigurationLoader()
                                        .getString(OptEcoConfiguration.CURRENCY_SYMBOL)
                                )
                );
            }
        } else {
            Player tranSender = Bukkit.getPlayer(transaction.getPlayer());
            if (tranSender != null) {
                tranSender.sendMessage(
                        getMessageFormat().format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.PAY_FAILED))
                );
            }
        }

        return true;
    }

    @Override
    public boolean onConsoleCommand(CommandSender sender, ArrayList<String> args) {
        sender.sendMessage(
                getMessageFormat().format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.FOR_PLAYER_COMMAND))
        );
        return true;
    }

    @Override
    public boolean onRemoteConsoleCommand(RemoteConsoleCommandSender sender, ArrayList<String> args) {
        sender.sendMessage(
                getMessageFormat().format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.FOR_PLAYER_COMMAND))
        );
        return true;
    }

    @Override
    public List<String> onTab(CommandSender commandSender, ArrayList<String> args) {
        return null;
    }
}
