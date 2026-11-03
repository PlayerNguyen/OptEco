# AGENTS.md

Guidance for AI coding agents working on OptEco.

## Project overview

OptEco is a Minecraft (Bukkit / Spigot / Paper) plugin that provides a player
points/currency economy: balances, admin adjustments, player-to-player payments,
leaderboards, placeholders, shop integrations, and a public Java API.

The project is being revived. It is a historical codebase, so treat existing
code as intentional unless you can prove otherwise. Prefer small, focused
changes over large rewrites.

## Build and verification

- Canonical build: `./gradlew clean build`
- Quick compile: `./gradlew compileJava`
- Output JAR: `build/libs/`
- The Gradle wrapper is Gradle 6.7 and the project targets **Java 8**.
- `pom.xml` is a stale legacy file. Use Gradle; do not trust its version or
  dependency list.
- The plugin version lives in `build.gradle` and is injected into
  `src/main/resources/plugin.yml` through `ReplaceTokens`. Do not hardcode it.
- There is currently no automated test suite. Verify changes by compiling and,
  for behavior changes, manually testing on a supported Paper/Spigot server
  (SQLite storage is the easiest setup).

## Repository tooling

- Bun is used only for repository tooling; it is not a runtime dependency of the
  plugin.
- `bun test` runs the PR validator tests in `scripts/check-pr/index.test.ts`.
- `bun run check:pr` validates the current PR title, body, and commit subjects.
  CI runs both commands in `.github/workflows/pr-check.yml`.
- Pull requests must use `.github/pull_request_template.md`. Add the
  `skip-pr-check` label only for bot PRs or exceptional cases.

## Architecture map

- `src/main/java/me/playernguyen/opteco/OptEco.java` — plugin entry point and
  wiring. Preserve the `onEnable()` setup order: loaders/configuration must be
  ready before storage, commands, listeners, accounts, and integrations.
- `OptEcoImplementation.java` — shared accessors used by plugin components.
- `account/` — account model, SQL balance operations, SQLite/MySQL backends, and
  the in-memory `OptEcoCacheAccountManager`.
- `configuration/` — config key enum, YAML loader, storage type enum.
- `language/` — language key enum and loader. User-facing strings belong here.
- `command/` — `/opteco` (aliases `/points`, `/pts`, `/p`) and `/ppto`.
  `OptEcoCommand` constructs and registers subcommands.
- `transaction/` — pending payment flow, SQL transaction history, confirm/cancel.
- `event/` — custom Bukkit events for balance changes and transactions.
- `listener/` — join/quit account cache handling and balance-change effects.
- `api/` — public `OptEcoAPI` plus PlaceholderAPI, MVdWPlaceholderAPI, Vault,
  BossShopPro, and ShopGUIPlus adapters.
- `sql/` — JDBC connection and table-creation abstraction (SQLite and MySQL).
- `schedule/` — countdown and title-counter tasks.
- `web_integration/` — standalone PHP client that reads/writes the MySQL
  account table directly. It is not part of the Java build.

## Conventions

- Use **Java 8** language features only. Do not introduce newer APIs.
- Add new configuration values to `OptEcoConfiguration` (path + default) and
  read them through the configuration loader. Never hardcode config paths.
- Add user-facing messages to `OptEcoLanguage` and `language/lang.yml`, then use
  `MessageFormat` / the language loader. Do not scatter raw strings.
- Add permissions to `OptEcoPermission` and `plugin.yml`, including the
  `opteco.admin` / `opteco.user` children when appropriate.
- Register new subcommands in the `OptEcoCommand` constructor and implement
  `onPlayerCommand`, `onConsoleCommand`, `onRemoteConsoleCommand`, and `onTab`.
- Route balance changes through `IAccountDatabase` so change events are fired
  and persisted. For online players, refresh the cache afterward.
- Keep `OptEcoAPI` source-compatible: other plugins compile against it.
- Bukkit API calls (events, player messages, scheduling) must follow the
  existing main-thread/async patterns. Database work may block; avoid adding
  new blocking work to the server thread where possible.
- Match the surrounding style: 4-space indentation, descriptive names, and small
  classes in the existing package structure.

## Commit convention

OptEco follows [Conventional Commits 1.0.0](https://www.conventionalcommits.org/en/v1.0.0/).
Clear, consistent commit messages make the history easier to review and let us
generate release notes later.

### Format

```text
<type>(<scope>): <description>

[optional body]

[optional footer(s)]
```

- The **type** is required and lowercase.
- The **scope** is optional but recommended for larger areas.
- The **description** is required, imperative, lowercase, and has no trailing
  period.
- Keep the first line at **72 characters or fewer**.
- Wrap body and footer lines at **72 characters**.
- Use one commit per logical change. Do not mix refactors with behavior changes.

### Types

| Type | Use for |
|---|---|
| `feat` | A new user-facing feature |
| `fix` | A bug fix |
| `docs` | Documentation only |
| `style` | Formatting or whitespace; no behavior change |
| `refactor` | Code restructuring; no feature or bug fix |
| `perf` | Performance improvement |
| `test` | Adding or fixing tests |
| `build` | Build system, Gradle, or dependency changes |
| `ci` | CI/CD configuration |
| `chore` | Maintenance that does not fit another type |
| `revert` | Reverting a previous commit |

### Scopes

Scopes are optional. Use one of these when the change is concentrated in an
area, or omit the scope for project-wide changes.

`account`, `api`, `command`, `config`, `language`, `sql`, `transaction`,
`web`, `build`, `docs`

### Examples

```text
feat(command): add /points transfer alias
fix(account): refresh cache after offline take
docs: document MySQL setup
build: update Gradle wrapper to 6.7
refactor(transaction): extract countdown task
```

With a body and issue reference:

```text
fix(account): prevent negative balance overflow

Clamp the balance before persisting and refresh the cache for online
players after the update.

Closes #42
```

### Breaking changes

Mark a breaking change with `!` after the type/scope and add a
`BREAKING CHANGE:` footer:

```text
feat(api)!: remove deprecated getPlugin accessor

BREAKING CHANGE: consumers must use OptEco.getInstance() instead.
```

### Issue references

- Use `Closes #123` or `Fixes #123` in the footer to close an issue when the
  commit or PR is merged.
- Use `Refs #123` for related work that should not close the issue.

### Reverts

Use the `revert` type and reference the reverted commit:

```text
revert: feat(api): add legacy balance setter

This reverts commit 1234abcd.
```

### Verification

- Pull requests are checked by `.github/workflows/pr-check.yml` using
  `scripts/check-pr/index.ts`. The check validates the PR title, the PR
  template, and every non-merge commit subject.
- Run the validator locally with `bun test` and `bun run check:pr` (set
  `PR_TITLE` and `PR_BODY` when running outside GitHub Actions).
- Use the `skip-pr-check` label only for bot PRs or exceptional cases.
- Keep commit messages in English, even if discussion or documentation is in
  another language.

## Important gotchas

- `StorageType` supports only `SQLITE` and `MYSQL`. YAML account storage is
  documented in older files but is not implemented.
- `VaultEconomyProvider` exists but is not registered (`hookWithVault()` is
  empty). Do not assume Vault support works.
- `TransactionStorageYaml` and `OptEcoTransactionLoader` are unused legacy code.
  Leave them alone unless a task explicitly targets them.
- The account cache (`OptEcoCacheAccountManager`) is the source of truth for
  online-player reads. External/database changes are not visible until the cache
  refreshes.
- Table names come from configuration and are interpolated into SQL. Do not
  interpolate untrusted user input into SQL; use prepared statements for values.
- Payments are **pending** until `/points confirm`; they expire and auto-cancel.
  Test confirm, cancel, and timeout paths when touching transactions.
- `plugin.yml` declares `loadbefore: [BossShopPro]` and soft-depends on
  PlaceholderAPI, BossShopPro, MVdWPlaceholderAPI, and ShopGUIPlus. Optional
  integrations must stay optional and null-safe.
- The PHP web integration writes directly to the MySQL table and duplicates the
  plugin's schema assumptions. Update both sides when the schema changes.

## Change checklist

1. `./gradlew clean build` succeeds.
2. New config keys have sensible defaults and appear in generated `config.yml`.
3. New messages exist in every shipped language file when reasonable.
4. New permissions are declared in `plugin.yml`.
5. Manual smoke test: join, `/points me`, admin `add`/`take`/`set`,
   `/points top`, a `pay` + `confirm`/`cancel`, and `/points reload`.
6. No secrets or server-specific credentials are committed (especially
   `web_integration/api/APIConfig.php`).

## Remote and workflow notes

- Default branch: `master`.
- Repository: `PlayerNguyen/OptEco` on GitHub.
- Follow the [commit convention](#commit-convention) for all commit messages.
- Fill in `.github/pull_request_template.md` when opening a pull request.
- Update `README.MD`, `CONFIGURATION.md`, or `API.MD` when behavior or public
  API changes.
