package me.playernguyen.opteco.configuration;

import me.playernguyen.opteco.OptEco;
import me.playernguyen.opteco.OptEcoConfiguration;

import java.io.File;

public class ConfigurationLoader extends LoaderAbstract {

    private static final String CONFIG_FILE = "config.yml";

    private static final String HEADER_HELPER = "Need help? Visit: https://github.com/PlayerNguyen/OptEco for more helpful tips of config!";

    public ConfigurationLoader () {
        super(new File(OptEco.getInstance().getDataFolder(), CONFIG_FILE), HEADER_HELPER);
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
        return getConfiguration().getBoolean(configuration.getPath());
    }

    public float getFloat (OptEcoConfiguration configuration) {
        return (float) getConfiguration().getDouble(configuration.getPath());
    }

    public double getDouble (OptEcoConfiguration configuration) {
        return getConfiguration().getDouble(configuration.getPath());
    }

    public String getString (OptEcoConfiguration configuration) {
        return getConfiguration().getString(configuration.getPath());
    }

    public int getInt(OptEcoConfiguration configuration) {
        return getConfiguration().getInt(configuration.getPath());
    }

    @Override
    public void saveDefault() {

    }
}
