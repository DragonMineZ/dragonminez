# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

DragonMineZ is a **Minecraft Forge 1.20.1 mod** (Java 17, mod id `dragonminez`, package `com.dragonminez`) that layers a Dragon Ball progression system — races, stats, transformations, skills, quests, dimensions, NPCs — on top of survival Minecraft.

The repo is **data-heavy and player-customizable**. Many systems load JSON at runtime from `config/dragonminez`, world-save folders, datapacks, and external addon folders. Do not treat JSON as internal-only or safe to rewrite destructively — players, server owners, and addon authors edit these files.

## Additional AI docs

`AI/Agents.md` (operating rules), `AI/Context.md` (detailed architecture and data-system notes), and `AI/Memory.md` (durable user instructions) are maintained for AI agents and go deeper than this file. Read `AI/Context.md` before non-trivial work — it documents config versioning, quest/wish/dragonball loading, storage backends, worldgen, and release automation in detail.

## Build & run

Use the Gradle wrapper from the repo root (PowerShell on Windows):

```powershell
.\gradlew.bat build       # full build; depends on reobfuscation tasks
.\gradlew.bat runClient   # working dir: run/
.\gradlew.bat runServer   # working dir: run/
.\gradlew.bat runData     # datagen; working dir run-data/, writes src/generated/resources/
```

- `src/generated/resources/` is a main resource source set; `build` re-runs `runData`.
- Gradle is configured **offline** (`org.gradle.offline=true` in `gradle.properties`) — dependency changes may require toggling this.
- There is **no committed `src/test` suite** despite the JUnit dependency. Validate gameplay changes with `build` and, when practical, `runClient`/`runServer`. Do not claim the build passes unless it passed this session.
- Language JSON: validate with `scripts/check_lang.sh` (needs `jq`). Source locale is `src/main/resources/assets/dragonminez/lang/en_us.json` (Crowdin source).
- Client-only dev mods (JEI runtime, Oculus, etc.) are excluded from `runServer`/`runData` task graphs on purpose — some mixin into vanilla codecs and would poison generated JSON. See the `includeClientOnlyDevMods` logic in `build.gradle.kts`.
- PackSquash resource optimization runs on CI and on jar packaging (off locally by default); force with `-PoptimizeResources=true|false`.

## Architecture

**Boot path:** `DragonMineZ` (`@Mod` entry) → `DMZCommon.init()` → side init via `DistExecutor` (`DMZClient.init()` / `DMZServer.init()`).

`DMZCommon.init()` is the central registration point: ConfigManager, quest/wish systems, `NetworkHandler.register()`, GeckoLib, and all deferred registries.

**Source layout (`com.dragonminez`):**
- `client` — rendering, GUI, models, animation, flight, collision, client events.
- `common` — config, registries, stats, networking, quests, wishes, dragonballs, combat, datagen, compat. Shared code.
- `server` — commands, recipes, storage, worldgen/dimensions, server events.
- `mixin` — common + client mixins.

### Conventions to follow

- **Registries:** add content through the existing deferred-register `common/init/Main*` classes (`MainBlocks`, `MainItems`, `MainEntities`, …) and wire it from `DMZCommon.init()`. Keep registry names stable once exposed to worlds/saves/recipes/assets/addons.
- **Networking:** register every packet in `NetworkHandler.register()` (Forge `SimpleChannel` `dragonminez:network`) with an explicit direction and stable encode/decode/handle triplet. Never trust client input for stats, unlocks, permissions, quests, wishes, or progression — validate server-side.
- **Player state:** capability-based via `StatsCapability` / `StatsProvider` / `StatsData`. `StatsData` owns Stats, Status, Cooldowns, Character, Resources, Skills, Effects, PlayerQuestData, BonusStats, Training, Techniques. Mind login/clone/death/dimension-change/tick/sync/save-load paths. Race/form/stat calculations depend heavily on `ConfigManager`.
- **Mixins:** require both a Java class and an entry in `src/main/resources/dragonminez.mixins.json`. Keep them minimal; verify dedicated-server safety for anything touching client classes.

### Runtime data systems (high-risk)

- **Config:** root `config/dragonminez`. Config classes carry `CURRENT_VERSION` / `configVersion`; outdated/corrupt defaults are moved to `old_<filename>` and regenerated, but unknown user-defined races/forms are preserved. Server syncs config to clients; `/dmzreload` reloads and resyncs everything.
- **Quests/sagas/sidequests:** loaded from world-save JSON under `<world>/dragonminez/{sagas,quests,sidequests}`. Existing world quest files are not overwritten by default; preserve saga filename ordering (`01_...json`) and translation-key titles.
- **Wishes/dragonballs:** merged from datapacks, the dragonball pack system, and config overrides (`config/dragonminez/wishes/*.json`). External addon packs (folder or `.zip`) load from a `dragonballs` game-dir root.
- **Storage:** `StorageManager` selects `NBT`, `JSON` (`<world>/dragonminez/playerdata_json/<uuid>.json`), or `DATABASE` (MariaDB + HikariCP, with fallback). Never block the server thread with DB/filesystem work; preserve NBT key names unless intentionally migrating; never log credentials.

When changing any JSON schema, update the parser, defaults, config version, and sync packets together, and consider migration/backup for existing worlds. Run `runData` after changing datagen providers and review the `src/generated/resources/` diff.

## Working agreements

- Make the smallest coherent change that solves the request. Backward compatibility is **not** assumed unless the change affects save data, player-editable JSON, configs, or addons — when in doubt for those, ask.
- Avoid destructive git operations (`reset --hard`, forced checkout, recursive deletion). Don't edit `build/`, `run/`, or `run-data/` unless the task targets them. Don't commit unless asked.
