package me.playernguyen.opteco.command;

import lombok.Getter;
import lombok.experimental.Accessors;
import me.playernguyen.opteco.OptEco;
import me.playernguyen.opteco.config.ConfigService;
import me.playernguyen.opteco.core.service.EconomyService;
import me.playernguyen.opteco.core.service.PaymentService;
import me.playernguyen.opteco.message.MessageService;
import me.playernguyen.opteco.runtime.AsyncExecutor;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable bundle of the services the command layer needs. The plugin builds
 * one instance during startup and passes the same object to
 * {@link CommandDispatcher}, which hands it to every {@link CommandContext}.
 *
 * <p>All fields are final references to long-lived singletons, so the bundle
 * itself is safely publishable. The services behind it keep their own
 * concurrency rules: database work must still run through
 * {@link me.playernguyen.opteco.runtime.AsyncExecutor}, and Bukkit lookups
 * through {@link PlayerResolver} belong on the main thread.
 */
@Getter
@Accessors(fluent = true)
public final class CommandDependencies {

    /** Owning plugin, used for logging and configuration reloads. */
    private final OptEco plugin;

    /** Reloadable configuration snapshot provider. */
    private final ConfigService config;

    /** Language and formatting service for player-facing output. */
    private final MessageService messages;

    /** Executor used to move database work off the main thread. */
    private final AsyncExecutor async;

    /** Economy facade for balance reads and mutations. */
    private final EconomyService economy;

    /** Pending-payment state machine used by the payment subcommands. */
    private final PaymentService payments;

    /** Resolves names to UUIDs and online players. */
    private final PlayerResolver players;

    /**
     * Creates the dependency bundle.
     *
     * @param plugin owning plugin
     * @param config configuration snapshot provider
     * @param messages language and formatting service
     * @param async executor for asynchronous database work
     * @param economy economy facade
     * @param payments pending-payment service
     * @param players player name resolver
     */
    public CommandDependencies(
            @NotNull OptEco plugin,
            @NotNull ConfigService config,
            @NotNull MessageService messages,
            @NotNull AsyncExecutor async,
            @NotNull EconomyService economy,
            @NotNull PaymentService payments,
            @NotNull PlayerResolver players) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.async = async;
        this.economy = economy;
        this.payments = payments;
        this.players = players;
    }
}
