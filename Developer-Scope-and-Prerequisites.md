# Developer Scope and Prerequisites

This wiki is intentionally scoped to developers and technical modders.

## Intended audience

- People who can edit JSON and resource pack assets.
- People who can run Minecraft Forge dev environments.
- People who want to contribute to DMZ-compatible content.

## Not in scope

- General player walkthroughs.
- Lore pages.
- Survival progression guides.

## Two ways to extend DMZ

| Approach | What you need | Where to start |
| :-- | :-- | :-- |
| **Data addons** (configs, races, forms, quests, wishes, dragon ball packs, resource packs) | A text editor and a DMZ instance | This page, then the JSON pages linked below |
| **Code addons** (your own Forge mod that hooks DMZ events and APIs) | A Forge 1.20.1 dev environment | [[Getting Started|Getting-Started]] |

## Baseline setup

1. Run DMZ once so config files are generated.
2. Use this config root for local dev instances:
   - `config/dragonminez`
3. Race configuration lives in:
   - `config/dragonminez/races/<race_name>/` (`character.json`, `stats.json` and a `forms/` folder)
4. Shared stack forms live in:
   - `config/dragonminez/forms/`
5. Config files for entities' attributes:
   - `config/dragonminez/entities.json`
6. Config files for general server settings:
   - `config/dragonminez/general-server.json`
7. Config files for general user (client) settings:
   - `config/dragonminez/general-user.json`
8. Config files for combat, training, skills and ki techniques:
   - `config/dragonminez/combat.json`
   - `config/dragonminez/training.json`
   - `config/dragonminez/skills.json`
   - `config/dragonminez/techniques.json`
9. Raid and tournament definitions (one JSON file per raid/tournament; extra files you add are loaded too):
   - `config/dragonminez/raids/`
   - `config/dragonminez/tournaments/`
10. Client-only HUD layout (written by the in-game HUD editor):
    - `config/dragonminez/hud_layout.json`

Every config file except `general-user.json` and `hud_layout.json` is sent from the server to each client on login, so clients always use the server's values.

Outdated or broken config files are not silently thrown away: DMZ moves the old file into `config/dragonminez/oldBackup/` (same relative path) and merges your edits into the new defaults. See [[Reloading and updating changes|Reloading-and-updating-changes]].

### World-save data

Some systems live inside the world folder instead of `config/`:

| Folder | Contents | Page |
| :-- | :-- | :-- |
| `<world>/dragonminez/sagas/`, `quests/`, `sidequests/` | Story sagas, saga quests and side quests | [[Custom Quests, Sagas and Sidequests|Custom-Quests-Sagas-and-Sidequests]] |
| `<world>/dragonminez/wishes/` | Per-world wish overrides (one file per dragon) | [[Custom Wishes|Custom-Wishes]] |
| `<world>/dragonminez/dialogues/` | NPC dialogue files | [[NPC Dialogue|NPC-Dialogue]] |
| `<world>/dragonminez/npcs/` | NPC placements (`placements.json`) and alignment rules (`alignment_rules.json`) | [[NPC Placement and Alignment|NPC-Placement-and-Alignment]] |
| `<world>/dragonminez/playerdata_json/` | Player data when the `JSON` storage backend is selected | [[General Server|General-Server]] |

Dragon ball addon packs (folders or `.zip` files) go in the `dragonballs` folder of the game directory. See [[Custom Wishes|Custom-Wishes]].

Not sure where a folder is on your machine? See [[Locating specific folders|Locating-specific-folders]].

## Reload workflow

After changing JSON config files, you are able to reload in-game:

- Command: `/dmzreload [all|config|story|wishes]` (no argument = `all`)
- Command source: `src/main/java/com/dragonminez/server/commands/ReloadCommand.java`

See [[Commands|Commands]] and [[Reloading and updating changes|Reloading-and-updating-changes]] for what each section reloads.

Resource pack changes (models/textures) normally require a resource reload or restart.

### Finding JSON mistakes

When a config, quest or wish file fails to load, DMZ records the file, the line/column and the JSON path of the problem. The report is printed to the server log and shown in chat to players when they log in. Server owners can turn the chat message off with `developer.reportJsonProblemsInChat = false` in `general-server.json`.

## Translations

All in-game text uses translation keys. The English source file is `assets/dragonminez/lang/en_us.json`; community translations are managed on Crowdin (see [[Contributing|Contributing]]).

DragonMineZ automatically downloads the latest Crowdin translation for the game language you selected (every language except `en_us`), so translation fixes reach players without a mod update. To turn this off (for example to test your own lang file), start the game with the JVM argument `-Ddmz.crowdin.enabled=false`.

When a translation key is missing, some DMZ screens fall back to a readable version of the id (for example quest texts and form names are turned into title-cased words). For proper names and other languages, ship your own keys in a resource pack under `assets/<namespace>/lang/<locale>.json` and use those keys in your JSON (for example a form's name uses `race.dragonminez.<race>.form.<group>.<form>`).
