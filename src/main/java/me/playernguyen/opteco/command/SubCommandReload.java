package me.playernguyen.opteco.command;

import me.playernguyen.opteco.OptEco;
import me.playernguyen.opteco.OptEcoLanguage;
import me.playernguyen.opteco.permission.OptEcoPermission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SubCommandReload extends SubCommand {

    SubCommandReload(String command, OptEco plugin) {
        super(command, plugin.getLanguageLoader().getLanguage(OptEcoLanguage.COMMAND_DESCRIBE_RELOAD), plugin);

        addPermissions(OptEcoPermission.ADMIN);
        addPermissions(OptEcoPermission.EVERYTHING);
        addPermissions(OptEcoPermission.RELOAD);

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

    public boolean execute(CommandSender sender, ArrayList<String> args) {

        // Reload config
        getPlugin().getConfigurationLoader().reload();

        // Reload language
        getPlugin().getLanguageLoader().reload();

        // Re-disable and enable
        Bukkit.getServer().getPluginManager().disablePlugin(getPlugin());
        Bukkit.getServer().getPluginManager().enablePlugin(getPlugin());
        sender.sendMessage(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.GRAY_BAR));
        sender.sendMessage(
                ChatColor.GRAY + " - OptEco by Player_Nguyen version " + getPlugin().getDescription().getVersion()
        );
        sender.sendMessage(
                getMessageFormat().format(getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.RELOAD_DONE))
        );

        return true;
    }

    @Override
    public List<String> onTab(CommandSender commandSender, ArrayList<String> args) {
        return null;
    }
}
