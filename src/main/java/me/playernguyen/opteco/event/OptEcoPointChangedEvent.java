package me.playernguyen.opteco.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class OptEcoPointChangedEvent extends OptEcoEvent {

    private final UUID who;
    private final double oldPoint;
    private final double newPoint;
    private final ModifyType type;

    public OptEcoPointChangedEvent(UUID who, double oldPoint, double newPoint, ModifyType type) {
        this.who = who;
        this.oldPoint = oldPoint;
        this.newPoint = newPoint;
        this.type = type;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return super.getHandlers();
    }

    public double getNewPoint() {
        return newPoint;
    }

    public double getOldPoint() {
        return oldPoint;
    }

    public UUID getWho() {
        return who;
    }

    public ModifyType getType() {
        return type;
    }

    public enum ModifyType {
        INCREASE, DECREASE
    }
}
