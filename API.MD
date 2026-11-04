# OptEco API

OptEco exposes `me.playernguyen.opteco.api.OptEcoAPI` through Bukkit's
`ServicesManager`. Add `OptEco` to your plugin's `softdepend` (or `depend`) and
look the service up after OptEco has enabled:

```java
Optional<OptEcoAPI> service =
        Bukkit.getServicesManager().load(OptEcoAPI.class);
if (service.isPresent()) {
    OptEcoAPI api = service.get();
    BigDecimal balance = api.getBalance(player.getUniqueId());
    player.sendMessage("Balance: " + api.getCurrencySymbol() + " " + balance);
}
```

The old `OptEco.getInstance().getApplicationInterface()` accessor remains as a
deprecated alias. New integrations should use the `ServicesManager` so they do
not depend on OptEco's plugin object or classloader.

## Amounts

All API amounts are `BigDecimal` and are normalized to eight decimal places.
Use `BigDecimal.ZERO`, not `0.0`, when comparing. `getBalance` performs a
blocking database read when the player is not cached; call
`getBalanceAsync` from the main thread:

```java
api.getBalanceAsync(player.getUniqueId())
        .thenAccept(balance -> Bukkit.getScheduler().runTask(
                plugin,
                () -> player.sendMessage("Balance: " + balance)));
```

`getCachedBalance` never touches the database and is suitable for placeholders
or scoreboard renderers. It returns zero when the player has not been loaded.

## Methods

```java
boolean hasAccount(UUID uuid);
BigDecimal getBalance(UUID uuid);
BigDecimal getCachedBalance(UUID uuid);
CompletableFuture<BigDecimal> getBalanceAsync(UUID uuid);
void setBalance(UUID uuid, BigDecimal amount);
void addBalance(UUID uuid, BigDecimal amount);
boolean takeBalance(UUID uuid, BigDecimal amount);
TransferResult transfer(UUID from, UUID to, BigDecimal amount);
boolean isPending(UUID uuid);
Optional<Transaction> getPendingTransaction(UUID uuid);
String getCurrencySymbol();
```

- `setBalance`, `addBalance`, and `takeBalance` write through the account cache
  and fire `OptEcoPointChangedEvent` after the database commit.
- `takeBalance` returns `false` when the amount is invalid or would take the
  balance below the configured minimum.
- `transfer` is atomic: the sender debit and receiver credit happen in one
  database transaction. It returns a `TransferResult` instead of throwing for
  expected business failures.
- `isPending` reports whether the player has a pending payment as the sender.
- `getPendingTransaction` returns an immutable `Transaction`.

## Events

`OptEcoPointChangedEvent` fires after a committed balance change and exposes
`player()`, `oldBalance()`, `newBalance()`, and `reason()`.

`OptEcoTransactionEvent` fires when a pending payment transitions to
`CONFIRMED`, `CANCELLED`, `EXPIRED`, or `FAILED`.

Both events are fired on the Bukkit main thread.

## Threading

- `hasAccount`, `getBalance`, `setBalance`, `addBalance`, `takeBalance`, and
  `transfer` may block on database I/O. Call them from an asynchronous task when
  possible.
- `getCachedBalance`, `isPending`, and `getPendingTransaction` are cache-only.
- PlaceholderAPI and shop hooks use the cache and never query the database from
  a placeholder expansion.

## Compatibility

The refactor is a breaking change: balances are now `BigDecimal`, the old
`VaultEconomyProvider`, `/ppto` importer, and PHP integration are not registered,
and the storage schema changed. Existing tables are migrated automatically.
Deprecated `double` convenience methods on `OptEcoAPI` are kept only to ease
migration; new code should use the `BigDecimal` methods.

## Maven/Gradle

JitPack coordinates remain the same:

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.PlayerNguyen:OptEco:Tag'
}
```
