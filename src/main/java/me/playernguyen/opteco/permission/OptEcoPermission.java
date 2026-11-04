package me.playernguyen.opteco.permission;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Permission nodes checked by the command layer, mirroring the entries
 * declared in {@code plugin.yml}.
 *
 * <p>{@link #ADMIN} and {@link #USER} are parent groups that grant their
 * children; the remaining constants correspond one-to-one with subcommands and
 * are handed to {@link me.playernguyen.opteco.command.SubCommand}. Enum
 * constants are immutable and safe to reference from any thread.
 */
@Getter
@Accessors(fluent = true)
public enum OptEcoPermission {
    /** Parent group granting every administrator subcommand. */
    ADMIN("opteco.admin"),
    /** Parent group granting the default player-facing subcommands. */
    USER("opteco.user"),
    /** Allows {@code /points add}. */
    ADD("opteco.add"),
    /** Allows {@code /points take}. */
    TAKE("opteco.take"),
    /** Allows {@code /points set}. */
    SET("opteco.set"),
    /** Allows {@code /points check}. */
    CHECK("opteco.check"),
    /** Allows {@code /points me}. */
    ME("opteco.me"),
    /** Allows {@code /points pay}. */
    PAY("opteco.pay"),
    /** Allows {@code /points confirm}. */
    CONFIRM("opteco.confirm"),
    /** Allows {@code /points cancel}. */
    CANCEL("opteco.cancel"),
    /** Allows {@code /points top}. */
    TOP("opteco.top"),
    /** Allows {@code /points reload}. */
    RELOAD("opteco.reload");

    /** Raw node string passed to {@code CommandSender.hasPermission}. */
    private final String node;

    /**
     * Binds a constant to its node string.
     *
     * @param node raw permission node, for example {@code opteco.add}
     */
    OptEcoPermission(String node) {
        this.node = node;
    }
}
