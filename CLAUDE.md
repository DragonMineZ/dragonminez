# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

DragonMineZ is a **Minecraft Forge 1.20.1 mod** (Java 17, mod id `dragonminez`, package `com.dragonminez`) that layers a Dragon Ball progression system — races, stats, transformations, skills, quests, dimensions, NPCs, raids — on top of survival Minecraft.

The repo is **data-heavy and player-customizable**. Many systems load JSON at runtime from `config/dragonminez`, world-save folders (`<world>/dragonminez/...`), datapacks, and external addon folders. Do not treat JSON as internal-only or safe to rewrite destructively — players, server owners, and addon authors edit these files.

Required mods: Forge, GeckoLib, TerraBlender, **Curios** (all `mandatory=true` in `mods.toml`). Declared **incompatible** with Legendary Tooltips, Epic Fight, and Better Combat — the `DragonMineZ` entry class throws at startup if any is installed.

## Additional AI docs

`AI/Agents.md` (operating rules), `AI/Context.md` (detailed architecture and data-system notes), and `AI/Memory.md` (durable lessons) go deeper than this file. Read `AI/Context.md` before non-trivial work — it documents config versioning, quest/wish/dragonball loading, storage backends, worldgen, and release automation in detail, and is kept in sync with the code.

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
- Language JSON: validate with `scripts/check_lang.sh` (needs `jq`). Source locale is `src/main/resources/assets/dragonminez/lang/en_us.json` (Crowdin source; CI validates and uploads it via `.github/workflows/languages.yml`).
- Client-only dev mods (JEI runtime, Oculus, etc.) are excluded from `runServer`/`runData` task graphs on purpose — some mixin into vanilla codecs and would poison generated JSON. See the `includeClientOnlyDevMods` logic in `build.gradle.kts`.
- The shippable jar is the no-classifier `jarJar` output (bundles MariaDB + HikariCP); the plain `jar` gets a `-slim` classifier. PackSquash resource optimization runs on CI and on jar packaging (off locally by default); force with `-PoptimizeResources=true|false`.

## Architecture

**Boot path:** `com.dragonminez.DragonMineZ` (`@Mod` entry; also enforces the incompatibility list) → `DMZCommon.init()` → side init via `DistExecutor` (`DMZClient.init()` / `DMZServer.init()`).

`DMZCommon.init()` is the central registration point: `ConfigManager.initialize()`, `QuestRegistry.init()`, `WishManager.init()`, `NetworkHandler.register()`, GeckoLib, and all deferred registries. Server lifecycle (storage init, quest/wish/NPC loading, dragonball first-spawn, reload listeners, command registration) lives in `common/events/ForgeCommonEvents`.

**Source layout (`com.dragonminez`):**
- `client` — rendering, GUI, models, animation, flight, collision, clash minigames, client systems (kisense, taiyoken, impact frames), Crowdin support, client events.
- `common` — config, registries (`init/Main*`), stats/capability, networking, quests, wishes, dragonballs, combat, alignment, passives, hair, space pods, datagen, compat. Shared code.
- `server` — commands, recipes, storage, worldgen/dimensions/raids, dynamic growth, server events.
- `mixin` — common + client mixins (declared in `src/main/resources/dragonminez.mixins.json`).

### Conventions to follow

- **Registries:** add content through the existing deferred-register `common/init/Main*` classes (`MainBlocks`, `MainItems`, `MainEntities`, …) and wire it from `DMZCommon.init()`. Keep registry names stable once exposed to worlds/saves/recipes/assets/addons.
- **Networking:** Forge `SimpleChannel` `dragonminez:network`; every packet is registered in `NetworkHandler.register()` with an explicit direction and encode/decode/handle triplet. Packet ids are assigned by **registration order** — do not reorder or insert registrations mid-list; append new packets. Never trust client input for stats, unlocks, permissions, quests, wishes, or progression — validate server-side.
- **Player state:** capability-based via `StatsCapability` / `StatsProvider` / `StatsData`. `StatsData` owns Stats, Status, Cooldowns, Character, Resources, Skills, Effects, SecondaryStatEffects, PlayerQuestData, BonusStats, Techniques, and DynamicGrowthData — adding a group means updating the ctor, `save()`, `load()`, `copyFrom()`, and the right sync packet together. Per-tick logic is split across three handlers (`StatsCapability.onPlayerTick`, `server/events/players/TickHandler`, `server/events/players/StatsEvents`). Stat/form/race math reads `ConfigManager` live (no caching).
- **Mixins:** require both a Java class and an entry in `dragonminez.mixins.json`. Keep them minimal; verify dedicated-server safety for anything touching client classes.

### Runtime data systems (high-risk)

- **Config:** root `config/dragonminez` (`general-user`, `general-server`, `combat`, `training`, `skills`, `techniques`, `entities`, `races/<race>/{character,stats,forms/*}`, stack forms in `forms/*`). Versioned with a **semver string** `configVersion`; outdated/corrupt defaults are moved into `config/dragonminez/oldBackup/<same relative path>` and regenerated with a three-way merge against baselines in `resources/data/dragonminez/previousConfigs/`. Unknown user-defined races/forms are preserved. Server syncs every config file except `general-user` to clients per-file; `/dmzreload [all|config|story|wishes]` reloads and resyncs.
- **Quests/sagas/sidequests:** loaded from world-save JSON under `<world>/dragonminez/{sagas,quests,sidequests}` by `QuestRegistry.loadAll` at server start. Existing world quest files are **never overwritten** — changing defaults in code does not affect existing worlds. Preserve saga filename ordering (`01_...json`) and translation-key titles.
- **Wishes/dragonballs:** definitions and wishes merge from hardcoded bootstrap defs, external addon packs (folder or `.zip`) under a game-dir `dragonballs` root, and per-world overrides in `<world>/dragonminez/wishes/*.json` (plain JSON arrays; the dragon id is the filename stem). There is no in-jar datapack wish JSON.
- **Storage:** `StorageManager` selects `NBT` (vanilla capability persistence only), `JSON` (`<world>/dragonminez/playerdata_json/<uuid>.json`), or `DATABASE` (MariaDB + HikariCP). Vanilla `.dat` capability persistence is **always on**; JSON/DATABASE add a second, authoritative store on top (async load on login overwrites vanilla values). On DB failure there is no active fallback switch — durability comes from the vanilla path. Never block the server thread with DB/filesystem work; preserve `StatsData` NBT key names (the save contract for all three backends); never log credentials.

When changing any JSON schema, update the parser, defaults, config version, and sync packets together, and consider migration/backup for existing worlds. Run `runData` after changing datagen providers and review the `src/generated/resources/` diff.

## Working agreements

- Make the smallest coherent change that solves the request. Backward compatibility is **not** assumed unless the change affects save data, player-editable JSON, configs, or addons — when in doubt for those, ask.
- Avoid destructive git operations (`reset --hard`, forced checkout, recursive deletion). Don't edit `build/`, `run/`, or `run-data/` unless the task targets them. Don't commit unless asked.
