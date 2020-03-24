package me.playernguyen.configuration;

import me.playernguyen.OptEco;
import me.playernguyen.OptEcoLanguage;
import org.bukkit.ChatColor;

import java.io.File;
import java.io.IOException;

public class LanguageLoader extends LoaderAbstract {

    public static final char COLOR_SYMBOL = '&';


    public LanguageLoader(String file, OptEco plugin) {
        super(plugin, new File(plugin.getDataFolder(), file +".yml"));
        saveDefault();
    }

    public void saveDefault() {
        this.getConfiguration().options().copyDefaults(true);

        for (OptEcoLanguage dc : OptEcoLanguage.values())
            this.getConfiguration().addDefault(dc.getPath(), dc.getDefaultSetting());

        try {
            this.getConfiguration().save(getFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getRawLanguage(OptEcoLanguage optEcoLanguage) {
        return this.getConfiguration().getString(optEcoLanguage.getPath());
    }

    public String getLanguage(OptEcoLanguage optEcoLanguage) {
        return ChatColor.translateAlternateColorCodes(COLOR_SYMBOL, this.getRawLanguage(optEcoLanguage));
    }

}
