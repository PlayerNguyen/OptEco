package me.playernguyen.updater;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;

public class OptEcoUpdater {

    public static final String UPDATE_LINK = "https://www.spigotmc.org/resources/";
    public static final String UPDATE_URL = "https://api.spigotmc.org/legacy/update.php?resource=";

    private Plugin plugin;
    private int resourceId;

    public OptEcoUpdater(Plugin plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public void getVersion(final Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try (InputStream inputStream = new URL(UPDATE_URL + this.resourceId).openStream(); Scanner scanner = new Scanner(inputStream)) {

                if (scanner.hasNext()) {
                    consumer.accept(scanner.next());
                }
            } catch (IOException exception) {
                this.plugin.getLogger().info("Cannot look for updates: " + exception.getMessage());
            }
        });
    }

}

