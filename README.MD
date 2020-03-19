![alt text][logo]

[logo]: https://raw.githubusercontent.com/PlayerNguyen/OptEco/0be367354df5f3341272e27604bfe7afe66a26ea/logo/Banner.png "Logo"


OptEco - Spigot Points System with API and web integration
* Support MySQL 
* PlaceholderAPI support
* Fully commands and permissions
* Pending payment (confirm/cancel before transfer)
* API Support
* Website Integration.
* bStats (Metrics) 
# Usage
OptEco is easy-to-use Spigot/Bukkit/Paper plugin that get points from database(YAML, MySQL,...) 
# Commands and permissions
You can use `points` instead of `opteco`.

| Command         | Usage                           | Permissions Group             |   
|-----------------|------------------------         |-------------------------------|
|`/points pay`    | Transfer points                 | `admin`, `user`,`everyone`    |
|`/points confirm`| Confirm the transfer            | `admin`, `user`,`everyone`    |
|`/points cancel` | Cancel the transfer             | `admin`, `user`,`everyone`    |
|`/points me`     | Check player account            | `admin`, `user`,`everyone`    |
|`/points add`    | Add point into player account   | `admin`, `everyone`           |
|`/points set`    | Set point for player            | `admin`, `everyone`           |
|`/points take`   | Take the point from player      | `admin`, `everyone`           |
|`/points check`  | Check the point of player       | `admin`, `everyone`           |
|`/points reload` | Reload the plugin               | `admin`, `everyone`           |

Each command have one permission like `opteco.pay`, `opteco.confirm`...
#### Group permissions   
* `everyone` is `opteco.*`
* `admin` is `opteco.admin`
* `user` is `opteco.user`

# Configurations
## Config.yml
Visit to [configuration page](CONFIGURATION.md) for more information. 

## Language
Visit to [language page](language/README.md) for more information.

# [API](API.md)


## PlaceholderAPI (1.2b)
From 1.2b version of plugin, OptEco added PlaceholderAPI for server

|Parameters         |Info                   |   From    |
|-------------------|-------------------    |----------:|
|%opteco_points%    | Get player points     |1.2b       | 
|%opteco_version%   | Get plugin version    |1.3b       |


## Website Integration
Check out our example website integration PHP [here](web_integration).

# Links:
* Discord: Player_Nguyen#5200
* [bStat](https://bstats.org/plugin/bukkit/OptEco/6793)
* [Spigot](https://www.spigotmc.org/resources/opteco-placeholderapi-mysql-support-api-web-integration-points-system.76179/)

Having any issues? Don't be shy, [create an issue now](https://github.com/PlayerNguyen/OptEco/issues).