package me.playernguyen.account;

import me.playernguyen.OptEco;
import me.playernguyen.configuration.AccountLoader;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class AccountConfiguration {

    private Player player;
    private OptEco plugin;
    private File file;
    private YamlConfiguration configuration;

    public AccountConfiguration (Player player, OptEco plugin) {
        this.player = player;
        this.plugin = plugin;

        File folder = new File(
                plugin.getDataFolder(),
                AccountLoader.ACCOUNT_STORE_FOLDER
        );

        if (!folder.exists()) folder.mkdir();

        this.file = new File(folder, player.getDisplayName()+".yml");
        this.configuration = YamlConfiguration.loadConfiguration(this.file);

    }

    public OptEco getPlugin() {
        return plugin;
    }

    public File getFile() {
        return file;
    }

    public Player getPlayer() {
        return player;
    }

    public YamlConfiguration getConfiguration() {
        return configuration;
    }

    public boolean save(HashMap <String, Object> configurations) {
        try {
            configurations.forEach((s, o) -> this.getConfiguration().set(s, o));
            this.getConfiguration().save(file);
            return true;
        } catch (IOException e) {
            this.getPlugin().getDebugger().printException(e);
            return false;
        }
    }

    public boolean save(Account account) {
        try {
            this.getConfiguration().set("data.player", account.getPlayer().getName());
            this.getConfiguration().set("data.balance", account.getBalance());
            this.getConfiguration().set("data.uuid", account.getPlayer().getUniqueId().toString());
            this.getConfiguration().save(file);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Account toAccount () {
        Player player =
                Bukkit.getServer().getPlayer(
                        UUID.fromString(Objects.requireNonNull(getConfiguration().getString("data.uuid")))
                );
        double balance = getConfiguration().getDouble("data.balance");
        return new Account(player, balance);
    }

}
