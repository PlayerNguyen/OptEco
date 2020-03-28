package me.playernguyen.command;

import me.playernguyen.OptEco;
import me.playernguyen.OptEcoConfiguration;
import me.playernguyen.OptEcoLanguage;
import me.playernguyen.permission.OptEcoPermission;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SubCommandMe extends SubCommand {


    SubCommandMe(String command, OptEco plugin) {

        super(command, plugin.getLanguageLoader().getLanguage(OptEcoLanguage.COMMAND_DESCRIBE_SELFCHECK), plugin);

        addPermissions(OptEcoPermission.EVERYTHING);
        addPermissions(OptEcoPermission.ADMIN);
        addPermissions(OptEcoPermission.ME);
        addPermissions(OptEcoPermission.USER);
    }

    @Override
    public boolean onPlayerCommand(Player player, ArrayList<String> args) {
        player.sendMessage(
                getMessageFormat().format(
                        getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.CHECK_SELF)
                                .replace("%value%", getMessageFormat().numberFormat(getPlugin().getAccountLoader().getBalance(player.getUniqueId())))
                                .replace("%currency%", getPlugin().getConfigurationLoader().getString(OptEcoConfiguration.CURRENCY_SYMBOL))
                )
        );
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
