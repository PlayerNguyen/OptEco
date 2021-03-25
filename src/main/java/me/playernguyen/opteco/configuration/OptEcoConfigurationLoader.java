package me.playernguyen.opteco.configuration;

public class OptEcoConfigurationLoader extends OptEcoLoaderAbstract {

    private static final String CONFIG_FILE = "config.yml";
    private static final String SYSTEM_LINE_SEPARATOR = System.lineSeparator();
    private static final String HEADER_HELPER =
            "[~] OptEco auto-generated plugin.yml" + SYSTEM_LINE_SEPARATOR +
                    "[-]  More help at https://github.com/PlayerNguyen/OptEco/wiki" + SYSTEM_LINE_SEPARATOR +
                    "[-]  Any issue at https://github.com/PlayerNguyen/OptEco/issues" + SYSTEM_LINE_SEPARATOR;

    public OptEcoConfigurationLoader() {
        super(CONFIG_FILE, HEADER_HELPER, "", false);
        saveDefault();
    }

    public Object getConfig(OptEcoConfiguration configuration) {
        if (!getConfiguration().contains(configuration.getPath())) {
            this.saveDefault();
            return getConfig(configuration);
        }
        return getConfiguration().get(configuration.getPath());
    }

    @Override
    public void saveDefault() {
        getConfiguration().options().copyDefaults(true);

        // Put default values if value is null
        for (OptEcoConfiguration c : OptEcoConfiguration.values()) {
            if (getConfiguration().get(c.getPath()) == null) {
                getConfiguration().addDefault(c.getPath(), c.getDefaultSetting());
            }
        }

        // Set header
        getConfiguration().options().header(this.getHeader());
        save();
    }
}
