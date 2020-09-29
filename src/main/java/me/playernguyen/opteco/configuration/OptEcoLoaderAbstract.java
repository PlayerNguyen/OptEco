package me.playernguyen.opteco.configuration;

import me.playernguyen.opteco.OptEcoConfiguration;
import me.playernguyen.opteco.OptEcoImplementation;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public abstract class OptEcoLoaderAbstract extends OptEcoImplementation {

    private File file;
    private YamlConfiguration configuration;

    private String header;

//    public LoaderAbstract(File file) {
//        this.file = file;
//        this.configuration = YamlConfiguration.loadConfiguration(file);
//        this.header = "";
//    }
//
//    public LoaderAbstract(String filename) {
//        this.file = new File(getPlugin().getDataFolder(), filename + ".yml");
//        this.configuration = YamlConfiguration.loadConfiguration(file);
//        this.header = "";
//    }
//
//    public LoaderAbstract(String filename, String header) {
//        this.file = new File(getPlugin().getDataFolder(), filename + ".yml");
//        this.configuration = YamlConfiguration.loadConfiguration(file);
//        this.header = header;
//    }

    public OptEcoLoaderAbstract(String name, String header, String parent, boolean fromResource) {
        // Parent handle
        // /OptEco folder
        File dataFolder = getPlugin().getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdir()) {
            throw new NullPointerException("Data folder of plugin not found: " + dataFolder.getPath());
        }
        // Parent folder
        File parentFolder = (!parent.equals("")) ? new File(dataFolder, parent) : dataFolder;
        if (!parentFolder.exists() && !parentFolder.mkdir()) {
            throw new NullPointerException("Parent folder not found: " + parentFolder.getPath());
        }
        // Load file, configuration and header
        InputStream stream = getPlugin().getResource(name);
        if (fromResource && stream == null) {
            throw new NullPointerException("Resource not found: " + name);
        }
        this.file = new File(parentFolder, name);
        this.configuration = (fromResource) ?
                YamlConfiguration.loadConfiguration(new InputStreamReader(stream)) :
                YamlConfiguration.loadConfiguration(file);
        this.header = header;
        save();
    }

    public String getHeader() {
        return header;
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

    public boolean getBool(OptEcoConfiguration configuration) {
        return getConfiguration().getBoolean(configuration.getPath());
    }

    public float getFloat(OptEcoConfiguration configuration) {
        return (float) getConfiguration().getDouble(configuration.getPath());
    }

    public double getDouble(OptEcoConfiguration configuration) {
        return getConfiguration().getDouble(configuration.getPath());
    }

    public String getString(OptEcoConfiguration configuration) {
        return getConfiguration().getString(configuration.getPath());
    }

    public int getInt(OptEcoConfiguration configuration) {
        return getConfiguration().getInt(configuration.getPath());
    }

}
