package me.playernguyen.command;

import me.playernguyen.OptEco;
import me.playernguyen.OptEcoConfiguration;
import me.playernguyen.OptEcoLanguage;
import me.playernguyen.account.Transaction;
import me.playernguyen.permission.OptEcoPermission;
import org.bukkit.command.CommandSender;
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
        if ( ! getPlugin().getTransactionManager().hasTransaction(player) ) {
            player.sendMessage(
                    getMessageFormat()
                            .format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.PAY_TRANSACTION_NOT_EXIST))
            );
            return true;
        }

        Transaction transaction = getPlugin().getTransactionManager().getTransaction(player);
        // Call accept method
        if (transaction.confirm()) {
            transaction.getPlayer().sendMessage(
                    getMessageFormat().format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.PAY_SUCCESS))
                    .replace("%who%", transaction.getTarget().getName())
                    .replace("%value%", getMessageFormat().numberFormat(transaction.getAmount()))
                    .replace("%currency%", getPlugin().getConfigurationLoader()
                            .getString(OptEcoConfiguration.CURRENCY_SYMBOL)
                    )
            );
            transaction.getTarget().sendMessage(
                    getMessageFormat().format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.PAY_SUCCESS_TARGET))
                            .replace("%who%", transaction.getPlayer().getName())
                            .replace("%value%", getMessageFormat().numberFormat(transaction.getAmount()))
                            .replace("%currency%", getPlugin().getConfigurationLoader()
                                    .getString(OptEcoConfiguration.CURRENCY_SYMBOL)
                            )
            );
        } else {
            transaction.getPlayer().sendMessage(
                    getMessageFormat().format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.PAY_FAILED))
            );
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
    public List<String> onTab(CommandSender commandSender, ArrayList<String> args) {
        return null;
    }
}
