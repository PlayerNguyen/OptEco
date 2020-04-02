# API Using

~~OptEco was create by manually way~~, first of all you must set the `softdepends` or `depend` into your `config.yml` file of your plugin.
You can also check our [source code](src/me/playernguyen/api/IOptEcoAPI.java) for the API.

# Import
[![](https://jitpack.io/v/PlayerNguyen/OptEco.svg)](https://jitpack.io/#PlayerNguyen/OptEco)

These is many way to import OptEco into your project. But by the way, you can follow this link to see the newest version and easy-to-use guide
* Jitpack.io: https://jitpack.io/#PlayerNguyen/OptEco
## Using Maven
Put this into your `pom.xml`. Also check your PaperMC/Spigot source.
````
<repositories>
        <!-- OptEco Repo -->
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
</repositories>

<dependencies>
    <!-- OptEco -->
    <!-- Remember to change the version tag to needed version -->
    <dependency>
            <groupId>com.github.PlayerNguyen</groupId>
            <artifactId>OptEco</artifactId>
            <version>Tag</version>
        </dependency>
</dependencies>
````
## Using Gradle
Add it in your root build.gradle at the end of repositories:

```
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```
And then add the dependency: 
```
dependencies {
    implementation 'com.github.PlayerNguyen:OptEco:v1.14.6b'
}
```
## Manual Import
Download the OptEco.jar and then put it into your project
* [InteliJ Import](https://www.jetbrains.com/help/idea/library.html#add-library-to-module-dependencies)
* [Eclipse Import](https://javabeat.net/eclipse-user-library/)

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
