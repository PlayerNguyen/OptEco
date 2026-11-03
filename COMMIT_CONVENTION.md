# Commit Convention

OptEco follows [Conventional Commits 1.0.0](https://www.conventionalcommits.org/en/v1.0.0/).
Clear, consistent commit messages make the history easier to review and let us
generate release notes later.

## Format

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

## Types

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

## Scopes

Scopes are optional. Use one of these when the change is concentrated in an
area, or omit the scope for project-wide changes.

`account`, `api`, `command`, `config`, `language`, `sql`, `transaction`,
`web`, `build`, `docs`

## Examples

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

## Breaking changes

Mark a breaking change with `!` after the type/scope and add a
`BREAKING CHANGE:` footer:

```text
feat(api)!: remove deprecated getPlugin accessor

BREAKING CHANGE: consumers must use OptEco.getInstance() instead.
```

## Issue references

- Use `Closes #123` or `Fixes #123` in the footer to close an issue when the
  commit or PR is merged.
- Use `Refs #123` for related work that should not close the issue.

## Reverts

Use the `revert` type and reference the reverted commit:

```text
revert: feat(api): add legacy balance setter

This reverts commit 1234abcd.
```

## Notes

- Pull requests are checked by `.github/workflows/pr-check.yml` using
  `scripts/check-pr.ts`. The check validates the PR title, the PR template, and
  every non-merge commit subject.
- Run the validator locally with `bun test` and `bun run check:pr` (set
  `PR_TITLE` and `PR_BODY` when running outside GitHub Actions).
- Use the `skip-pr-check` label only for bot PRs or exceptional cases.
- Pull requests should follow the template in
  `.github/pull_request_template.md`.
- Keep commit messages in English, even if discussion or documentation is in
  another language.
