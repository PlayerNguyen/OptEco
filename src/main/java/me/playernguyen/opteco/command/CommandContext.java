package me.playernguyen.opteco.command;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.playernguyen.opteco.core.exception.PlayerNotFoundException;
import me.playernguyen.opteco.core.exception.StorageException;
import me.playernguyen.opteco.message.MessageKey;
import me.playernguyen.opteco.util.AmountParseException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable view of one command invocation: the shared service bundle, the
 * sender who triggered it, and the arguments that follow the subcommand name.
 *
 * <p>A dispatcher creates a context on the main server thread for every
 * invocation and hands it to exactly one {@link SubCommand}. The context only
 * holds references, so it is safe to capture from the async callbacks that
 * {@link me.playernguyen.opteco.runtime.AsyncExecutor#run} schedules back onto
 * the main thread; replies therefore always reach the sender on the right
 * thread.
 */
@Getter
@Accessors(fluent = true)
public final class CommandContext {

    /** Shared services used by the subcommand that receives this context. */
    private final CommandDependencies dependencies;

    /** Bukkit sender that issued the command; may be a player or the console. */
    @NotNull
    private final CommandSender sender;

    /** Arguments after the subcommand name, in the order the sender typed them. */
    private final String[] args;

    /**
     * Creates a context for a single command invocation.
     *
     * @param dependencies shared services the subcommand may use
     * @param sender the Bukkit sender that issued the command
     * @param args the arguments that follow the subcommand name
     */
    public CommandContext(
            @NotNull CommandDependencies dependencies, @NotNull CommandSender sender, @NotNull String[] args) {
        this.dependencies = dependencies;
        this.sender = sender;
        this.args = args;
    }

    /**
     * Returns an argument by position without throwing when it is absent.
     *
     * @param index zero-based position in the argument list
     * @return the argument at {@code index}, or {@code null} when it was not supplied
     */
    @Nullable
    public String arg(int index) {
        return index >= 0 && index < args.length ? args[index] : null;
    }

    /**
     * Tests whether at least the given number of arguments were supplied.
     *
     * @param count minimum number of arguments required
     * @return {@code true} when the argument list is that long or longer
     */
    public boolean hasArgs(int count) {
        return args.length >= count;
    }

    /**
     * Reports whether the sender is a player rather than the console.
     *
     * @return {@code true} when the sender is a {@link Player}
     */
    public boolean isPlayer() {
        return sender instanceof Player;
    }

    /**
     * Returns the sender as a player when possible.
     *
     * @return the sending player, or {@code null} when the sender is not a player
     */
    @Nullable
    public Player player() {
        return sender instanceof Player ? (Player) sender : null;
    }

    /**
     * Sends a localized message to the sender, coloured and interpolated.
     *
     * @param key message template to render
     * @param placeholders alternating placeholder names and values
     */
    public void reply(@NotNull MessageKey key, Object... placeholders) {
        dependencies.messages().send(sender, key, placeholders);
    }

    /**
     * Sends a localized message to the sender without colour or prefix
     * processing, used by list-style output such as the leaderboard.
     *
     * @param key message template to render
     * @param placeholders alternating placeholder names and values
     */
    public void replyPlain(@NotNull MessageKey key, Object... placeholders) {
        dependencies.messages().sendPlain(sender, key, placeholders);
    }

    /**
     * Translates an amount parsing failure into the matching player-facing
     * message, keeping the reason-to-message mapping in one place.
     *
     * @param exception the failure reported by {@link me.playernguyen.opteco.util.AmountParser}
     */
    public void replyAmountError(@NotNull AmountParseException exception) {
        switch (exception.reason()) {
            case NOT_A_NUMBER:
                reply(MessageKey.NOT_A_NUMBER, "value", exception.value());
                return;
            case NEGATIVE:
                reply(MessageKey.VALUE_NEGATIVE);
                return;
            case ZERO:
                reply(MessageKey.VALUE_ZERO);
                return;
            case MUST_BE_INTEGER:
                reply(MessageKey.MUST_BE_INTEGER);
                return;
            default:
                reply(MessageKey.INVALID_AMOUNT, "value", exception.value());
        }
    }

    /**
     * Reports an error to the sender using the most specific message available.
     *
     * <p>Unwraps {@link java.util.concurrent.CompletionException} so failures
     * raised inside an async task keep their original type, then maps
     * {@link PlayerNotFoundException}, {@link AmountParseException}, and
     * {@link StorageException} to their dedicated messages. Storage failures
     * are also logged. Anything else is logged and reported with
     * {@link MessageKey#ERROR_GENERIC}.
     *
     * @param error failure thrown by the subcommand or its async task
     */
    public void handleError(@NotNull Throwable error) {
        Throwable cause = error;
        if (cause instanceof java.util.concurrent.CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof PlayerNotFoundException) {
            reply(MessageKey.PLAYER_NOT_FOUND, "who", ((PlayerNotFoundException) cause).playerName());
            return;
        }
        if (cause instanceof AmountParseException) {
            replyAmountError((AmountParseException) cause);
            return;
        }
        if (cause instanceof StorageException) {
            dependencies.plugin().getLogger().warning("Storage error: " + cause.getMessage());
            reply(MessageKey.STORAGE_ERROR);
            return;
        }
        dependencies.plugin().getLogger().warning("Command failed: " + cause);
        reply(MessageKey.ERROR_GENERIC);
    }

    /**
     * Parses an argument as a money amount using the active configuration
     * rules, so integer-only and decimal modes behave the same everywhere.
     *
     * @param index zero-based position of the argument to parse
     * @param allowZero whether a zero amount is acceptable
     * @return the parsed, scale-normalized amount
     * @throws AmountParseException when the argument is missing, not a number, negative, or not an integer while
     *     decimals are disabled, or when it is zero and {@code allowZero} is {@code false}
     */
    @NotNull
    public BigDecimal amount(int index, boolean allowZero) {
        String raw = arg(index);
        if (raw == null) {
            throw new AmountParseException(AmountParseException.Reason.NOT_A_NUMBER, "");
        }
        return allowZero
                ? me.playernguyen.opteco.util.AmountParser.parseNonNegative(
                        raw, dependencies.config().current())
                : me.playernguyen.opteco.util.AmountParser.parsePositive(
                        raw, dependencies.config().current());
    }
}
