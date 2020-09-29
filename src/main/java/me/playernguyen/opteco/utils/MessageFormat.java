package me.playernguyen.opteco.utils;

import me.playernguyen.opteco.OptEcoImplementation;
import me.playernguyen.opteco.OptEcoLanguage;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

public class MessageFormat extends OptEcoImplementation {

    private final char SPACE_CHARACTER = ' ';
    private final char START_LIST_CHARACTER = '>';

    public MessageFormat() {
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

    public String numberFormat(double d) {
        if (d % 1 == 0) {
            return String.valueOf((int) d);
        }
        return String.format("%.2f", d);
    }

}
