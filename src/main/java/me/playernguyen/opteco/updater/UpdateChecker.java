package me.playernguyen.opteco.updater;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import me.playernguyen.opteco.runtime.AsyncExecutor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Checks the latest GitHub release for OptEco.
 *
 * <p>GitHub releases are the source of truth for published versions; the
 * Spigot resource page is only a distribution channel. The request is made
 * asynchronously with short timeouts so a slow or unavailable GitHub API never
 * delays server startup.</p>
 */
public final class UpdateChecker {

    private static final String LATEST_RELEASE = "https://api.github.com/repos/PlayerNguyen/OptEco/releases/latest";

    private final Plugin plugin;
    private final AsyncExecutor async;

    public UpdateChecker(@NotNull Plugin plugin, @NotNull AsyncExecutor async) {
        this.plugin = plugin;
        this.async = async;
    }

    /**
     * Fetches the latest GitHub release and prints a console notice when its tag
     * differs from the running plugin version.
     *
     * @param currentVersion version from {@code plugin.yml}; may contain a
     *                       leading {@code v}
     */
    public void check(@NotNull String currentVersion) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Release release = fetchLatestRelease();
            if (release == null || normalize(release.tag).equalsIgnoreCase(normalize(currentVersion))) {
                return;
            }
            async.runOnMain(() -> Bukkit.getConsoleSender()
                    .sendMessage(ChatColor.YELLOW + "OptEco " + release.tag + " is available: " + release.url));
        });
    }

    @SuppressWarnings("deprecation") // JsonParser's instance API works on the Gson bundled by 1.13 servers.
    private Release fetchLatestRelease() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(LATEST_RELEASE).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("User-Agent", "OptEco-UpdateChecker");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                plugin.getLogger().fine("GitHub update check returned HTTP " + connection.getResponseCode());
                return null;
            }
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
            }
            JsonObject json = new JsonParser().parse(body.toString()).getAsJsonObject();
            if (!json.has("tag_name") || !json.has("html_url")) {
                return null;
            }
            return new Release(
                    json.get("tag_name").getAsString(), json.get("html_url").getAsString());
        } catch (Exception exception) {
            plugin.getLogger().fine("GitHub update check failed: " + exception.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String normalize(String version) {
        return version.startsWith("v") || version.startsWith("V") ? version.substring(1) : version;
    }

    private static final class Release {
        private final String tag;
        private final String url;

        private Release(String tag, String url) {
            this.tag = tag;
            this.url = url;
        }
    }
}
