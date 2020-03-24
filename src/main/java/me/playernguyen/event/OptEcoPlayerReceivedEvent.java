package me.playernguyen.event;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class OptEcoPlayerReceivedEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private CommandSender sender;
    private Player receiver;
    private double balance;

    public OptEcoPlayerReceivedEvent(CommandSender sender, Player receiver, double balance) {
        this.sender = sender;
        this.receiver = receiver;
        this.balance = balance;
    }

    public CommandSender getSender() {
        return sender;
    }

    public Player getReceiver() {
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
