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
- The Gradle wrapper is Gradle 8.14.6 and runs on JDK 21. Plugin source compiles
  for Java 8 via `options.release = 8`; tests compile for Java 17 because JUnit 6
  requires it.
- `gradle/wrapper/gradle-wrapper.jar` is tracked intentionally; keep the
  `.gitignore` exception so `./gradlew` works in a fresh clone.
- The JAR bundles HikariCP, SQLite JDBC, and MySQL Connector/J; plugin and
  server APIs are `compileOnly`.
- The build is Gradle-only. The legacy Maven `pom.xml` was removed; do not
  reintroduce it.
- The plugin version is injected into `src/main/resources/plugin.yml` through
  `ReplaceTokens`. Set it with `-Pversion=X.Y.Z` or `VERSION`; local builds
  default to `0.0.0-dev`.
- Releases are built by `.github/workflows/release.yml` from `vX.Y.Z` tags.
- Java tests use JUnit 6 under `src/test/java`. Run `./gradlew test` (or
  `./gradlew clean build`, which runs tests as part of `build`).
- CI runs `./gradlew clean build` on JDK 21 in
  `.github/workflows/java-tests.yml`.
- `build` also runs static analysis. Spotless enforces Palantir Java Format
  (4-space indentation, 120 columns); run `./gradlew spotlessApply` before
  committing. SpotBugs analyzes the compiled classes; run
  `./gradlew spotbugsMain` and read `build/reports/spotbugs/main.html`.
- `config/spotbugs/exclude.xml` contains the documented baseline for
  dependency-injection and vendored-code findings. Fix the issue and delete the
  matching entry instead of widening an exclusion. Only add entries for
  confirmed false positives, and say why in the pull request.
- For behavior changes, also manually test on a supported Paper/Spigot server
  (SQLite storage is the easiest setup).

## Repository tooling

- Bun is used only for repository tooling; it is not a runtime dependency of the
  plugin.
- `bun test` runs the PR validator tests in `scripts/check-pr/index.test.ts`.
- `bun run check:pr` validates the current PR title, body, and commit subjects.
  CI runs both commands in `.github/workflows/pr-check.yml`.
- See `scripts/check-pr/README.md` for the validator architecture and flow
  diagram.
- Pull requests must use `.github/pull_request_template.md`. Add the
  `skip-pr-check` label only for bot PRs or exceptional cases.

## Architecture map

- `src/main/java/me/playernguyen/opteco/OptEco.java` — plugin bootstrap and
  lifecycle only. It builds the service graph, registers Bukkit adapters, and
  closes resources in reverse order on disable.
- `core/model/` — immutable domain types: `Account`, `Transaction`, `Money`,
  `PaymentResult`, `TransferResult`, and balance-change reasons. `Money` stores
  exact `BigDecimal` values at a fixed scale of eight.
- `core/port/` — repository, cache, unit-of-work, clock, and event-bus
  interfaces. Core code must not import Bukkit.
- `core/service/` — `EconomyService` (balance mutations and atomic transfers)
  and `PaymentService` (persisted pending-payment state machine).
- `storage/jdbc/` — shared SQLite/MySQL connection providers, resource-backed
  schema migrations (`migration/`), account and payment repositories,
  dialect-specific upserts, and transactions.
- `config/` — immutable `OptEcoConfig` snapshot, config keys, validation, and
  reloadable `ConfigService`.
- `message/` — typed message keys and the language loader.
- `command/` — `/opteco` (aliases `/points`, `/pts`, `/p`) plus the command
  dispatcher, argument parsing, and one class per subcommand.
- `event/` — custom Bukkit events for balance changes and payment transitions.
- `listener/` — join/quit cache loading and the balance title effect.
- `schedule/` — payment countdown/expiry task.
- `hook/` — optional PlaceholderAPI, MVdWPlaceholderAPI, BossShopPro, and
  ShopGUIPlus adapters.
- `api/` — public `OptEcoAPI`, registered through Bukkit's `ServicesManager`.
- `runtime/` — async database executor and main-thread event bridge.
- `updater/` — GitHub release update check.
- `metrics/` — vendored bStats implementation.
- `web_integration/` — deprecated standalone PHP client. It is not part of the
  Java build and is tracked as follow-up work.

## Conventions

- Use **Java 8** language features only. Do not introduce newer APIs.
- Add new configuration values to `ConfigKey` (path + default), read them in
  `ConfigService`, and expose a typed field on `OptEcoConfig`. Never hardcode
  config paths in commands.
- Add schema changes as the next numbered SQL file under both
  `src/main/resources/migration/sqlite/` and `src/main/resources/migration/mysql/`.
  Never edit or renumber an applied file: startup compares its SHA-256 with
  `opteco_schema_history` and refuses to run on a mismatch. Use
  `{{account_table}}` / `{{transaction_table}}` instead of literal table names.
  See `storage/jdbc/migration/package-info.java`.
- Add user-facing messages to `MessageKey`; the language file is generated and
  merged automatically. Do not scatter raw player-facing strings.
- Add permissions to `OptEcoPermission` and `plugin.yml`, including the
  `opteco.admin` / `opteco.user` children when appropriate.
- Register new subcommands in `PointsCommand`; implement `SubCommand.execute`
  and optionally `SubCommand.complete`. Arguments are parsed by
  `AmountParser`/`PlayerResolver`.
- Route every balance change through `EconomyService`. Do not call a repository
  from a command, listener, hook, or the API directly.
- Store money as `BigDecimal` through `Money`; never store a balance as a
  `double` in new code. All money crosses the database boundary as scaled longs.
- Keep `OptEcoAPI` source-compatible where practical. Add methods rather than
  changing existing signatures; document thread-safety expectations.
- Core services and storage must not import Bukkit. Database work runs on
  `AsyncExecutor`; events, messages, titles, and command replies run on the
  main thread through the runtime bridge.
- Use JetBrains `@NotNull` / `@Nullable` on public APIs and nullable return
  values. Lombok `@Getter`, `@Builder`, and related annotations are allowed for
  data classes; do not add mutable setters to domain models.
- Run `./gradlew spotlessApply` after editing Java files. The build also runs
  SpotBugs and Spotless checks.
- Add JUnit 6 tests under `src/test/java` for pure logic. Name tests
  `should<DoSomething>When<Criteria>` and split bodies into `// Arrange`,
  `// Act`, and `// Assert` blocks. Keep unit tests independent of a running
  Bukkit server; SQLite integration tests use a temporary file.

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

- `StorageType` supports only `SQLITE` and `MYSQL`. YAML account storage is not
  implemented. `LegacyImporter` imports the old `opteco` and
  `opteco_transaction` tables into the new schema when it finds them.
- Vault, the PHP web integration, and the PlayerPoints importer are deliberately
  outside this refactor. They are tracked as follow-up issues; do not register
  or depend on a Vault provider until it is implemented properly.
- The write-through `AccountCache` is authoritative for online-player reads.
  Every mutation must update the cache after the DB commit; do not add a second
  cache or a read-through DB lookup in placeholder hooks.
- Table names come from configuration and are validated as identifiers. Values
  always use prepared statements; never interpolate player input into SQL.
- Payments are **pending** until `/points confirm`; they expire and auto-cancel.
  Confirm, cancel, and timeout paths are covered by `PaymentServiceTest`.
  Transaction state changes and balance moves share one DB transaction.
- `plugin.yml` declares `loadbefore: [BossShopPro]` and soft-depends on
  PlaceholderAPI, BossShopPro, MVdWPlaceholderAPI, and ShopGUIPlus. Optional
  integrations must stay optional and null-safe; catch `Throwable` around class
  loading.
- Plugin APIs (Paper, PlaceholderAPI, ShopGUI, BossShopPro, MVdW) are
  `compileOnly`. HikariCP, SQLite JDBC, and MySQL Connector/J are shaded into
  the JAR. Do not move server or plugin APIs to `implementation`.
- `build.gradle` uses Lombok as an annotation processor; keep the `compileOnly`
  and `annotationProcessor` entries in sync.
- `/points reload` swaps config/message snapshots. Storage or database connection
  changes require a server restart and are reported to the sender.
- BossShopPro and MVdWPlaceholderAPI are resolved from online Maven
  repositories. Do not reintroduce `lib/*.jar`; keep build inputs reproducible.

## Change checklist

1. `./gradlew clean build` succeeds (this includes `test`, `spotlessCheck`, and
   `spotbugsMain`).
2. New config keys have sensible defaults in `ConfigKey`, are validated by
   `OptEcoConfig`, and appear in `src/main/resources/config.yml`.
3. New messages exist in `MessageKey` and are created in the generated language
   file.
4. New permissions are declared in `plugin.yml`.
5. Manual smoke test: join, `/points me`, admin `add`/`take`/`set`,
   `/points top`, a `pay` + `confirm`/`cancel`/timeout, and `/points reload`.
6. SQLite and MySQL both start from a fresh database and from the legacy schema.
7. No secrets or server-specific credentials are committed (especially
   `web_integration/api/APIConfig.php`).

## Remote and workflow notes

- Default branch: `master`.
- Repository: `PlayerNguyen/OptEco` on GitHub.
- To release, push a `vX.Y.Z` tag. The release workflow builds with
  `-Pversion=X.Y.Z` and creates a GitHub release with the JAR attached.
- Follow the [commit convention](#commit-convention) for all commit messages.
- Fill in `.github/pull_request_template.md` when opening a pull request.
- Update `README.MD`, `CONFIGURATION.md`, or `API.MD` when behavior or public
  API changes.
