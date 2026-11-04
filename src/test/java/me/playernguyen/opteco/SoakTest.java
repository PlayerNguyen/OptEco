package me.playernguyen.opteco;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import me.playernguyen.opteco.core.model.BalanceChangeReason;
import me.playernguyen.opteco.core.model.PaymentResult;
import me.playernguyen.opteco.core.model.PaymentStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Opt-in soak test for lifecycle leaks.
 *
 * <p>It drives the real plugin inside MockBukkit through repeated join/quit
 * churn, balance changes, title animations, and payment state transitions, then
 * asserts that every per-player structure is back to zero. It is deliberately
 * excluded from {@code ./gradlew test} and only runs through
 * {@code ./gradlew soakTest}, which also records JFR and configures a small heap
 * so a regression is easy to diagnose with {@code jcmd} or JMC.</p>
 *
 * <p>The counters are authoritative for the two known retention paths: the
 * write-through account cache after a join/quit race, and the balance title
 * animation map after an animation finishes. Use the JFR recording in
 * {@code build/reports/soak/soak.jfr} when the counters stay clean but the live
 * set still grows.</p>
 */
@Tag("soak")
class SoakTest {

    /** Iterations; override with {@code -PsoakIterations=...}. */
    private static final int ITERATIONS = Math.max(1, Integer.getInteger("soak.iterations", 150));

    /** Players per churn batch; override with {@code -PsoakPlayers=...}. */
    private static final int PLAYERS_PER_BATCH = Math.max(1, Integer.getInteger("soak.players", 20));

    /** Players that receive balance changes and therefore a title animation. */
    private static final int TITLE_PLAYERS = 3;

    /** Upper bound for one database executor drain. */
    private static final long DRAIN_TIMEOUT_SECONDS = 20L;

    private ServerMock server;
    private OptEco plugin;

    @BeforeEach
    void setUp() throws IOException {
        server = MockBukkit.mock();
        writeFile(new File(TestPluginFolders.dataFolder(server), "config.yml"), "Update:\n  CheckForUpdate: false\n");
        writeFile(
                new File(new File(TestPluginFolders.pluginsFolder(server), "bStats"), "config.yml"),
                "enabled: false\nserverUuid: 00000000-0000-0000-0000-000000000000\n");
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
    void shouldReleasePerPlayerStateAfterChurnAndAnimations() {
        // Act
        for (int iteration = 1; iteration <= ITERATIONS; iteration++) {
            churnPlayers(iteration);
            if (iteration % 5 == 0) {
                exerciseTitleAnimations(iteration);
            }
            if (iteration % 10 == 0) {
                exercisePayments(iteration);
            }
        }

        // Assert
        assertPerPlayerStateReleased();
    }

    /**
     * Joins a batch and disconnects it immediately, which is the race that used
     * to leave offline accounts in the write-through cache.
     *
     * @param iteration current soak iteration, used to keep names unique
     */
    private void churnPlayers(int iteration) {
        List<PlayerMock> joined = new ArrayList<>();
        for (int index = 0; index < PLAYERS_PER_BATCH; index++) {
            joined.add(server.addPlayer("Soak" + iteration + "x" + index));
        }
        // No scheduler tick: the join loads are still queued, so they finish
        // after the quit eviction unless the completion callback cleans up.
        for (PlayerMock player : joined) {
            player.disconnect();
        }

        awaitAsyncIdle();
        server.getScheduler().performTicks(2L);

        assertEquals(0, plugin.economy().cachedAccounts(), "A join/quit race must not leave offline players cached");
    }

    /**
     * Starts an animation for a few online players, lets it finish, and checks
     * that the listener released every tracking entry.
     *
     * @param iteration current soak iteration, used to keep names unique
     */
    private void exerciseTitleAnimations(int iteration) {
        List<PlayerMock> players = new ArrayList<>();
        for (int index = 0; index < TITLE_PLAYERS; index++) {
            PlayerMock player = server.addPlayer("Title" + iteration + "x" + index);
            players.add(player);
            plugin.economy().ensureAccount(player.getUniqueId(), player.getName());
            plugin.economy().add(player.getUniqueId(), player.getName(), BigDecimal.ONE, BalanceChangeReason.ADMIN_ADD);
        }
        assertEquals(
                TITLE_PLAYERS,
                plugin.activeTitleAnimations(),
                "Every balance change for an online player should start one animation");

        server.getScheduler().performTicks(120L);

        assertEquals(0, plugin.activeTitleAnimations(), "Finished animations must release their tracking entry");
        for (PlayerMock player : players) {
            player.disconnect();
        }
        awaitAsyncIdle();
        server.getScheduler().performTicks(2L);
    }

    /**
     * Exercises request/confirm/cancel and checks that the pending index is
     * emptied on every terminal state.
     *
     * @param iteration current soak iteration, used to keep names unique
     */
    private void exercisePayments(int iteration) {
        UUID sender = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();
        String senderName = "Pay" + iteration;
        String receiverName = "Receive" + iteration;
        plugin.economy().ensureAccount(sender, senderName);
        plugin.economy().ensureAccount(receiver, receiverName);
        plugin.economy().add(sender, senderName, BigDecimal.TEN, BalanceChangeReason.ADMIN_ADD);

        PaymentResult requested = plugin.payments().request(sender, senderName, receiver, receiverName, BigDecimal.ONE);
        assertEquals(PaymentStatus.SUCCESS, requested.status());

        PaymentResult confirmed = plugin.payments().confirm(sender);
        assertEquals(PaymentStatus.SUCCESS, confirmed.status());
        assertTrue(
                plugin.payments().pendingTransactions().isEmpty(), "Confirmed payments must leave the pending index");

        PaymentResult second = plugin.payments().request(sender, senderName, receiver, receiverName, BigDecimal.ONE);
        assertEquals(PaymentStatus.SUCCESS, second.status());

        PaymentResult cancelled = plugin.payments().cancel(sender);
        assertEquals(PaymentStatus.SUCCESS, cancelled.status());
        assertTrue(
                plugin.payments().pendingTransactions().isEmpty(), "Cancelled payments must leave the pending index");

        plugin.economy().evict(sender);
        plugin.economy().evict(receiver);
    }

    /** Verifies that every per-player structure is empty at the end of the run. */
    private void assertPerPlayerStateReleased() {
        awaitAsyncIdle();
        server.getScheduler().performTicks(120L);

        assertEquals(0, plugin.economy().cachedAccounts(), "The account cache must only hold online players");
        assertEquals(0, plugin.activeTitleAnimations(), "Finished animations must not be retained");
        assertTrue(
                plugin.payments().pendingTransactions().isEmpty(),
                "No payment may stay pending after the terminal transitions");
    }

    /**
     * Submits a barrier task to the plugin's database executor and waits for it,
     * so every earlier join load and its completion callback has been scheduled.
     */
    private void awaitAsyncIdle() {
        try {
            plugin.async().supply(() -> Boolean.TRUE).get(DRAIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception exception) {
            fail("Timed out waiting for the OptEco database executor: " + exception);
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
