![alt text][logo]

[logo]: https://raw.githubusercontent.com/PlayerNguyen/OptEco/0be367354df5f3341272e27604bfe7afe66a26ea/logo/Banner.png "Logo"


OptEco - Spigot Points System with API and web integration
* Support MySQL 
* PlaceholderAPI support
* Fully commands and permissions
* Pending payment (confirm/cancel before transfer)
* [API support](#)
* Website Integration.
* bStats (Metrics) 
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

# Configuration
## Config.yml
Visit to [configuration page](CONFIGURATION.md) for more information. 

## Language
Visit to [language page](language/README.md) for more information.

# API
At this time, API wasn't tested in every version. It will test and release asap

# PlaceholderAPI (1.2b)
Using parameter `%opteco_points%` to get player point

#Links:
* Discord: Player_Nguyen#5200
* [bStat](https://bstats.org/plugin/bukkit/OptEco/6793)