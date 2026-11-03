package me.playernguyen.opteco.core.exception;

public class PlayerNotFoundException extends RuntimeException {

    private final String playerName;

    public PlayerNotFoundException(String playerName) {
        super("Player not found: " + playerName);
        this.playerName = playerName;
    }

    public String playerName() {
        return playerName;
    }
}
