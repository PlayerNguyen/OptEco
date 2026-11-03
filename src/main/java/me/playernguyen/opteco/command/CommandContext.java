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

@Getter
@Accessors(fluent = true)
public final class CommandContext {

    private final CommandDependencies dependencies;
    private final CommandSender sender;
    private final String[] args;

    public CommandContext(
            @NotNull CommandDependencies dependencies, @NotNull CommandSender sender, @NotNull String[] args) {
        this.dependencies = dependencies;
        this.sender = sender;
        this.args = args;
    }

    @Nullable
    public String arg(int index) {
        return index >= 0 && index < args.length ? args[index] : null;
    }

    public boolean hasArgs(int count) {
        return args.length >= count;
    }

    public boolean isPlayer() {
        return sender instanceof Player;
    }

    @Nullable
    public Player player() {
        return sender instanceof Player ? (Player) sender : null;
    }

    @NotNull
    public CommandSender sender() {
        return sender;
    }

    public void reply(@NotNull MessageKey key, Object... placeholders) {
        dependencies.messages().send(sender, key, placeholders);
    }

    public void replyPlain(@NotNull MessageKey key, Object... placeholders) {
        dependencies.messages().sendPlain(sender, key, placeholders);
    }

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
