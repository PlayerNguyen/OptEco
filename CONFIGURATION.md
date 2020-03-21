# Config.yml
#### 🔷 language: lang
This is the language file, it can be any string type like `en_lang`, `uk_lang`,...
You can visit [language](https://github.com/PlayerNguyen/OptEco/tree/master/language) to download it or 
you can [share](#) your language for everybody
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
#### 🔷 settings.storeType: SQLITE
If you want to contain player account by using MySQL, change it to `MYSQL`. Or `YAML` if you want to contain player account with yaml file.

| Key | Value |
|-----|-------|
|YAML (not advise)| Use .yaml config to contain player data|
|SQLITE| Use .sqlite file to contain player data
|MYSQL| Use MySQL server to contain player data|
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
##### 🔷 settings.sqlite.file: account.sqlite
SQLite file which contain player data/account