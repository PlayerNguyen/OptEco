package me.playernguyen.opteco;

import be.seeseemelk.mockbukkit.ServerMock;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * Resolves the folders MockBukkit uses for a plugin.
 *
 * <p>MockBukkit names a plugin data folder {@code <name>-<version>}, with the
 * version read from the {@code plugin.yml} on the classpath. Tests that want to
 * pre-seed {@code config.yml} before the plugin enables therefore cannot use
 * the bare plugin name.</p>
 */
public final class TestPluginFolders {

    private TestPluginFolders() {}

    /**
     * Returns the data folder MockBukkit creates for OptEco.
     *
     * @param server the mock server that owns the plugins folder
     * @return the plugin data folder, which may not exist yet
     */
    @NotNull
    public static File dataFolder(@NotNull ServerMock server) {
        YamlConfiguration description = new YamlConfiguration();
        try (InputStream stream = OptEco.class.getResourceAsStream("/plugin.yml")) {
            if (stream == null) {
                throw new IllegalStateException("plugin.yml is missing from the test classpath");
            }
            description.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException exception) {
            throw new IllegalStateException("Could not read plugin.yml for the test data folder", exception);
        }
        return new File(
                server.getPluginsFolder(), description.getString("name") + "-" + description.getString("version"));
    }

    /**
     * Returns the folder shared by plugins that keep their own configuration in
     * a sibling folder, such as bStats.
     *
     * @param server the mock server that owns the plugins folder
     * @return the plugins folder itself
     */
    @NotNull
    public static File pluginsFolder(@NotNull ServerMock server) {
        return server.getPluginsFolder();
    }
}
