package me.playernguyen.updater;

import me.playernguyen.OptEco;
import me.playernguyen.OptEcoObject;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;

public class OptEcoUpdater extends OptEcoObject {

    public static final String UPDATE_LINK = "https://www.spigotmc.org/resources/";
    public static final String UPDATE_URL = "https://api.spigotmc.org/legacy/update.php?resource=";

    private int resourceId;

    public OptEcoUpdater(OptEco plugin, int resourceId) {
        super(plugin);
        getPlugin().getDebugger().info("Creating connection check for updates...");
        this.resourceId = resourceId;
        getPlugin().getDebugger().info("Check for updates id " + resourceId);
    }

    public void getVersion(final Consumer<String> consumer) {
        getPlugin().getDebugger().info("Create runTaskAsynchronously and look for updates...");
        Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            try (InputStream inputStream = new URL(UPDATE_URL + this.resourceId).openStream(); Scanner scanner = new Scanner(inputStream)) {
                if (scanner.hasNext()) consumer.accept(scanner.next());
            } catch (IOException exception) {
                getPlugin().getLogger().info("Cannot look for updates: " + exception.getMessage());
            }
        });
    }

}

