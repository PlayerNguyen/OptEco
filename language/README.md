# Language files

These files are legacy examples from the pre-refactor plugin. The current
plugin generates `plugins/OptEco/language/en.yml` on first start and merges any
missing keys from `MessageKey`, so the generated file is the authoritative
template for translations.

To translate OptEco:

1. Start the server once to generate `plugins/OptEco/language/en.yml`.
2. Copy it to another file in the same folder.
3. Set `Settings.Preferences.Language` in `config.yml` to that file name.
4. Reload with `/points reload` or restart the server.

Keys are nested paths such as `COMMAND.ADD.SUCCEEDED` and support `&` color
codes plus placeholders like `%value%`, `%who%`, `%currency%`, and `%second%`.
