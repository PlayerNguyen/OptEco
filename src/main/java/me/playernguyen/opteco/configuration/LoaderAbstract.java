package me.playernguyen.opteco.configuration;

import me.playernguyen.opteco.OptEco;
import me.playernguyen.opteco.OptEcoConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class LoaderAbstract {

    private OptEco plugin;
    private File file;
    private YamlConfiguration configuration;

    private String header;

    public LoaderAbstract(OptEco plugin, File file) {
        this.plugin = plugin;
        this.file = file;
        this.configuration = YamlConfiguration.loadConfiguration(file);
        this.header = "";
    }

    public LoaderAbstract(OptEco plugin, File file, String header) {
        this.plugin = plugin;
        this.file = file;
        this.configuration = YamlConfiguration.loadConfiguration(file);
        this.header = header;
    }

    public void saveDefault() {

        this.getConfiguration().options().copyDefaults(true);
        this.getConfiguration().options().header(header);

        for (OptEcoConfiguration dc : OptEcoConfiguration.values()) {
            this.getConfiguration().addDefault(dc.getPath(), dc.getDefaultSetting());
        }

        try {
            this.getConfiguration().save(file);
        } catch (IOException e) {
            getPlugin().getDebugger().printException(e);
        }
    }

    public File getFile() {
        return file;
    }

    public YamlConfiguration getConfiguration() {
        return configuration;
    }

    public OptEco getPlugin() {
        return plugin;
    }


    public void reload() {
        this.configuration = YamlConfiguration.loadConfiguration(file);
    }

}
