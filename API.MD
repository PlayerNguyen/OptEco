# API Using
OptEco was create by manually way, first of all you must set the `softdepends` or `depend` into your `config.yml` file of your plugin.

You can also check our [source code](src/me/playernguyen/api/IOptEcoAPI.java) for the API.

# ExamplePlugin.java
## Soft Depends
```java
public class ExamplePlugin extends JavaPlugin {

    private static boolean optEcoEnable;

    public void onEnable() {
        if (Bukkit.getServer().getPluginManager().getPlugin("OptEco") != null) {
            optEcoEnable = true;
        } else {
            // On not exist
            optEcoEnable = false;
            this.getLogger().severe("Can't found OptEco. Please install at https://www.spigotmc.org/resources/76179/.");
        
        }
    }

    public static boolean isOptEcoEnable() {
        return optEcoEnable;
    }
}
```
## Depends
```java
public class ExamplePlugin extends JavaPlugin {

    private static boolean optEcoEnable;

    public void onEnable() {
        if (Bukkit.getServer().getPluginManager().getPlugin("OptEco") != null) {
            optEcoEnable = true;
        } else {
            // On not exist
            optEcoEnable = false;
            this.getLogger().severe("Can't found OptEco. Please install at https://www.spigotmc.org/resources/76179/.");
            // Disable plugin 
            this.getPluginLoader().disablePlugin(this);
        }
    }

    public static boolean isOptEcoEnable() {
        return optEcoEnable;
    }
}
```
## Example PlayerJoinEvent
```java
public class ExamplePlugin implements Listener {
    @EventHandler public void onPlayerJoinEvent(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (ExamplePlugin.isOptEcoEnable()) {
            IOptEcoAPI api = new OptEcoAPI(player);
            event.setJoinMessage("Welcome, you have " + api.getPoints() + " " + api.getCurrencySymbol() + " in your account!");
        }
    }
}
```
