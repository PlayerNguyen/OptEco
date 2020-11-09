package me.playernguyen.opteco.configuration;

import me.playernguyen.opteco.OptEcoLanguage;
import org.bukkit.ChatColor;

import java.io.IOException;

public class OptEcoLanguageLoader extends OptEcoLoaderAbstract {

    public static final char COLOR_SYMBOL = '&';

    public OptEcoLanguageLoader(String file) throws IOException {
//        super(new File(OptEco.getInstance().getDataFolder(), file));
        super(file, "Auto-generated language by OptEco", "language", false);
        saveDefault();
    }

    public void saveDefault() throws IOException {
        this.getConfiguration().options().copyDefaults(true);

        for (OptEcoLanguage dc : OptEcoLanguage.values())
            this.getConfiguration().addDefault(dc.getPath(), dc.getDefaultSetting());

        this.getConfiguration().save(getFile());
    }

    public String getRawLanguage(OptEcoLanguage optEcoLanguage) {
        return this.getConfiguration().getString(optEcoLanguage.getPath());
    }


    public String getLanguage(OptEcoLanguage optEcoLanguage) {
        return ChatColor.translateAlternateColorCodes(COLOR_SYMBOL, this.getRawLanguage(optEcoLanguage));
    }

}
