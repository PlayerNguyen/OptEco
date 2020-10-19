package me.playernguyen.opteco.command;

import me.playernguyen.opteco.OptEco;
import me.playernguyen.opteco.permission.OptEcoPermission;
import me.playernguyen.opteco.playerpoints.PlayerPointsAdapter;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PlayerPointToOptEcoCommand extends OptEcoAbstractCommand{

    public PlayerPointToOptEcoCommand() {
        super("playerpointtoopteco");
        // Add permissions
        addPermissions(OptEcoPermission.EVERYTHING);
        addPermissions(OptEcoPermission.ADMIN);
        addPermissions(OptEcoPermission.PPTO);
    }

    @Override
    public boolean onPlayerCommand(Player player, Command command, String s, String[] args) {
        return exec(player, args);
    }

    @Override
    public boolean onConsoleCommand(ConsoleCommandSender sender, Command command, String s, String[] args) {
        return exec(sender, args);
    }

    @Override
    public boolean onRemoteConsole(RemoteConsoleCommandSender sender, Command command, String s, String[] args) {
        return exec(sender, args);
    }

    @Override
    public boolean onAny(CommandSender sender, Command command, String s, String[] args) {
        return exec(sender, args);
    }

    private boolean exec(CommandSender sender, String[] arguments) {
        sender.sendMessage(ChatColor.GOLD + "Trying to convert data...");
        PlayerPointsAdapter adapter = new PlayerPointsAdapter();
        // Checking
        if (!adapter.isHasPlugin()) {
            sender.sendMessage(ChatColor.RED + "The plugin PlayerPoints not found!");
            return true;
        }
        if (!adapter.getDataFolder().exists()) {
            sender.sendMessage(ChatColor.RED + "The folder plugins/PlayerPoints not found!");
            return true;
        }
        if (!adapter.getStorageFile().exists()) {
            sender.sendMessage(ChatColor.RED + "The storage file (storage.yml) of PlayerPoints not found!");
            return true;
        }
        // Collect data
        List<PlayerPointsAdapter.PlayerPointsObject> collect = adapter.collect();
        // Push data to database
        if (collect.size() > 0) {
            sender.sendMessage(ChatColor.GRAY + "Pushing data into the database...");
            for (PlayerPointsAdapter.PlayerPointsObject object : collect) {
                getLogger().info("---- Import ----");
                getLogger().info("- UUID: " + object.getUUID() + "-");
                getLogger().info("- Points: " + object.getPoint() + "-");
                sender.sendMessage(ChatColor.GRAY + " * " + object.getUUID() + ": " + object.getPoint() + "...");
                getAccountManager().pushNewOne(object.getUUID(), object.getPoint());
            }
        } else {
            sender.sendMessage(ChatColor.GRAY + " (Nothing changes because the storage is empty)");
        }
        sender.sendMessage(ChatColor.GREEN + "Done!");
        sender.sendMessage(ChatColor.GOLD + "Now you can remove the PlayerPoints plugin, reload and use " +
                "OptEco instead. Good luck!");

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return null;
    }
}
