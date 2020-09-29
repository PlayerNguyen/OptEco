package me.playernguyen.opteco;

public enum OptEcoLanguage {

    PREFIX("PREFIX", "&e[OptEco]"),

    NO_PERMISSION("NO_PERMISSIONS", "&cYou have no permission to require this action."),
    COMMAND_NOT_FOUND("COMMAND_NOT_FOUND", "&cCommand not found."),
    FOR_PLAYER_COMMAND("FOR_PLAYER", "&cThis command just for player, console cannot use this."),

    VAR_AMOUNT("COMMAND.VAR_AMOUNT", "<amount>"),
    VAR_NOT_A_NUMBER("COMMAND.VAR_NOT_A_NUMBER", "&cValue %value% is not a number."),
    VAR_PLAYER("COMMAND.VAR_PLAYER", "<player>"),
    VALUE_CANNOT_BE_NEGATIVE("COMMAND.VAR_VALUE_CANNOT_BE_NEGATIVE", "&cThe amount cannot be negative."),
    VAR_PLAYER_NOT_FOUND("COMMAND.PLAYER_NOT_FOUND", "&cCannot found player %who% in server or database."),

    COMMAND_DESCRIBE_ADD("COMMAND.ADD.DESCRIPTION", "add balance to player"),
    COMMAND_SUCCEEDED_ADD("COMMAND.ADD.SUCCEEDED", "&aSucceeded add &d%value% %currency% &ainto &d%who% &aaccount."),
    COMMAND_FAILED_ADD("COMMAND.ADD.FAILED", "&cFailed to add &d%value% %currency% &ainto &d%who% &aaccount."),

    COMMAND_DESCRIBE_CHECK("COMMAND.CHECK.DESCRIPTION", "check player balance"),
    COMMAND_DESCRIBE_SELFCHECK("COMMAND.ME.DESCRIPTION", "check my balance"),
    CHECK_SELF("COMMAND.CHECK.SELF", "&aYour balance now is &d%value% %currency%"),
    CHECK_ANOTHER("COMMAND.CHECK.ANOTHER", "&aPlayer %who% balance now is &d%value% %currency%"),

    COMMAND_DESCRIBE_TOP("COMMAND.TOP.DESCRIPTION", "view top points"),

    COMMAND_DESCRIBE_PAY("COMMAND.PAY.DESCRIPTION", "transfer points to player"),
    PAY_ON_TRANSACTION("COMMAND.PAY.PAY_ON_TRANSACTION", "&cYou are on transaction with another player, please using &d/points [confirm/cancel] &cto confirm your before transaction."),
    PAY_CONFIRM_DISPLAY("COMMAND.PAY.CONFIRM_DISPLAY", "&aYou have &d%value% seconds &ato confirm, please using &d/points [confirm/cancel] &ato confirm your transaction."),
    PAY_OUT_OF_TIME_CONFIRM("COMMAND.PAY.OUT_OF_TIME_CONFIRM", "&cYou are out of time to confirm your transaction."),
    PAY_NOT_ENOUGH("COMMAND.PAY.NOT_ENOUGH", "&cNot enough to pay."),
    PAY_CANNOT_SELF_TRANSFER("COMMAND.PAY.PAY_CANNOT_SELF_TRANSFER", "&cYou cannot transfer to yourself."),
    PAY_TRANSACTION_NOT_EXIST("COMMAND.PAY.PAY_TRANSACTION_NOT_EXIST", "&cYou have not paid for everybody."),
    PAY_SUCCESS("COMMAND.PAY.ACCEPTED", "&aTransfer &d%value% %currency% &ato &d%who%."),
    PAY_SUCCESS_TARGET("COMMAND.PAY.ACCEPTED_TARGET", "&aYou has been received &d%value% %currency% &aby &d%who%"),
    PAY_FAILED("COMMAND.PAY.FAILED", "&cHaving an error while pending transferred."),
    PAY_DENY("COMMAND.PAY.DENY", "&cYou are denied the transaction."),

    COMMAND_DESCRIBE_RELOAD("COMMAND.RELOAD.DESCRIPTION", "reload plugin"),
    RELOAD_DONE("COMMAND.RELOAD.DONE", "&aReloaded all config."),

    COMMAND_DESCRIBE_TAKE("COMMAND.TAKE.DESCRIPTION", "take player points"),
    TAKE_SUCCESS("COMMAND.TAKE.SUCCESS", "&aTaking &d%value% %currency% &afrom &d%who%"),
    TAKE_FAIL("COMMAND.TAKE.FAIL", "&cCannot take &d%value% %currency% &afrom &d%who% &cbecause of the error!"),
    TAKE_NOT_ENOUGH("COMMAND.TAKE.NOT_ENOUGH", "&cYou cannot take out of limitation."),

    COMMAND_DESCRIBE_SET("COMMAND.SET.DESCRIPTION", "set player points"),
    SET_SUCCESS("COMMAND.SET.SUCCESS", "&aSetting &d%value% %currency% &ato &d%who%"),
    SET_FAIL("COMMAND.SET.FAIL", "&cCannot set &d%value% %currency% &ato &d%who% &cbecause of the error!"),

    COUNTDOWN_FORMAT("COUNTDOWN_FORMAT", "&8%second%..."),

    COMMAND_DESCRIBE_CONFIRM("COMMAND.CONFIRM.DESCRIPTION", "confirm transaction"),
    COMMAND_DESCRIBE_CANCEL("COMMAND.CANCEL.DESCRIPTION", "cancel transaction"),

    RED_BAR("RED_BAR", "&c---------------------------"),
    GRAY_BAR("RED_BAR", "&7---------------------------"),
    COMMAND_TOP_FORMAT("COMMAND.TOP.FORMAT", "&7%id%. &6%name%  ~  &c%balance%");

    private final String path;
    private final Object wh;

    OptEcoLanguage(String path, Object wh) {
        this.path = path;
        this.wh = wh;
    }

    public String getPath() {
        return path;
    }

    public Object getDefaultSetting() {
        return wh;
    }

}
