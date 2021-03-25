package me.playernguyen.opteco.command;

import me.playernguyen.opteco.OptEco;
import me.playernguyen.opteco.configuration.OptEcoConfiguration;
import me.playernguyen.opteco.language.OptEcoLanguage;
import me.playernguyen.opteco.account.Account;
import me.playernguyen.opteco.permission.OptEcoPermission;
import org.bukkit.command.CommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SubCommandTop extends SubCommand {

    public SubCommandTop(String command, OptEco plugin) {
        super(
                command,
                plugin.getLanguageLoader().getLanguage(OptEcoLanguage.COMMAND_DESCRIBE_TOP),
                plugin
        );

        addPermissions(OptEcoPermission.ADMIN);
        addPermissions(OptEcoPermission.TOP);
        addPermissions(OptEcoPermission.USER);
        addPermissions(OptEcoPermission.EVERYTHING);
    }

    @Override
    public boolean onPlayerCommand(Player player, ArrayList<String> args) {
        return execute(player, args);
    }

    @Override
    public boolean onConsoleCommand(CommandSender sender, ArrayList<String> args) {
        return execute(sender, args);
    }

    @Override
    public boolean onRemoteConsoleCommand(RemoteConsoleCommandSender sender, ArrayList<String> args) {
        return execute(sender, args);
    }

    @Override
    public List<String> onTab(CommandSender commandSender, ArrayList<String> args) {
        return null;
    }

    private boolean execute(CommandSender sender, ArrayList<String> args) {
        sender.sendMessage(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.RED_BAR));
        List<Account> accounts = getPlugin().getAccountDatabase().topPlayer(
                getPlugin().getConfigurationLoader().getInt(OptEcoConfiguration.COMMAND_LIMIT_TOP)
        );
        int i = 0;
        for (Account account : accounts) {
            i++;
            String name = account.getOfflinePlayer().getName();
            double balance = account.getBalance();
            sender.sendMessage(
                    getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.COMMAND_TOP_FORMAT)
                            .replace("%id%", String.valueOf(i))
                            .replace("%name%", name != null ? name : "<undefined>")
                            .replace("%balance%", String.valueOf(balance))
            );
        }
        sender.sendMessage(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.RED_BAR));
        return true;
    }
}
