package me.playernguyen.command;

import me.playernguyen.OptEco;
import me.playernguyen.OptEcoLanguage;

public enum CommandArguments {

    AMOUNT, PLAYER;

    public String asString(OptEco plugin) {
        switch (this){
            case AMOUNT: return plugin.getLanguageLoader().getLanguage(OptEcoLanguage.VAR_AMOUNT);
            case PLAYER: return plugin.getLanguageLoader().getLanguage(OptEcoLanguage.VAR_PLAYER);
            default: return "";
        }
    }

}
