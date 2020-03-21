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

public class SubCommandPay extends SubCommand {

    public SubCommandPay(String s, OptEco pl) {
        super(s, pl.getLanguageLoader().getLanguage(OptEcoLanguage.COMMAND_DESCRIBE_PAY), pl);

        addArgument(CommandArguments.PLAYER);
        addArgument(CommandArguments.AMOUNT);

        addPermissions(OptEcoPermission.EVERYTHING);
        addPermissions(OptEcoPermission.ADMIN);
        addPermissions(OptEcoPermission.PAY);
        addPermissions(OptEcoPermission.USER);

    }

    @Override
    public boolean onPlayerCommand(Player player, ArrayList<String> args) {
        // If args lower than 2
        if (args.size() < 2) {
            player.sendMessage(getMessageFormat().format(getHelp()));
            return true;
        }
        // If sender has another transaction
        if (getPlugin().getTransactionManager().hasTransaction(player)) {
            player.sendMessage(
                    getMessageFormat()
                            .format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.PAY_ON_TRANSACTION))
            );
            return true;
        }
        // Get args of player, value
        String _target = args.get(0); Player target = Bukkit.getServer().getPlayerExact(_target);
        String _value = args.get(1);
        if (target == null) {
            player.sendMessage(
                    getMessageFormat()
                            .format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.VAR_PLAYER_NOT_FOUND))
                            .replaceAll("%who%", _target)
            );
            return true;
        }
        // If sender transfer to themselves
        if (target.equals(player)) {
            player.sendMessage(getMessageFormat().format(
                    getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.PAY_CANNOT_SELF_TRANSFER)
            ));
            return true;
        }
        // If the value is not number
        if (!ValidationChecker.isNumber(_value)) {
            player.sendMessage(
                    getMessageFormat()
                            .format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.VAR_NOT_A_NUMBER))
                            .replace("%value%", _value)
            );
            return true;
        }
        // If the value is negative
        if (Double.parseDouble(_value) < 0) {
            player.sendMessage(
                    getMessageFormat()
                            .format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.VALUE_CANNOT_BE_NEGATIVE))
            );
            return true;
        }
        // If sender don't have enough points
        if ((getPlugin().getAccountLoader().getBalance(player) - Double.parseDouble(_value))
                < getPlugin().getConfigurationLoader().getDouble(OptEcoConfiguration.MIN_BALANCE) ) {
            player.sendMessage(
                    getMessageFormat()
                            .format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.PAY_NOT_ENOUGH))
                            .replaceAll("%value%", _value)
            );
            return true;
        }
        // Excellent condition. Create new Task to countdown and wait for accept/deny
        player.sendMessage(getMessageFormat()
                .format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.PAY_CONFIRM_DISPLAY))
                .replaceAll("%value%", String.valueOf(getPlugin().getConfigurationLoader().getInt(OptEcoConfiguration.PAYMENT_CONFIRM)))
        );
        this.getPlugin().getTransactionManager().addTransaction(player, target, Double.parseDouble(_value));
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
