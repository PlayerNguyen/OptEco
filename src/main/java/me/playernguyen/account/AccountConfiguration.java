package me.playernguyen.account;

import me.playernguyen.OptEco;
import me.playernguyen.OptEcoObject;
import me.playernguyen.configuration.AccountLoader;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class AccountConfiguration extends OptEcoObject implements IYamlAccount {

    private OfflinePlayer player;
    private File file;
    private YamlConfiguration configuration;

    public AccountConfiguration (UUID uuid, OptEco plugin) {
        super(plugin);
        this.player = Bukkit.getOfflinePlayer(uuid);

        File folder = new File(
                plugin.getDataFolder(),
                AccountLoader.ACCOUNT_STORE_FOLDER
        );

        if (!folder.exists()) folder.mkdir();

        this.file = new File(folder, player.getName()+".yml");
        this.configuration = YamlConfiguration.loadConfiguration(this.file);

    }


    public File getFile() {
        return file;
    }

    public OfflinePlayer getPlayer() {
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
            this.getConfiguration().set("data.player", Bukkit.getOfflinePlayer(account.getPlayer()).getName());
            this.getConfiguration().set("data.balance", account.getBalance());
            this.getConfiguration().set("data.uuid", account.getPlayer().toString());
            this.getConfiguration().save(file);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Account getAccount() {
        return this.toAccount();
    }

    private Account toAccount () {
        Player player =
                Bukkit.getServer().getPlayer(
                        UUID.fromString(Objects.requireNonNull(getConfiguration().getString("data.uuid")))
                );
        double balance = getConfiguration().getDouble("data.balance");
        return new Account(player, balance);
    }

}
