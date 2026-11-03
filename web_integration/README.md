# OptEco PHP Web Integration (Deprecated)

> **Deprecated as of November 2026. Do not deploy this directory.**
>
> This PHP integration is outdated, unmaintained, and contains security
> vulnerabilities. It is kept only for historical reference and may be removed
> without notice. No fixes or security patches will be provided.

## Why it is deprecated

This code was written for PHP 7 and an older OptEco schema. It is not integrated
with the plugin's account cache, transaction system, or event lifecycle.

Known problems:

- **Unsafe SQL construction.** Queries are assembled with string concatenation
  and `addslashes()` instead of prepared statements. `OptEcoAPI::getAccounts()`
  also accepts a raw SQL condition. Any caller that passes user-controlled input
  can read or modify arbitrary data.
- **No authentication or authorization.** The shipped scripts implement no
  login, permissions, CSRF protection, or rate limiting. Anyone who can reach a
  deployment can read or change balances.
- **Direct database writes bypass the plugin.** Changes are invisible to the
  online-player cache and skip OptEco's transaction history, events, and
  validation. They can be overwritten or leave balances inconsistent.
- **Credentials in the web root.** `api/APIConfig.php` stores the MySQL user and
  password in PHP source that is tracked by Git and usually served from the
  website directory.
- **Information disclosure.** Database errors are returned to the client through
  `die()`.
- **Unsupported runtime.** PHP 7 is end-of-life, and the bundled Mojang helper
  uses legacy API endpoints. There are no tests and the integration receives no
  maintenance.

## If it is already deployed

1. Remove this directory from the web root, or block access to it at the web
   server or reverse proxy.
2. Rotate the MySQL credentials used by `api/APIConfig.php`.
3. Do not expose the OptEco MySQL database to the internet. Use a firewall and a
   dedicated, least-privilege database user.
4. Audit balance changes made through the web integration; direct writes may
   have bypassed the plugin's cache and transaction history.

## What to use instead

- **Gameplay and administration:** use the `/points` (`/pts`, `/p`) commands and
  their permissions. See the
  [commands and permissions wiki](https://github.com/PlayerNguyen/OptEco/wiki/Commands-&-Permissions).
- **Plugin development:** use the Java `OptEcoAPI` instead of writing to the
  database. See [API.MD](../API.MD).
- **Placeholders and scoreboards:** use the PlaceholderAPI integration.
- **Website leaderboards or account pages:** build a separate, read-only
  integration against a database replica or a dedicated export. Use a read-only
  database user, prepared statements, authentication, rate limiting, and
  caching. Never let a public website write directly to the OptEco account
  table.

If you must keep using this code, treat it as untrusted legacy code: isolate it,
keep it off the public internet, convert it to prepared statements, move
credentials out of source control, and accept that it remains unsupported.

## License

Same as the rest of OptEco; see [LICENSE](../LICENSE).
