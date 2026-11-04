package me.playernguyen.opteco.command;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.command.ConsoleCommandSenderMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import me.playernguyen.opteco.MoneyAssert;
import me.playernguyen.opteco.OptEco;
import me.playernguyen.opteco.TestPluginFolders;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Drives the real plugin through the server console, the way an operator does on
 * a live server: commands are dispatched to the console sender and the replies
 * are read back from it.
 *
 * <p>This closes the gap between pure unit tests and a full server run: the real
 * dispatcher, permission checks, argument parsing, {@link
 * me.playernguyen.opteco.runtime.AsyncExecutor} callbacks, message rendering and
 * SQLite storage all take part. The async work is completed by ticking the mock
 * scheduler, which is what the plugin's main-thread bridge schedules onto.</p>
 */
class ConsoleCommandTest {

    /** Console commands must answer within this budget. */
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    /** Every line the console has received, already stripped of colour codes. */
    private final List<String> output = new ArrayList<>();

    private ServerMock server;
    private ConsoleCommandSenderMock console;
    private OptEco plugin;
    private PlayerMock alice;

    @BeforeEach
    void setUp() throws IOException {
        server = MockBukkit.mock();
        console = server.getConsoleSender();
        writeFile(new File(TestPluginFolders.dataFolder(server), "config.yml"), "Update:\n  CheckForUpdate: false\n");
        writeFile(
                new File(new File(TestPluginFolders.pluginsFolder(server), "bStats"), "config.yml"),
                "enabled: false\nserverUuid: 00000000-0000-0000-0000-000000000000\n");
        alice = server.addPlayer("Alice");
        plugin = MockBukkit.load(OptEco.class);
    }

    @AfterEach
    void tearDown() {
        if (plugin != null && plugin.isEnabled()) {
            server.getPluginManager().disablePlugin(plugin);
        }
        MockBukkit.unmock();
    }

    @Test
    void shouldAddPointsToAnOnlinePlayerFromTheConsole() {
        // Act
        dispatch(console, "points add Alice 100");
        awaitOutput("Added 100 points to Alice");

        // Assert
        MoneyAssert.assertMoney("100", plugin.api().getBalance(alice.getUniqueId()));
    }

    @Test
    void shouldTakePointsFromAnOnlinePlayerFromTheConsole() {
        // Arrange
        dispatch(console, "points add Alice 100");
        awaitOutput("Added 100 points to Alice");

        // Act
        dispatch(console, "points take Alice 40");
        awaitOutput("Took 40 points from Alice");

        // Assert
        MoneyAssert.assertMoney("60", plugin.api().getBalance(alice.getUniqueId()));
    }

    @Test
    void shouldSetAnExactBalanceFromTheConsole() {
        // Act
        dispatch(console, "points set Alice 250");
        awaitOutput("Set Alice's balance to 250 points");

        // Assert
        MoneyAssert.assertMoney("250", plugin.api().getBalance(alice.getUniqueId()));
    }

    @Test
    void shouldPrintTheLeaderboardFromTheConsole() {
        // Arrange
        PlayerMock bob = server.addPlayer("Bob");
        dispatch(console, "points add Alice 100");
        awaitOutput("Added 100 points to Alice");
        dispatch(console, "points add Bob 50");
        awaitOutput("Added 50 points to Bob");

        // Act
        dispatch(console, "points top");

        // Assert
        awaitOutput("OptEco Top");
        awaitOutput("1. Alice - 100 points");
        awaitOutput("2. Bob - 50 points");
        MoneyAssert.assertMoney("100", plugin.api().getBalance(alice.getUniqueId()));
        MoneyAssert.assertMoney("50", plugin.api().getBalance(bob.getUniqueId()));
    }

    @Test
    void shouldReloadConfigurationFromTheConsole() {
        // Act
        dispatch(console, "points reload");

        // Assert
        awaitOutput("Configuration and language reloaded");
        assertTrue(plugin.isEnabled(), "Reload must not disable the plugin");
    }

    @Test
    void shouldShowHelpWhenTheConsoleRunsTheBareCommand() {
        // Act
        dispatch(console, "points");

        // Assert
        awaitOutput("OptEco Commands");
        awaitOutput("/opteco add <player> <amount>");
        awaitOutput("/opteco top");
    }

    @Test
    void shouldRejectAPlayerOnlyCommandFromTheConsole() {
        // Act
        dispatch(console, "points me");

        // Assert
        awaitOutput("only be used by a player");
    }

    @Test
    void shouldReportAnUnknownSubcommandAndPrintHelp() {
        // Act
        dispatch(console, "points nope");

        // Assert
        awaitOutput("Command not found");
        awaitOutput("OptEco Commands");
    }

    @Test
    void shouldRejectAdminCommandsFromAPlayerWithoutPermission() {
        // Arrange
        PlayerMock bob = server.addPlayer("Bob");

        // Act
        dispatch(bob, "points add Alice 10");

        // Assert
        awaitPlayerOutput(bob, "do not have permission");
        MoneyAssert.assertMoney("0", plugin.api().getBalance(alice.getUniqueId()));
    }

    /**
     * Dispatches one command line exactly as the server would.
     *
     * @param sender the console or a player issuing the command
     * @param commandLine the command line without a leading slash
     */
    private void dispatch(CommandSender sender, String commandLine) {
        assertTrue(server.dispatchCommand(sender, commandLine), "'" + commandLine + "' should be a known command");
    }

    /**
     * Ticks the mock scheduler until the console received a line containing the
     * expected fragment. Async commands reply from a scheduler task, so the
     * queue has to be drained on every tick.
     *
     * @param fragment text the console should have printed
     */
    private void awaitOutput(String fragment) {
        Instant deadline = Instant.now().plus(TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            server.getScheduler().performTicks(1L);
            collectOutput();
            if (output.stream().anyMatch(line -> line.contains(fragment))) {
                return;
            }
            sleepBriefly();
        }
        fail("Timed out waiting for console output containing '" + fragment + "'. Received: " + output);
    }

    /**
     * Ticks the mock scheduler until a player received a line containing the
     * expected fragment. Replies go to the sender, so player-issued commands
     * have to be read from the player, not from the console.
     *
     * @param player the player that issued the command
     * @param fragment text the player should have received
     */
    private void awaitPlayerOutput(PlayerMock player, String fragment) {
        List<String> received = new ArrayList<>();
        Instant deadline = Instant.now().plus(TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            server.getScheduler().performTicks(1L);
            String message;
            while ((message = player.nextMessage()) != null) {
                received.add(ChatColor.stripColor(message));
            }
            if (received.stream().anyMatch(line -> line.contains(fragment))) {
                return;
            }
            sleepBriefly();
        }
        fail("Timed out waiting for player output containing '" + fragment + "'. Received: " + received);
    }

    /** Moves every queued console message into {@link #output} without colour codes. */
    private void collectOutput() {
        String message;
        while ((message = console.nextMessage()) != null) {
            output.add(ChatColor.stripColor(message));
        }
    }

    /** Avoids spinning the CPU while the database thread finishes its work. */
    private void sleepBriefly() {
        try {
            TimeUnit.MILLISECONDS.sleep(10L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            fail("Interrupted while waiting for the console output");
        }
    }

    private static void writeFile(File file, String contents) throws IOException {
        File parent = file.getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }
        Files.write(file.toPath(), contents.getBytes(StandardCharsets.UTF_8));
    }
}
