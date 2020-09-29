package me.playernguyen.opteco.configuration;

import me.playernguyen.opteco.OptEcoImplementation;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public abstract class LoaderAbstract extends OptEcoImplementation {

    private File file;
    private YamlConfiguration configuration;

    private String header;

    public LoaderAbstract(File file) {
        this.file = file;
        this.configuration = YamlConfiguration.loadConfiguration(file);
        this.header = "";
    }

    public LoaderAbstract(String filename) {
        this.file = new File(getPlugin().getDataFolder(), filename + ".yml");
        this.configuration = YamlConfiguration.loadConfiguration(file);
        this.header = "";
    }

    public LoaderAbstract(String filename, String header) {
        this.file = new File(getPlugin().getDataFolder(), filename + ".yml");
        this.configuration = YamlConfiguration.loadConfiguration(file);
        this.header = header;
    }

    public LoaderAbstract(File file, String header) {
        this.file = file;
        this.configuration = YamlConfiguration.loadConfiguration(file);
        this.header = header;
    }


    public void setFile(File file) {
        this.file = file;
    }

    public void setConfiguration(YamlConfiguration configuration) {
        this.configuration = configuration;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public abstract void saveDefault();

    public File getFile() {
        return file;
    }

    public YamlConfiguration getConfiguration() {
        return configuration;
    }

    public void reload() {
        this.configuration = YamlConfiguration.loadConfiguration(file);
    }

    public boolean save() {
        try {
            this.getConfiguration().save(getFile());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}
