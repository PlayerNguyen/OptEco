package me.playernguyen;

public abstract class OptEcoObject {

    private OptEco plugin;

    public OptEcoObject (OptEco plugin) {
        this.plugin = plugin;
    }

    public OptEco getPlugin() {
        return plugin;
    }

}
