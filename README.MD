![alt text][logo]

[logo]: https://raw.githubusercontent.com/PlayerNguyen/OptEco/0be367354df5f3341272e27604bfe7afe66a26ea/logo/Banner.png "Logo"

[![](https://jitpack.io/v/PlayerNguyen/OptEco.svg)](https://jitpack.io/#PlayerNguyen/OptEco)

OptEco is a player points/economy plugin for Bukkit, Spigot, and Paper servers.

- SQLite and MySQL storage with automatic migration from the old schema
- Exact `BigDecimal` balances (stored as scaled 64-bit integers)
- Admin balance commands and a leaderboard
- Pending player-to-player payments with confirm, cancel, and timeout
- PlaceholderAPI, MVdWPlaceholderAPI, BossShopPro, and ShopGUIPlus hooks
- Public `OptEcoAPI` registered through Bukkit's `ServicesManager`
- bStats metrics and a GitHub release update check

## Requirements

- **Java 8 or newer** at runtime
- **Minecraft 1.13 or newer** (Paper or Spigot)
- No external MySQL driver or SQLite driver installation is required; both are
  bundled in the OptEco jar

## Installation

1. Drop the OptEco jar into `plugins/`.
2. Start the server once to generate `config.yml` and `language/en.yml`.
3. Choose `sqlite` or `mysql` under `Settings.Storage.Type`.
4. Restart the server after changing storage settings.

Existing installations are migrated automatically. The old `opteco` and
`opteco_transaction` tables are imported into the new schema and left in place as
a backup.

## Commands

All commands use `/opteco`; aliases are `/points`, `/pts`, and `/p`.

| Command | Permission | Description |
|---|---|---|
| `/points help` | - | Show available commands |
| `/points me` | `opteco.me` | Show your own balance |
| `/points check [player]` | `opteco.check` | Show your or another player's balance |
| `/points top` | `opteco.top` | Show the leaderboard |
| `/points pay <player> <amount>` | `opteco.pay` | Start a pending payment to an online player |
| `/points confirm` | `opteco.confirm` | Confirm your pending payment |
| `/points cancel` | `opteco.cancel` | Cancel your pending payment |
| `/points add <player> <amount>` | `opteco.add` | Add points to a player |
| `/points take <player> <amount>` | `opteco.take` | Take points from a player |
| `/points set <player> <amount>` | `opteco.set` | Set a player's balance |
| `/points reload` | `opteco.reload` | Reload config and language files |

`opteco.admin` grants all administrator commands and `opteco.user` grants the
default player commands. See `plugin.yml` for the complete permission tree.

## Configuration

See [CONFIGURATION.md](CONFIGURATION.md) and the generated `config.yml`.

Storage settings require a restart after they are changed. `/points reload`
reloads messages, limits, and the currency symbol without disabling the plugin;
if it detects a storage change it tells the sender that a restart is required.

## Website integration

The PHP integration in [`web_integration/`](web_integration) is deprecated and
writes directly to MySQL. It is not part of the Java build and is not compatible
with the new schema without a follow-up migration. Do not deploy it.

## Building

```bash
./gradlew clean build
```

The build runs JUnit tests, Spotless formatting checks, and SpotBugs. Use:

```bash
./gradlew spotlessApply
./gradlew spotbugsMain
```

The output jar is in `build/libs/`.

To boot a local test server with the plugin installed:

```bash
./gradlew runServer
```

This uses the [runServer](https://plugins.gradle.org/plugin/com.undefinedcreations.runServer)
Gradle plugin to start Purpur 1.20.4 (Paper-compatible) in `run/PURPUR/` with
2 GB of RAM and the EULA pre-accepted. The folder is git-ignored. Stop the
server with `stop` in the console. Purpur is used instead of PaperMC because
runServer 0.1.8 downloads Paper through the retired `api.papermc.io` v2 API,
which now returns HTTP 410.

To run the automated server smoke test instead:

```bash
./gradlew jar
bun run smoke:server
```

It boots Purpur in `build/smoke-server/`, installs the built jar, drives the
console through a few commands, and fails on packaging, class-loader or startup
errors. CI runs it weekly, on release tags and on demand. The MySQL integration
tests are skipped unless `OPTECO_TEST_MYSQL_HOST` is set; the `mysql` CI job
provides a MySQL 8 service for them.

## Links

- [JavaDoc](https://playernguyen.github.io/OptEco/me/playernguyen/opteco/api/OptEcoAPI.html)
- [Spigot Resource](https://www.spigotmc.org/resources/76179/)
- [API guide](API.MD)
- [Configuration guide](CONFIGURATION.md)

## License

Copyright (c) Player_Nguyen. Licensed under the [MIT license](LICENSE).
