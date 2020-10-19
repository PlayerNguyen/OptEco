package me.playernguyen.opteco.playerpoints;

import com.google.common.base.Preconditions;
import me.playernguyen.opteco.OptEco;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PlayerPointsAdapter {

    private static final String PLAYER_POINTS_STORAGE_FILE_NAME = "storage.yml";
    private final File dataFolder;
    private final File storageFile;
    private final boolean hasPlugin;
    private final Plugin playerPointsPlugin;

    public PlayerPointsAdapter() {
        // If not found PlayerPoints, throw an exception
        this.playerPointsPlugin = Bukkit.getPluginManager().getPlugin("PlayerPoints");
        this.hasPlugin = playerPointsPlugin != null;
        // If found, doing the process
        this.dataFolder = (playerPointsPlugin != null) ? playerPointsPlugin.getDataFolder(): null;
        this.storageFile = new File(getDataFolder(), PLAYER_POINTS_STORAGE_FILE_NAME);
    }

    public Plugin getPlayerPointsPlugin() {
        return playerPointsPlugin;
    }

    public void disable() {
        OptEco.getInstance().getPluginLoader().disablePlugin(getPlayerPointsPlugin());
    }

    public boolean isHasPlugin() {
        return hasPlugin;
    }

    public File getDataFolder() {
        return dataFolder;
    }

    public List<PlayerPointsObject> collect() {
        ArrayList<PlayerPointsObject> playerPointsObjects = new ArrayList<>();
        // Scan the PlayerPoints/storage.yml
        FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(storageFile);
        // File configuration loader
        if (fileConfiguration.contains("Points")) {
            ConfigurationSection points = fileConfiguration.getConfigurationSection("Points");
            // Not null check
            Preconditions.checkNotNull(points);
            // Collect as set
            Set<String> uuids = points.getKeys(false);
            // Put set
            for (String uuid : uuids) {
                playerPointsObjects.add(new PlayerPointsObject(UUID.fromString(uuid),
                        points.getInt(uuid)));
            }
        }

        return playerPointsObjects;
    }

    public File getStorageFile() {
        return storageFile;
    }

    public static class PlayerPointsObject {

        private final UUID uuid;
        private final int point;

        public PlayerPointsObject(UUID uuid, int point) {
            this.uuid = uuid;
            this.point = point;
        }

        public int getPoint() {
            return point;
        }

        public UUID getUUID() {
            return uuid;
        }
    }



}
