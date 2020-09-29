package me.playernguyen.opteco.account;

import me.playernguyen.opteco.OptEcoImplementation;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class AccountConfiguration extends OptEcoImplementation {

    public static final String ACCOUNT_STORE_FOLDER = "account";

    private final OfflinePlayer player;
    private File file;
    private YamlConfiguration configuration;

    public AccountConfiguration(UUID uuid) {

        this.player = Bukkit.getOfflinePlayer(uuid);

        // Making a directory if not existed
        File folder = new File(
                getPlugin().getDataFolder(),
                ACCOUNT_STORE_FOLDER
        );
        if (!folder.exists()) {
            if (!folder.mkdir())
                throw new IllegalStateException(String.format("Cannot make dir %s...", folder.getName()));
        }

        this.load(folder);

    }

    public boolean exist() {
        return getFile().exists();
    }

    private void load(File folder) {
        this.file = new File(folder, player.getName() + ".yml");
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

    public Account toAccount() {
        OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(getConfiguration().getString("data.uuid")));
        double balance = getConfiguration().getDouble("data.balance");
        return new Account(player, balance);
    }

}
