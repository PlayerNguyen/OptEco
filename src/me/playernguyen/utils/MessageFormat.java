package me.playernguyen.utils;

import me.playernguyen.OptEco;
import me.playernguyen.OptEcoLanguage;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

public class MessageFormat {

    private final char SPACE_CHARACTER = ' ';
    // Maybe: ✱
    private final char START_LIST_CHARACTER = '>';
    private final OptEco plugin;

    public MessageFormat (OptEco plugin) {
        this.plugin = plugin;
    }

    public OptEco getPlugin() {
        return plugin;
    }

    public String format(String string) {
        return getPlugin().getLanguageLoader().getLanguage(OptEcoLanguage.PREFIX)
                + SPACE_CHARACTER
                + ChatColor.RESET + string;
    }

    public void sendCuteList(CommandSender commandSender, ArrayList<String> strings, ChatColor color, char startChar) {
        for (String string : strings) {
            commandSender.sendMessage(color + "" + startChar + SPACE_CHARACTER + string);
        }
    }

    public void sendCuteList(CommandSender commandSender, ArrayList<String> strings, ChatColor chatColor) {
        sendCuteList(commandSender, strings, chatColor, START_LIST_CHARACTER);
    }

    public void sendCuteList(CommandSender commandSender, ArrayList<String> strings) {
        sendCuteList(commandSender, strings, ChatColor.RESET, START_LIST_CHARACTER);
    }

}
