package me.playernguyen.opteco.updater;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import me.playernguyen.opteco.runtime.AsyncExecutor;
import me.playernguyen.opteco.util.Semver;
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

    /** GitHub endpoint returning the newest published release. */
    private static final String LATEST_RELEASE = "https://api.github.com/repos/PlayerNguyen/OptEco/releases/latest";

    /** Plugin used for the asynchronous scheduler task and fine-level logging. */
    private final Plugin plugin;

    /** Executor used to hop back to the main thread before printing the notice. */
    private final AsyncExecutor async;

    /**
     * Creates a checker.
     *
     * @param plugin owning plugin, used to schedule the background request and log failures
     * @param async executor that delivers the console notice on the main thread
     */
    public UpdateChecker(@NotNull Plugin plugin, @NotNull AsyncExecutor async) {
        this.plugin = plugin;
        this.async = async;
    }

    /**
     * Fetches the latest GitHub release and prints a console notice when it is
     * newer than the running plugin version.
     *
     * @param currentVersion version from {@code plugin.yml}; may contain a
     *                       leading {@code v}
     */
    public void check(@NotNull String currentVersion) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Release release = fetchLatestRelease();
            if (release == null) {
                return;
            }
            boolean newer;
            try {
                newer = release.isNewerThan(currentVersion);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().fine("Skipping update notice, cannot compare versions: " + exception.getMessage());
                return;
            }
            if (!newer) {
                return;
            }
            async.runOnMain(() -> Bukkit.getConsoleSender()
                    .sendMessage(ChatColor.YELLOW + "OptEco " + release.tag + " is available: " + release.url));
        });
    }

    /**
     * Performs the GitHub request with short timeouts.
     *
     * <p>Every failure, including a non-200 response, malformed JSON or a
     * network error, is logged at fine level and reported as {@code null} so the
     * update check never disrupts startup.</p>
     *
     * @return the latest release, or {@code null} when it could not be read
     */
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

    /** Minimal view of the GitHub release fields used by the notice. */
    private static final class Release {
        /** Release tag, for example {@code v1.2.3}. */
        private final String tag;

        /** Browser URL of the release. */
        private final String url;

        /**
         * Creates the release view.
         *
         * @param tag release tag
         * @param url release page URL
         */
        private Release(String tag, String url) {
            this.tag = tag;
            this.url = url;
        }

        /**
         * Checks whether this release is newer than the running version.
         *
         * @param currentVersion version from {@code plugin.yml}
         * @return {@code true} when the release has higher SemVer precedence
         * @throws IllegalArgumentException when either version cannot be parsed
         */
        private boolean isNewerThan(@NotNull String currentVersion) {
            return Semver.isNewer(tag, currentVersion);
        }
    }
}
