package me.playernguyen.opteco.core.exception;

/**
 * Signals that a player reference could not be resolved.
 *
 * <p>Commands and player resolvers throw this unchecked exception when the
 * supplied name does not match an online or cached player. The command layer
 * catches it and reports {@code PLAYER_NOT_FOUND} using {@link #playerName()}.</p>
 */
public class PlayerNotFoundException extends RuntimeException {

    /** Name that could not be resolved. */
    private final String playerName;

    /**
     * Creates the exception for a missing player name.
     *
     * @param playerName name that could not be resolved
     */
    public PlayerNotFoundException(String playerName) {
        super("Player not found: " + playerName);
        this.playerName = playerName;
    }

    /**
     * Returns the name that could not be resolved.
     *
     * @return unresolved player name
     */
    public String playerName() {
        return playerName;
    }
}
