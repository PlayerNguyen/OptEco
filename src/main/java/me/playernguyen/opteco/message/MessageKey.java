package me.playernguyen.opteco.message;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Catalogue of the user-facing texts rendered by OptEco.
 *
 * <p>Each constant pairs its path inside the language file with the text used
 * when that path is missing. {@link MessageService} writes missing entries into
 * the configured language file and replaces {@code %name%} placeholders when it
 * formats a message. The enum is immutable, so constants can be shared between
 * the main thread and asynchronous callbacks.</p>
 */
@Getter
@Accessors(fluent = true)
public enum MessageKey {
    /** Chat prefix prepended by {@link MessageService#format}. */
    PREFIX("PREFIX", "&e[OptEco]"),
    /** Shown when the sender lacks the permission for a subcommand. */
    NO_PERMISSION("NO_PERMISSIONS", "&cYou do not have permission to do that."),
    /** Shown when the typed subcommand does not exist. */
    COMMAND_NOT_FOUND("COMMAND_NOT_FOUND", "&cCommand not found. Use &e/points help&c."),
    /** Shown when a player-only subcommand is invoked from the console. */
    FOR_PLAYER("FOR_PLAYER", "&cThis command can only be used by a player."),
    /** Shown when a database operation fails with a storage error. */
    STORAGE_ERROR("STORAGE_ERROR", "&cA storage error occurred. Please try again."),
    /** Fallback for unexpected failures that have no more specific message. */
    ERROR_GENERIC("ERROR_GENERIC", "&cAn unexpected error occurred."),
    /** Rendered with {@code %who%} when no account exists for the resolved player. */
    ACCOUNT_NOT_FOUND("ACCOUNT_NOT_FOUND", "&cNo account exists for &e%who%&c."),
    /** Rendered with {@code %who%} when a player name cannot be resolved at all. */
    PLAYER_NOT_FOUND("COMMAND.PLAYER_NOT_FOUND", "&cPlayer &e%who%&c was not found."),
    /** Rendered with {@code %who%} when the target player is not online. */
    TARGET_NOT_FOUND("COMMAND.TARGET_NOT_FOUND", "&cPlayer &e%who%&c is not online."),

    /** Rendered with {@code %value%} when an amount is not numeric. */
    NOT_A_NUMBER("COMMAND.VAR_NOT_A_NUMBER", "&cValue &e%value%&c is not a number."),
    /** Shown when an amount is negative. */
    VALUE_NEGATIVE("COMMAND.VAR_VALUE_CANNOT_BE_NEGATIVE", "&cThe amount cannot be negative."),
    /** Shown when an amount is zero where a positive value is required. */
    VALUE_ZERO("COMMAND.VAR_VALUE_MUST_BE_POSITIVE", "&cThe amount must be greater than zero."),
    /** Shown when a fractional amount is used while {@code UseDouble} is disabled. */
    MUST_BE_INTEGER("COMMAND.VAR_VALUE_MUST_BE_INTEGER", "&cThe amount must be an integer."),
    /** Rendered with {@code %value%} for otherwise invalid amounts. */
    INVALID_AMOUNT("COMMAND.VAR_INVALID_AMOUNT", "&cThe amount &e%value%&c is invalid."),

    /** Help text for {@code /points add}. */
    ADD_DESCRIPTION("COMMAND.ADD.DESCRIPTION", "add points to a player"),
    /** Rendered with {@code %value%}, {@code %currency%} and {@code %who%} after a successful add. */
    ADD_SUCCESS("COMMAND.ADD.SUCCEEDED", "&aAdded &d%value% %currency% &ato &d%who%&a."),
    /** Rendered with {@code %value%}, {@code %currency%} and {@code %who%} when an add fails. */
    ADD_FAILED("COMMAND.ADD.FAILED", "&cCould not add &d%value% %currency% &ato &d%who%&c."),

    /** Help text for {@code /points take}. */
    TAKE_DESCRIPTION("COMMAND.TAKE.DESCRIPTION", "take points from a player"),
    /** Rendered with {@code %value%}, {@code %currency%} and {@code %who%} after a successful take. */
    TAKE_SUCCESS("COMMAND.TAKE.SUCCESS", "&aTook &d%value% %currency% &afrom &d%who%&a."),
    /** Rendered with {@code %value%}, {@code %currency%} and {@code %who%} when a take fails. */
    TAKE_FAILED("COMMAND.TAKE.FAIL", "&cCould not take &d%value% %currency% &afrom &d%who%&c."),
    /** Shown when a take would push the target below the configured minimum. */
    TAKE_NOT_ENOUGH("COMMAND.TAKE.NOT_ENOUGH", "&cThat player does not have enough points."),

    /** Help text for {@code /points set}. */
    SET_DESCRIPTION("COMMAND.SET.DESCRIPTION", "set a player's points"),
    /** Rendered with {@code %who%}, {@code %value%} and {@code %currency%} after a successful set. */
    SET_SUCCESS("COMMAND.SET.SUCCESS", "&aSet &d%who%&a's balance to &d%value% %currency%&a."),
    /** Rendered with {@code %who%}, {@code %value%} and {@code %currency%} when a set fails. */
    SET_FAILED("COMMAND.SET.FAIL", "&cCould not set &d%who%&a's balance to &d%value% %currency%&c."),

    /** Help text for {@code /points check}. */
    CHECK_DESCRIPTION("COMMAND.CHECK.DESCRIPTION", "check a player's points"),
    /** Rendered with {@code %value%} and {@code %currency%} when a player checks their own balance. */
    CHECK_SELF("COMMAND.CHECK.SELF", "&aYour balance is &d%value% %currency%&a."),
    /** Rendered with {@code %who%}, {@code %value%} and {@code %currency%} for another player's balance. */
    CHECK_ANOTHER("COMMAND.CHECK.ANOTHER", "&aPlayer &d%who%&a has &d%value% %currency%&a."),

    /** Help text for {@code /points me}. */
    ME_DESCRIPTION("COMMAND.ME.DESCRIPTION", "show your own points"),

    /** Help text for {@code /points pay}. */
    PAY_DESCRIPTION("COMMAND.PAY.DESCRIPTION", "send points to another player"),
    /** Shown when the sender already has an unexpired pending payment. */
    PAY_ON_TRANSACTION(
            "COMMAND.PAY.PAY_ON_TRANSACTION",
            "&cYou already have a pending payment. Use &e/points confirm&c or " + "&e/points cancel&c."),
    /** Rendered with {@code %value%} holding the remaining seconds to confirm the payment. */
    PAY_CONFIRM_DISPLAY(
            "COMMAND.PAY.CONFIRM_DISPLAY",
            "&aYou have &d%value% seconds &ato confirm this payment. Use "
                    + "&e/points confirm&a or &e/points cancel&a."),
    /** Shown when a pending payment expired before it was confirmed. */
    PAY_OUT_OF_TIME("COMMAND.PAY.OUT_OF_TIME_CONFIRM", "&cYour pending payment has expired."),
    /** Shown when the sender's balance cannot cover the payment. */
    PAY_NOT_ENOUGH("COMMAND.PAY.NOT_ENOUGH", "&cYou do not have enough points."),
    /** Shown when the sender tries to pay themselves. */
    PAY_CANNOT_SELF("COMMAND.PAY.PAY_CANNOT_SELF_TRANSFER", "&cYou cannot pay yourself."),
    /** Shown when confirm or cancel is used without a pending payment. */
    PAY_TRANSACTION_NOT_EXIST("COMMAND.PAY.PAY_TRANSACTION_NOT_EXIST", "&cYou have no pending payment."),
    /** Rendered with {@code %value%}, {@code %currency%} and {@code %who%} after the sender confirms a payment. */
    PAY_SUCCESS("COMMAND.PAY.ACCEPTED", "&aSent &d%value% %currency% &ato &d%who%&a."),
    /** Rendered with {@code %value%}, {@code %currency%} and {@code %who%} to the receiving player. */
    PAY_SUCCESS_TARGET("COMMAND.PAY.ACCEPTED_TARGET", "&aYou received &d%value% %currency% &afrom &d%who%&a."),
    /** Shown when the payment could not be completed. */
    PAY_FAILED("COMMAND.PAY.FAILED", "&cThe payment could not be completed."),
    /** Shown when the sender cancels a pending payment. */
    PAY_CANCELLED("COMMAND.PAY.DENY", "&cThe payment was cancelled."),
    /** Shown when the receiver is not online to accept the payment. */
    PAY_TARGET_OFFLINE("COMMAND.PAY.TARGET_OFFLINE", "&cThe receiver must be online to receive a payment."),

    /** Help text for {@code /points confirm}. */
    CONFIRM_DESCRIPTION("COMMAND.CONFIRM.DESCRIPTION", "confirm your pending payment"),
    /** Help text for {@code /points cancel}. */
    CANCEL_DESCRIPTION("COMMAND.CANCEL.DESCRIPTION", "cancel your pending payment"),

    /** Help text for {@code /points top}. */
    TOP_DESCRIPTION("COMMAND.TOP.DESCRIPTION", "show the leaderboard"),
    /** Header printed before the leaderboard rows. */
    TOP_HEADER("COMMAND.TOP.HEADER", "&8----- &6OptEco Top &8-----"),
    /** Leaderboard row rendered with {@code %id%}, {@code %name%}, {@code %balance%} and {@code %currency%}. */
    TOP_FORMAT("COMMAND.TOP.FORMAT", "&7%id%. &6%name% &8- &d%balance% %currency%"),
    /** Shown when the leaderboard has no accounts. */
    TOP_EMPTY("COMMAND.TOP.EMPTY", "&7No accounts yet."),
    /** Footer printed after the leaderboard rows. */
    TOP_FOOTER("COMMAND.TOP.FOOTER", "&8---------------------"),

    /** Help text for {@code /points reload}. */
    RELOAD_DESCRIPTION("COMMAND.RELOAD.DESCRIPTION", "reload OptEco configuration"),
    /** Shown when the configuration and language files reload successfully. */
    RELOAD_DONE("COMMAND.RELOAD.DONE", "&aConfiguration and language reloaded."),
    /** Shown when storage settings changed and only a restart can apply them. */
    RELOAD_RESTART_REQUIRED(
            "COMMAND.RELOAD.RESTART_REQUIRED", "&eStorage settings changed. Restart the server to apply them."),

    /** Help text for {@code /points help}. */
    HELP_DESCRIPTION("COMMAND.HELP.DESCRIPTION", "show this help"),
    /** Header printed before the command list. */
    HELP_HEADER("COMMAND.HELP.HEADER", "&8----- &6OptEco Commands &8-----"),
    /** Help row rendered with {@code %usage%} and {@code %description%}. */
    HELP_FORMAT("COMMAND.HELP.FORMAT", "&e%usage% &8- &7%description%"),
    /** Usage line rendered with {@code %usage%}. */
    USAGE("COMMAND.USAGE", "&cUsage: &e%usage%"),

    /** Countdown notice rendered with {@code %second%} for each remaining second. */
    COUNTDOWN_FORMAT("COUNTDOWN_FORMAT", "&8%second%..."),

    /** Balance title shown while the balance increases; rendered with {@code %current%}. */
    TITLE_INCREASE("EFFECT.TITLE.INCREASE.TITLE", "&a+ &8%current% &a+"),
    /** Subtitle shown while the balance increases; rendered with {@code %remain%}. */
    TITLE_INCREASE_SUB("EFFECT.TITLE.INCREASE.SUBTITLE", "&c^ &6%remain% &c^"),
    /** Balance title shown while the balance decreases; rendered with {@code %current%}. */
    TITLE_DECREASE("EFFECT.TITLE.DECREASE.TITLE", "&c- &8%current% &c-"),
    /** Subtitle shown while the balance decreases; rendered with {@code %remain%}. */
    TITLE_DECREASE_SUB("EFFECT.TITLE.DECREASE.SUBTITLE", "&c^ &6%remain% &c^");

    /** Path inside the language YAML file. */
    private final String path;

    /** Text written when the path is missing; may contain {@code %name%} placeholders. */
    private final String defaultValue;

    /**
     * Creates a message key.
     *
     * @param path path inside the language YAML file
     * @param defaultValue text written when the path is missing
     */
    MessageKey(String path, String defaultValue) {
        this.path = path;
        this.defaultValue = defaultValue;
    }
}
