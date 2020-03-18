![alt text][logo]

[logo]: https://raw.githubusercontent.com/PlayerNguyen/OptEco/0be367354df5f3341272e27604bfe7afe66a26ea/logo/Banner.png "Logo"


OptEco - Spigot Points System with API and web integration
* Support MySQL 
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
Hm...with this system I though is must be clear and great so we take a whole time to create this. All config are in `OptEco` folder.
### Config.yml
#### 🔷 language: lang
This is the language file, it can be any string type like `en_lang`, `uk_lang`,...
You can visit [language](#) to download it or you can [share](#) your language for everybody
#### 🔷  debug: false
Opps, switch this section on will display all of your MySQL execute command, processing. Warning! This section can leak player OptEco account information.
#### 🔷 checkForUpdate: true
New update will be display while starting server. Update or not, your decision.
#### 🔷 settings.startBalance: 0.0
At first join, it will set to the value which are set.
#### 🔷 settings.minimumBalance: -15.0
The lowest points that player can reach. Set it to `0` if you don't want they owe your server :)
#### 🔷 settings.timeToConfirmPayment: 15
Time to confirm or cancel the transaction, it must greater than 1 or player cannot confirm/cancel.
#### 🔷 settings.currencySymbol: points
The currency symbol to display. It can be `$` (dollar sign) or bla bla bla...
#### 🔷 settings.storeType: YAML
If you want to contain player account by using MySQL, change it to `MYSQL`. Or `YAML` if you want to contain player account with yaml file.
##### 🔷 settings.mysql.host: localhost
Address of MySQL Server.
##### 🔷 settings.mysql.database: dbname
Database of MySQL Server
##### 🔷 settings.mysql.port: 3306
Port of MySQL Server
##### 🔷 settings.mysql.username: root
Username of MySQL Server
##### 🔷 settings.mysql.password: 
Password of MySQL Server
##### 🔷 settings.mysql.tableName: OptEco
Table of MySQL which containing all player account

# API
At this time, API wasn't tested in every version. It will test and release asap

#Links:
* 
