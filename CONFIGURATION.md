# OptEco configuration

`config.yml` is generated in `plugins/OptEco/` on first start. The file is
validated at startup; an invalid value disables the plugin with a clear error
instead of silently falling back to defaults.

## Update

```yaml
Update:
  CheckForUpdate: true
```

OptEco checks the latest GitHub release (`PlayerNguyen/OptEco`) asynchronously.
The check has short timeouts and never blocks startup.

## Debug

```yaml
Debug: false
```

Reserved for verbose diagnostic logging. Do not leave it on in production.

## Settings.CurrencySymbol

```yaml
Settings:
  CurrencySymbol: points
```

The label appended to balances and placeholders. It can contain color codes.

## Settings.Preferences

```yaml
Settings:
  Preferences:
    Language: en.yml
    BeginPoint: 0
    MinimumReachPoint: -15
    UseDouble: false
    LimitTopCommand: 5
    TabExecutor: true
```

- `Language` — file under `plugins/OptEco/language/`. Missing keys are filled
  from the built-in defaults.
- `BeginPoint` — balance inserted when an account is created.
- `MinimumReachPoint` — lowest balance a `take` or transfer may leave.
- `UseDouble` — when false, command amounts must be whole numbers. Internally
  balances are always exact decimals.
- `LimitTopCommand` — number of leaderboard rows.
- `TabExecutor` — enable/disable command tab completion.

## Settings.Storage

```yaml
Settings:
  Storage:
    Type: sqlite
```

Allowed values: `sqlite`, `mysql`. Storage changes require a restart.

## Settings.SQL

```yaml
Settings:
  SQL:
    SQLite:
      File: account.sqlite
    MySQL:
      Host: localhost
      Port: 3306
      Database: dbname
      Username: root
      Password: ''
      Parameter: 'useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8&serverTimezone=UTC'
    Table:
      Accounts: opteco_accounts
      Transactions: opteco_transactions
```

- The SQLite file is relative to `plugins/OptEco/`.
- MySQL table names must match `[A-Za-z0-9_]+`; values are always sent through
  prepared statements.
- The bundled jar includes the SQLite and MySQL JDBC drivers.
- `SchemaMigrator` imports the legacy `opteco` / `opteco_transaction` tables when
  they are found. The old tables are kept as a backup.

## Settings.Payment

```yaml
Settings:
  Payment:
    Pending:
      Duration: 15
      Countdown:
        Enable: true
        Type: message
```

- `Duration` — seconds before a pending payment expires.
- `Countdown.Type` — `message` or `title`.

## Settings.Effect.TitleCounter

```yaml
Settings:
  Effect:
    TitleCounter:
      Enable: true
      Stay: 20
      FadeOut: 100
      Speed: 5
      Limit: 10000
```

The balance-change title animation is cosmetic. `Limit` caps how large a change
is animated; larger changes jump to the final balance.

## Reloading

`/points reload` reloads `config.yml` and the language file and regenerates the
message cache. It does **not** recreate database connections or change tables,
files, or storage type; those changes require a server restart.
