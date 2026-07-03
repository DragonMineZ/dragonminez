# How to JSON

You don't need to know Java to configure DMZ, but the JSON has to be *valid* JSON, and DMZ has some specific behaviors around versioning you should know about before you start editing.

## Ground rules

- **Standard JSON only.** No comments, no trailing commas. If you want to leave yourself a note, most DMZ config objects don't have a `"_comment"` convention — just keep notes outside the file.
- **UTF-8 encoding.** Config is read/written with `StandardCharsets.UTF_8` (`ConfigLoader.java`). Don't save files as UTF-16 or with a BOM your editor might add silently.
- **Use a real editor.** VS Code, Notepad++, or IntelliJ will catch bracket/quote mismatches before they hit the server. Notepad works but won't warn you about syntax errors.
- **Validate before you deploy.** A single misplaced comma will fail to parse. Paste the file into any JSON validator (or your editor's built-in linter) before restarting/reloading a live server.

## What happens when DMZ can't parse your file

This is the part that surprises people. DMZ does **not** crash or silently ignore a broken config. For versioned config files (`ConfigManager.loadAndValidate`), if a file:

- is missing,
- fails to parse (syntax error), or
- has a `configVersion` older than the mod's current version,

...DMZ renames your existing file to `old_<filename>` in the same folder, and regenerates a fresh default in its place. If the failure wasn't a hard parse error, it reads your old raw JSON first and merges any recognizable values into the regenerated file, so you don't necessarily lose all your tuning — but you should always check the new file and diff it against `old_<filename>` after a regeneration.

**Practical takeaway:** if you edit a config file and it comes back looking like the default again after a restart/reload, check the logs for a `Regenerating <file>. Reason: ...` warning, and look for the `old_` backup sitting next to it.

## `configVersion` fields

Most config files (`entities.json`, race `character.json`/`stats.json`, forms, etc.) carry a `configVersion` field. **Do not hand-edit this number.** It's how DMZ decides whether your file matches the schema the current mod build expects. Changing it manually can trigger an unwanted regeneration or, worse, convince DMZ your outdated file is current.

## Custom/user-defined files are preserved differently

Unknown files that aren't part of DMZ's built-in default set (a custom race folder, a custom form file, a custom quest/saga) are **not** subject to the version-regeneration logic above — DMZ loads whatever it finds and preserves it as-is. This is what lets you safely add your own races, forms, and quests without them getting overwritten on update. The regeneration behavior above only applies to DMZ's own default/built-in config files.

## Common mistakes

- Forgetting to rename `raceName`/`groupName`/id-like fields consistently between the folder name and the JSON content — DMZ keys many lookups off both.
- Copy-pasting a form/race file and forgetting to change internal identifiers, causing one entry to silently override another.
- Editing a file while the server is running and expecting it to apply live — see [[Reloading and updating changes|Reloading-and-updating-changes]] for what actually needs a reload vs. a restart.
- Editing `old_<filename>` thinking it's still active — it isn't; it's a backup, DMZ never reads it back in.

## Where to check your syntax against a schema

Every config page in this wiki links back to its backing Java class (e.g. `RaceCharacterConfig.java`, `FormConfig.java`). When in doubt about a field name or type, that class is the ground truth — the wiki describes it, but the code defines it.
