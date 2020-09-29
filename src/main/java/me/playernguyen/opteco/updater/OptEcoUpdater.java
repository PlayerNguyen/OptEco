package me.playernguyen.opteco.updater;

import me.playernguyen.opteco.OptEcoImplementation;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;

public class OptEcoUpdater extends OptEcoImplementation {

    public static final String UPDATE_LINK = "https://www.spigotmc.org/resources/";
    public static final String UPDATE_URL = "https://api.spigotmc.org/legacy/update.php?resource=";

    private final int resourceId;

    public OptEcoUpdater(int resourceId) {
        getPlugin().getDebugger().info("Creating connection check for updates...");
        this.resourceId = resourceId;
        getPlugin().getDebugger().info("Checking for updates with id " + resourceId);
    }

    public void getVersion(final Consumer<String> consumer) {
        getPlugin().getDebugger().info("Creating runTaskAsynchronously and looking for updates...");
        Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            try (InputStream inputStream = new URL(UPDATE_URL + this.resourceId).openStream();
                 Scanner scanner = new Scanner(inputStream)) {
                if (scanner.hasNext()) {
                    String next = scanner.next();
                    getPlugin().getDebugger().info("Found next version of: " + next);
                    consumer.accept(next);
                }
            } catch (IOException exception) {
                getPlugin().getLogger().info("Cannot look for updates: " + exception.getMessage());
            }
        });
    }

}

