package me.playernguyen.opteco;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import me.playernguyen.opteco.api.OptEcoAPI;
import org.bukkit.command.PluginCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Boots the real plugin inside a mock Bukkit server to catch broken wiring,
 * missing resources, or schema failures that unit tests cannot see.
 */
class PluginStartupTest {

    private ServerMock server;
    private OptEco plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        if (plugin != null && plugin.isEnabled()) {
            server.getPluginManager().disablePlugin(plugin);
        }
        MockBukkit.unmock();
    }

    @Test
    void shouldEnablePluginAndRegisterRuntimeServicesWhenStartingFresh() throws IOException {
        // Arrange
        File pluginsFolder = server.getPluginsFolder();
        writeFile(
                new File(new File(pluginsFolder, OptEco.PLUGIN_NAME), "config.yml"),
                "Update:\n  CheckForUpdate: false\n");
        writeFile(
                new File(new File(pluginsFolder, "bStats"), "config.yml"),
                "enabled: false\nserverUuid: 00000000-0000-0000-0000-000000000000\n");

        // Act
        plugin = MockBukkit.load(OptEco.class);

        // Assert
        assertTrue(plugin.isEnabled(), "OptEco should stay enabled after onEnable()");
        assertSame(plugin, OptEco.getInstance());
        assertNotNull(plugin.configService());
        assertNotNull(plugin.messages());
        assertNotNull(plugin.async());
        assertNotNull(plugin.economy());
        assertNotNull(plugin.payments());
        assertNotNull(plugin.api());

        OptEcoAPI api = server.getServicesManager().load(OptEcoAPI.class);
        assertNotNull(api, "OptEcoAPI should be registered with Bukkit's ServicesManager");
        assertSame(plugin.api(), api);

        PluginCommand command = server.getPluginCommand("opteco");
        assertNotNull(command, "plugin.yml should register the opteco command");
        assertNotNull(command.getExecutor(), "The dispatcher should be installed as executor");
        assertNotNull(command.getTabCompleter(), "The dispatcher should be installed as tab completer");

        File dataFolder = plugin.getDataFolder();
        assertTrue(new File(dataFolder, "config.yml").isFile(), "config.yml should be created");
        assertTrue(new File(dataFolder, "language/en.yml").isFile(), "The language file should be created");
        assertTrue(new File(dataFolder, "account.sqlite").isFile(), "The SQLite database should be created");

        UUID playerId = UUID.randomUUID();
        plugin.economy().ensureAccount(playerId, "SmokeTest");
        assertTrue(api.hasAccount(playerId), "The migrated schema should accept new accounts");
        assertEquals(0, api.getBalance(playerId).compareTo(BigDecimal.ZERO));
    }

    private static void writeFile(File file, String contents) throws IOException {
        File parent = file.getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }
        Files.write(file.toPath(), contents.getBytes(StandardCharsets.UTF_8));
    }
}
