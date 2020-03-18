package me.playernguyen.configuration;

import me.playernguyen.OptEco;
import me.playernguyen.OptEcoConfiguration;

import java.io.File;

public class ConfigurationLoader extends LoaderAbstract {

    private static final String CONFIG_FILE = "config.yml";

    private static final String HEADER_HELPER = "Wanna help? https://google.com";

    public ConfigurationLoader (OptEco plugin) {
        super(plugin, new File(plugin.getDataFolder(), CONFIG_FILE), HEADER_HELPER);
        saveDefault();

    }

    public Object getConfig(OptEcoConfiguration configuration) {
        if ( !getConfiguration().contains(configuration.getPath()) ) {
            this.saveDefault();
            return getConfig(configuration);
        }
        return getConfiguration().get(configuration.getPath());
    }

    public boolean getBool (OptEcoConfiguration configuration) {
        return (boolean) getConfig(configuration);
    }

    public float getFloat (OptEcoConfiguration configuration) {
        return (float) getConfig(configuration);
    }

    public double getDouble (OptEcoConfiguration configuration) {
        return (double) getConfig(configuration);
    }

    public String getString (OptEcoConfiguration configuration) {
        return (String) getConfig(configuration);
    }

    public int getInt(OptEcoConfiguration configuration) {
        return (Integer) getConfig(configuration);
    }

}
