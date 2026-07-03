# Locating Specific Folders

DMZ splits its editable data across a few different roots depending on **who owns the data** and **when it's read**. Knowing which root you need saves a lot of confusion, especially since some folders only appear after you've run the server/client at least once.

## `config/dragonminez/` — mod config (per-instance, not per-world)

Generated the first time DMZ boots. This is where you tune balance, stats, and add custom races/forms/entities.

| Path | Purpose |
| :-- | :-- |
| `config/dragonminez/general-server.json` | Server-wide gameplay toggles (see [[General Server\|general-server]]). |
| `config/dragonminez/general-user.json` | Client-side settings synced per user (see [[General User\|general-user]]). |
| `config/dragonminez/combat.json` | Combat tuning values (see [[Combat\|Combat]]). |
| `config/dragonminez/skills.json` | Skill costs/cooldowns/eligibility, and per-race/master skill offerings — both live in this one file (see [[Skills\|Skills]]). |
| `config/dragonminez/training.json` | TP-training minigame tuning (Rhythm, Control, Memory, Precision, Gravity). |
| `config/dragonminez/techniques.json` | Ki attack and strike attack XP-cost/cooldown tuning. |
| `config/dragonminez/entities.json` | Hard mode multipliers and per-entity base stats (see [[Entities\|Entities]]). |
| `config/dragonminez/races/<race>/character.json` | Race identity/appearance defaults. |
| `config/dragonminez/races/<race>/stats.json` | Race base stats and scaling per class. |
| `config/dragonminez/races/<race>/forms/*.json` | Forms that belong only to that race. |
| `config/dragonminez/forms/*.json` | Shared "stack" forms layered on top of a race's own forms (see [[Custom Forms\|Custom-Forms]]). |

Reference: `ConfigManager.java` (`loadGeneralConfigs`, `loadAllRaces`, `createOrLoadStackForms`).

## `<world save>/dragonminez/` — per-world story & player content

Lives inside the world save, so it's per-world, not per-instance. This is why quest/saga changes don't show up until you point at the right save folder (`run/saves/<world>/dragonminez/...` in a dev environment).

| Path | Purpose |
| :-- | :-- |
| `dragonminez/sagas/` | Saga manifests (`questFolder` pointer + saga metadata). |
| `dragonminez/quests/<questFolder>/` | Quest files for a saga, loaded alphabetically (`01_...json`, `02_...json`, ...). |
| `dragonminez/sidequests/` | Standalone side quests, loaded when side quests are enabled. |
| `dragonminez/wishes/*.json` | Per-dragon wish list overrides (see [[Custom Wishes\|Custom-Wishes]]). |
| `dragonminez/npcs/placements.json` | Master/trainer NPC spawn positions (see [[NPC Placement & Alignment\|NPC-Placement-and-Alignment]]). |
| `dragonminez/npcs/alignment_rules.json` | Per-NPC alignment/hostility thresholds (see [[NPC Placement & Alignment\|NPC-Placement-and-Alignment]]). |
| `dragonminez/playerdata_json/<uuid>.json` | Player save data, only used when storage mode is `JSON`. |

Reference: `QuestRegistry.loadAll(server)`.

DMZ does **not** overwrite existing saga/quest/sidequest files here by default — it only generates the defaults when the folders are missing and default-generation toggles are enabled in `general-server.json` (`storyModeEnabled`, `createDefaultSagas`, `sideQuestsEnabled`, `createDefaultSideQuests`).

## `<game dir>/dragonballs/` — external dragon ball addon packs

A root next to your world/instance (not inside the world save) used for external, distributable dragon ball definitions. Supports both a plain folder and a `.zip` pack. Loaded/merged by `DragonBallPackManager`, layered over the bootstrap definitions in `DragonDefinitionReloadListener`.

## Datapacks — `data/<namespace>/dragonminez/...`

Some systems are plain Forge/vanilla reload listeners and follow normal datapack rules instead of living in `config/` or the world save:

- `dragonminez/dragonballs/` — dragon ball/wish definitions merged with the config-based overrides in the world save (see [[Custom Wishes\|Custom-Wishes]]).
- `spacepod/*.json` — Space Pod destination list (see [[Space Pod Destinations\|Space-Pod-Destinations]]).
- `weapon_attributes/*.json` — weapon attribute definitions (see [[Weapon Attributes\|Weapon-Attributes]]).

None of these three are covered by `/dmzreload` — they need a vanilla `/reload` or a restart. See [[Reloading and updating changes|Reloading-and-updating-changes]] for the full breakdown of what `/dmzreload` does and doesn't cover.

## Assets — `assets/dragonminez/...` (resource pack territory)

Models, textures, and animations referenced by config (`customModel` fields) live under the normal resource pack namespace, e.g.:

- `assets/dragonminez/geo/entity/races/`
- `assets/dragonminez/geo/entity/forms/`
- `assets/dragonminez/lang/en_us.json` (source language file)

These require a resource reload (`F3+T` client-side) or restart — see [[Reloading and updating changes|Reloading-and-updating-changes]].

## Quick decision guide

- Tuning stats/damage/costs, adding a race/form/entity → `config/dragonminez/`
- Adding/editing story content for **this world** → `<world save>/dragonminez/`
- Shipping a distributable dragon ball pack → `<game dir>/dragonballs/`
- Adding a model/texture/animation → `assets/dragonminez/...` in a resource pack or datapack
