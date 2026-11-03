package me.playernguyen.opteco.message;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum MessageKey {
    PREFIX("PREFIX", "&e[OptEco]"),
    NO_PERMISSION("NO_PERMISSIONS", "&cYou do not have permission to do that."),
    COMMAND_NOT_FOUND("COMMAND_NOT_FOUND", "&cCommand not found. Use &e/points help&c."),
    FOR_PLAYER("FOR_PLAYER", "&cThis command can only be used by a player."),
    STORAGE_ERROR("STORAGE_ERROR", "&cA storage error occurred. Please try again."),
    ERROR_GENERIC("ERROR_GENERIC", "&cAn unexpected error occurred."),
    ACCOUNT_NOT_FOUND("ACCOUNT_NOT_FOUND", "&cNo account exists for &e%who%&c."),
    PLAYER_NOT_FOUND("COMMAND.PLAYER_NOT_FOUND", "&cPlayer &e%who%&c was not found."),
    TARGET_NOT_FOUND("COMMAND.TARGET_NOT_FOUND", "&cPlayer &e%who%&c is not online."),

    NOT_A_NUMBER("COMMAND.VAR_NOT_A_NUMBER", "&cValue &e%value%&c is not a number."),
    VALUE_NEGATIVE("COMMAND.VAR_VALUE_CANNOT_BE_NEGATIVE", "&cThe amount cannot be negative."),
    VALUE_ZERO("COMMAND.VAR_VALUE_MUST_BE_POSITIVE", "&cThe amount must be greater than zero."),
    MUST_BE_INTEGER("COMMAND.VAR_VALUE_MUST_BE_INTEGER", "&cThe amount must be an integer."),
    INVALID_AMOUNT("COMMAND.VAR_INVALID_AMOUNT", "&cThe amount &e%value%&c is invalid."),

    ADD_DESCRIPTION("COMMAND.ADD.DESCRIPTION", "add points to a player"),
    ADD_SUCCESS("COMMAND.ADD.SUCCEEDED", "&aAdded &d%value% %currency% &ato &d%who%&a."),
    ADD_FAILED("COMMAND.ADD.FAILED", "&cCould not add &d%value% %currency% &ato &d%who%&c."),

    TAKE_DESCRIPTION("COMMAND.TAKE.DESCRIPTION", "take points from a player"),
    TAKE_SUCCESS("COMMAND.TAKE.SUCCESS", "&aTook &d%value% %currency% &afrom &d%who%&a."),
    TAKE_FAILED("COMMAND.TAKE.FAIL", "&cCould not take &d%value% %currency% &afrom &d%who%&c."),
    TAKE_NOT_ENOUGH("COMMAND.TAKE.NOT_ENOUGH", "&cThat player does not have enough points."),

    SET_DESCRIPTION("COMMAND.SET.DESCRIPTION", "set a player's points"),
    SET_SUCCESS("COMMAND.SET.SUCCESS", "&aSet &d%who%&a's balance to &d%value% %currency%&a."),
    SET_FAILED("COMMAND.SET.FAIL", "&cCould not set &d%who%&a's balance to &d%value% %currency%&c."),

    CHECK_DESCRIPTION("COMMAND.CHECK.DESCRIPTION", "check a player's points"),
    CHECK_SELF("COMMAND.CHECK.SELF", "&aYour balance is &d%value% %currency%&a."),
    CHECK_ANOTHER("COMMAND.CHECK.ANOTHER", "&aPlayer &d%who%&a has &d%value% %currency%&a."),

    ME_DESCRIPTION("COMMAND.ME.DESCRIPTION", "show your own points"),

    PAY_DESCRIPTION("COMMAND.PAY.DESCRIPTION", "send points to another player"),
    PAY_ON_TRANSACTION(
            "COMMAND.PAY.PAY_ON_TRANSACTION",
            "&cYou already have a pending payment. Use &e/points confirm&c or " + "&e/points cancel&c."),
    PAY_CONFIRM_DISPLAY(
            "COMMAND.PAY.CONFIRM_DISPLAY",
            "&aYou have &d%value% seconds &ato confirm this payment. Use "
                    + "&e/points confirm&a or &e/points cancel&a."),
    PAY_OUT_OF_TIME("COMMAND.PAY.OUT_OF_TIME_CONFIRM", "&cYour pending payment has expired."),
    PAY_NOT_ENOUGH("COMMAND.PAY.NOT_ENOUGH", "&cYou do not have enough points."),
    PAY_CANNOT_SELF("COMMAND.PAY.PAY_CANNOT_SELF_TRANSFER", "&cYou cannot pay yourself."),
    PAY_TRANSACTION_NOT_EXIST("COMMAND.PAY.PAY_TRANSACTION_NOT_EXIST", "&cYou have no pending payment."),
    PAY_SUCCESS("COMMAND.PAY.ACCEPTED", "&aSent &d%value% %currency% &ato &d%who%&a."),
    PAY_SUCCESS_TARGET("COMMAND.PAY.ACCEPTED_TARGET", "&aYou received &d%value% %currency% &afrom &d%who%&a."),
    PAY_FAILED("COMMAND.PAY.FAILED", "&cThe payment could not be completed."),
    PAY_CANCELLED("COMMAND.PAY.DENY", "&cThe payment was cancelled."),
    PAY_TARGET_OFFLINE("COMMAND.PAY.TARGET_OFFLINE", "&cThe receiver must be online to receive a payment."),

    CONFIRM_DESCRIPTION("COMMAND.CONFIRM.DESCRIPTION", "confirm your pending payment"),
    CANCEL_DESCRIPTION("COMMAND.CANCEL.DESCRIPTION", "cancel your pending payment"),

    TOP_DESCRIPTION("COMMAND.TOP.DESCRIPTION", "show the leaderboard"),
    TOP_HEADER("COMMAND.TOP.HEADER", "&8----- &6OptEco Top &8-----"),
    TOP_FORMAT("COMMAND.TOP.FORMAT", "&7%id%. &6%name% &8- &d%balance% %currency%"),
    TOP_EMPTY("COMMAND.TOP.EMPTY", "&7No accounts yet."),
    TOP_FOOTER("COMMAND.TOP.FOOTER", "&8---------------------"),

    RELOAD_DESCRIPTION("COMMAND.RELOAD.DESCRIPTION", "reload OptEco configuration"),
    RELOAD_DONE("COMMAND.RELOAD.DONE", "&aConfiguration and language reloaded."),
    RELOAD_RESTART_REQUIRED(
            "COMMAND.RELOAD.RESTART_REQUIRED", "&eStorage settings changed. Restart the server to apply them."),

    HELP_DESCRIPTION("COMMAND.HELP.DESCRIPTION", "show this help"),
    HELP_HEADER("COMMAND.HELP.HEADER", "&8----- &6OptEco Commands &8-----"),
    HELP_FORMAT("COMMAND.HELP.FORMAT", "&e%usage% &8- &7%description%"),
    USAGE("COMMAND.USAGE", "&cUsage: &e%usage%"),

    COUNTDOWN_FORMAT("COUNTDOWN_FORMAT", "&8%second%..."),

    TITLE_INCREASE("EFFECT.TITLE.INCREASE.TITLE", "&a+ &8%current% &a+"),
    TITLE_INCREASE_SUB("EFFECT.TITLE.INCREASE.SUBTITLE", "&c^ &6%remain% &c^"),
    TITLE_DECREASE("EFFECT.TITLE.DECREASE.TITLE", "&c- &8%current% &c-"),
    TITLE_DECREASE_SUB("EFFECT.TITLE.DECREASE.SUBTITLE", "&c^ &6%remain% &c^");

    private final String path;
    private final String defaultValue;

    MessageKey(String path, String defaultValue) {
        this.path = path;
        this.defaultValue = defaultValue;
    }
}
