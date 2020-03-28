package me.playernguyen.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class OptEcoPlayerReceivedEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private UUID sender;
    private UUID receiver;
    private double balance;

    public OptEcoPlayerReceivedEvent(UUID sender, UUID receiver, double balance) {
        this.sender = sender;
        this.receiver = receiver;
        this.balance = balance;
    }

    public UUID getSender() {
        return sender;
    }

    public UUID getReceiver() {
        return receiver;
    }

    public double getBalance() {
        return balance;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
