# DragonMineZ AI Context

This file captures project context that AI agents should read after `AI/Agents.md` and `AI/Memory.md`. Keep it factual and update it when the architecture, workflows, or important repo conventions change. Last full audit against the code: 2026-07-07 (v2.1.1).

## Project Snapshot

DragonMineZ is a Minecraft Forge mod for Minecraft `1.20.1`, Java 17, mod id `dragonminez`. The root project name is `DragonMineZ`; archives are named from `mod_id`.

This repo is data-heavy and player-customizable. Many systems load JSON from runtime folders in `config/dragonminez`, world save folders (`<world>/dragonminez/...`), datapacks, and external addon folders. Do not assume JSON is internal-only or safe to rewrite destructively.

Dependencies declared in `META-INF/mods.toml`: Forge, Minecraft, GeckoLib, TerraBlender, and **Curios** are all `mandatory=true`. Legendary Tooltips, Epic Fight, and Better Combat are declared `type="incompatible"`, and the `com.dragonminez.DragonMineZ` entry class additionally throws `IllegalStateException` at construction if any of them is loaded.

## Build And Run

Use the Gradle wrapper from repo root, usually PowerShell on Windows:

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer
.\gradlew.bat runData
```

Important details:

- Java toolchain and compiler release are Java 17.
- `runClient` and `runServer` use `run/` as working directory; `runData` uses `run-data/` and writes to `src/generated/resources/`.
- `src/generated/resources/` is included as a main resource source set. `build` depends on reobfuscation tasks and re-runs `runData`.
- Gradle runs **offline** (`org.gradle.offline=true` in `gradle.properties`).
- The distributable jar is the no-classifier `jarJar` artifact (bundles `mariadb-java-client` and `HikariCP`); the plain `jar` task produces `-slim`. Never ship the slim jar.
- Client-only dev mods (JEI runtime, Oculus, Xenon, Free Cam, Huge Structure Blocks) are on the classpath only for `runClient` (`includeClientOnlyDevMods` in `build.gradle.kts`) because some mixin into vanilla codecs and would poison `runData` output.
- PackSquash optimizes bundled PNG/OGG/JSON before jar packaging; automatic on CI, off locally, forced with `-PoptimizeResources=true|false`. Options in `packsquash.toml`.
- There is no committed `src/test` suite. Validate with `build`, `runClient`, `runServer`, or targeted in-game scenarios.
- `scripts/check_lang.sh` validates language JSON with `jq`.

## Lifecycle

Primary boot path:

- `com.dragonminez.DragonMineZ` is the `@Mod` entry point. Its constructor first enforces the incompatibility list (legendarytooltips, epicfight, bettercombat), then calls `DMZCommon.init()`, then side-specific init through `DistExecutor` (`DMZClient.init()` on client, `DMZServer.init()` on dedicated server — the latter is log-only; commands register via event).
- Root-package helpers: `Reference` (MOD_ID), `Env`/`LogUtil` (logging).

`DMZCommon.init()` is the central registration and setup point, in order: `ConfigManager.initialize()`, `QuestRegistry.init()`, `WishManager.init()`, `NetworkHandler.register()`, `GeckoLib.initialize()`, then deferred registers on the mod bus — `MainAttributes`, `EntityAttributes`, `MainBlocks`, `MainBlockEntities`, `MainItems`, `MainFluids`, `MainSounds`, `MainTabs`, `MainEntities`, `MainVillagers`, `MainParticles`, `MainRecipes`, `MainMenus`, `MainEffects`, `MainEnchants`, `MainLootModifiers`, `MainStructurePlacements`, `MainStructureProcessors`, `MainStructureTypes`, `OverworldFeatures`, `SacredKaiFeatures` — plus `ModCommonEvents::commonSetup`, `MainGameRules.register()`, `MainDamageTypes.register()`. Note the structure/feature classes live under `server/world/...` but register from common init.

`ModCommonEvents` (mod bus) also registers entity attributes, adds player attributes (`STRENGTH`, etc.), registers `DragonBallSavedData` capability, adds a pack finder, and in `commonSetup` registers the TerraBlender `OverworldRegion` (weight 40, adds the rocky biome without replacing vanilla biomes) and `OverworldSurfaceRules`.

Server event flow (`common/events/ForgeCommonEvents`, forge bus):

- `onServerAboutToStart`: pre-loads Otherworld regions when `worldGen.otherworldActive`, and runs `VillagePoolInjector.injectAll`.
- `onServerStarting`: `StorageManager.init()`, `BetaWhitelist.reload()` (async), `WishManager.loadWishes(server)`, `DMZPermissions.init()`, `QuestRegistry.loadAll(server)`, `NpcAlignmentRules.load`, `NPCPlacementManager.load`, `WorldGuardCompat.init()`, then first dragonball scatter per ball set if `worldGen.generateDragonBalls`.
- `onServerStarted`: `StructureSpawnPlanner.precomputeAndWait`, Otherworld region load (double-check), `NPCPlacementManager.spawnForLoadedLevels`.
- `onLevelLoad`: `StructureSpawnPlanner.onLevelLoad`; re-runs Otherworld region loading when the Otherworld level loads.
- `onServerStopping`: `StorageManager.shutdown()`, `StructureSpawnPlanner.reset()`.
- `onAddReloadListeners`: `SpacePodDestinationRegistry`, `DragonDefinitionReloadListener`, `DragonWishRegistry`, and an inline listener calling `WeaponRegistry.loadAttributes`.
- `onRegisterCommands` → `DMZServer.registerCommands`.
- `ForgeCommonEvents` also hosts gameplay handlers: login/logout/death/respawn/dimension-change, melee/ranged crits, `LivingAttack`/`LivingDamage` mitigation hooks, equipment change, mob spawn finalize, villager trades.

## Source Layout

Main packages (`com.dragonminez`):

- `client`: `animation`, `beta` (beta-access verification screen), `clash` (beam/combat clash UI), `collision`, `crowdin`, `dragonball` (radar rendering), `events`, `flight`, `gui`, `init`, `model`, `render`, `systems` (`impactframes`, `kisense`, `taiyoken`), `title`, `util`.
- `common`: `alignment` (NPC alignment rules/disposition), `combat` (`clash`, `logic`, `player`, `util`, `weapon`), `compat` (JEI plugin, `WorldGuardCompat` — reflective, no hard dep), `config`, `datagen`, `dragonball`, `events`, `hair` (custom hair strands), `init` (all `Main*` registries), `loot`, `network` (`C2S`, `S2C`, `NetworkHandler`), `passives` (class passives + handlers), `quest`, `spacepod`, `stats` (capability + `character`, `extras`, `skills`, `techniques`), `util`, `wish`.
- `server`: `commands`, `dynamicgrowth` (`DynamicGrowthService`), `energy` (`StarEnergyStorage`), `events` (incl. `players/TickHandler`, `players/StatsEvents`, `DataSyncHandler`, `QuestEvents`, `DragonBallsHandler`), `recipes`, `storage`, `util`, `world` (`biome`, `data`, `dimension`, `feature`, `gen`, `npc`, `raid`, `region`, `structure`, `tree`).
- `mixin`: `common` + `client` mixins declared in `src/main/resources/dragonminez.mixins.json` (with `dragonminez.refmap.json`); the `server` mixin list is empty.

Registration pattern:

- New content belongs in a `common/init/Main*` class registered from `DMZCommon.init()`.
- New packets must be registered in `NetworkHandler.register()`; see Networking below for ordering rules.
- New mixins require both a Java class and an entry in `dragonminez.mixins.json`.

## Player State

Player state is capability-based:

- Core classes: `common/stats/StatsCapability`, `StatsProvider`, `StatsData` (~1570 lines; state + most stat/form/combat math).
- `StatsCapability` registers the capability and attaches a `StatsProvider` to every `Player` in `AttachCapabilitiesEvent<Entity>`. `StatsProvider` implements `INBTSerializable` (`serializeNBT()→data.save()`, `deserializeNBT()→data.load()`), so vanilla `.dat` persistence is always active regardless of storage backend.
- `StatsData` owns exactly 12 sub-objects (ctor-constructed): `Stats`, `Status`, `Cooldowns`, `Character`, `Resources`, `Skills`, `Effects`, `SecondaryStatEffects`, `PlayerQuestData`, `BonusStats`, `Techniques`, `DynamicGrowthData`. (There is no `Training` object; training config lives in `config/dragonminez/training.json`.) Sub-objects live in `common/stats/character/`, `common/stats/skills/`, `common/stats/techniques/`, `common/stats/extras/` (DynamicGrowthData), and `common/quest/` (PlayerQuestData).
- NBT keys written by `save()`: `Stats`, `Status`, `Cooldowns`, `Character`, `Resources`, `Skills`, `Effects`, `SecondaryStatEffects`, `PlayerQuestData`, `BonusStats`, `Techniques`, `DynamicGrowth`, `HasInitializedHealth`. These are the save-format contract for all three storage backends — renaming any silently orphans existing NBT/JSON/DB data.
- `load()` is a per-key partial loader EXCEPT it hard-throws (`ClassNotFoundException`, wrapped in RuntimeException) when the `PlayerQuestData` key is absent. Treat `load()` as a full-document loader, not a safe partial merge.
- Clone (death/dimension): `onPlayerClone` revives caps, `newData.copyFrom(oldData)` (deep copies; PlayerQuestData round-trips through NBT), invalidates old caps. A client-side `static StatsData CLIENT_CACHE` preserves character-creation state across clone only.
- Login sync (`onPlayerLogin`): sends every config file except `general-user` via `SyncServerConfigS2C` (first packet `reset=true`), `SyncQuestRegistryS2C`, then per-player fixes (visited dimension, unlock `saiyan_saga`, form-selection defaults, clear stun/knockdown/strike-lock, skill-name repair, deactivate kisense) and a full `StatsSyncS2C`.
- Adding a state group requires updating ctor + `save()` + `load()` + `copyFrom()` together, and deciding which sync packet carries it.

Sync packets (all decode into the same client-side `data.load(nbt)`):

- `StatsSyncS2C` = full `data.save()` (login, dimension change).
- `ProgressionSyncS2C` = `Stats` + `BonusStats` + `Skills` + `Techniques` + `PlayerQuestData` (after progression changes, `/dmzreload`).
- `ResourceSyncS2C` = `Resources` + `Status` only (respawn, resource changes). Because `load()` throws without `PlayerQuestData`, this packet relies on the client already having full data — be careful changing `load()` or packet contents.

Ticking is split across THREE PlayerTick handlers:

- `StatsCapability.onPlayerTick` (server, END phase) → `StatsData.tick()` which only ticks `Cooldowns`.
- `server/events/players/TickHandler` — the real gameplay loop: regen (every 20 ticks), sync (every 10), effects/secondary-stat-effects/techniques ticking, aura light, stun handling.
- `server/events/players/StatsEvents` — weights, food-regen queue, health. Also applies the max-health `AttributeModifier` (`DMZ_HEALTH_MODIFIER_UUID`); energy/stamina/poise are custom attributes from `MainAttributes` (`MAX_ENERGY`, `MAX_STAMINA`, `MAX_POISE`). `hasInitializedHealth` (persisted) gates the one-time full heal.

Race, class, form, stack-form, effect, mastery, TP, and stat calculations read `ConfigManager` live on every call — there is no memoization, so `/dmzreload` changes results immediately.

## Networking

`NetworkHandler` creates a Forge `SimpleChannel` named `dragonminez:network` (protocol "1.0", all versions accepted). ~54 C2S and ~25 S2C packet classes live in `common/network/C2S` and `common/network/S2C`.

- Packet ids come from a sequential `id()` counter, so **registration order defines the wire ids**. Append new packets; never reorder or insert mid-list, or client/server builds desync.
- Each registration is a `messageBuilder(...).decoder(...).encoder(...).consumerMainThread(handle).add()` triplet with explicit `NetworkDirection`.
- Helper send methods: `sendToServer`, `sendToPlayer`, `sendToAllPlayers`, `sendToTrackingEntityAndSelf`, `sendToTrackingEntity`.
- C2S categories: character creation/update, stat/skill changes, quests (accept/claim/unlock), wishes (`GrantWishC2S`), space pod travel, actions/forms/release limit, flight/dash/ki blasts, party, techniques, combat. S2C: stat/resource/progression sync, config sync, quest/wish registry sync, radar sync, animations, UI opens, party toasts, weapon registry sync, space pod destinations.
- Validate all C2S input server-side. Never trust the client for stats, unlocks, permissions, quests, wishes, or progression.

## Config System

Runtime config root: `config/dragonminez`. Managed by `common/config/ConfigManager` (static; initialized from `DMZCommon.init()`).

Files loaded/generated:

- `general-user.json` (client-only prefs — never synced), `general-server.json`, `combat.json`, `training.json`, `skills.json`, `techniques.json`, `entities.json`.
- Per race (`DEFAULT_RACES` = human, saiyan, namekian, frostdemon, bioandroid, majin; custom race folders auto-detected): `races/<race>/character.json`, `races/<race>/stats.json`, `races/<race>/forms/*.json`.
- Stack forms: `forms/*.json`.
- (`<world>/dragonminez/wishes/*.json` is wish data owned by `WishManager`, not ConfigManager — see Wishes.)

Versioning rules:

- Config version is a **semver String** `"X.Y.Z"` (`ConfigManager.CONFIG_VERSION`, mirrored by each config class's `CURRENT_VERSION`/`configVersion`). `parseSemver` requires exactly 3 numeric components; anything else (all legacy `double`/`int` versions like `21.0`, `21.10`, `12`) is treated as outdated and forces an upgrade. Do NOT go back to numeric versions — `21.10` as a `double` equals `21.1`.
- Outdated or missing-version default config files are MOVED to `config/dragonminez/oldBackup/<same relative path>` (`backupOldConfig`) and regenerated (the three-way merge re-applies the author's still-valid edits). `oldBackup/` is excluded from config scans. The `old_` filename prefix is only a skip-filter for user-authored form files — it is NOT the backup mechanism.
- **Unparsable (malformed-JSON) files are NEVER moved or overwritten** — that would throw away the author's in-progress edits. `loadAndValidate` returns in-memory defaults for the session and leaves the file exactly as-is; the same guard covers default form files (`createOrLoadRace`/`createOrLoadStackForms` only write a default when the file is genuinely missing). Malformed race/stack/user form files, and malformed quest/saga/sidequest/wish files, are likewise skipped (not regenerated). All of these are recorded in `JsonLoadReport` so the author is told which file to fix; a syntax fix + `/dmzreload` restores their values. This is deliberate: brand-new custom quests/forms that are broken are reported, not salvaged.
- Regeneration merges **three-way** against the previous release's defaults bundled at `resources/data/dragonminez/previousConfigs/<same relative path>` (`loadBaselineObject`): if the user's value equals the old default it upgrades to the new default (propagates balance changes); if it differs it is kept (respects user edits). New default keys are added; user-added map keys are kept. With no baseline file (custom races/forms, or newly added defaults) it falls back to "preserve any value differing from the new default".
- **Per-release workflow:** before shipping, copy the previous release's generated `config/dragonminez` tree into `resources/data/dragonminez/previousConfigs/` (excluding `oldBackup`/`old_*`), then bump `CONFIG_VERSION`. Skipping this silently weakens the merge for everything without a baseline.
- The pre-`21.2` `DEFENSE_SCALING_FOLD` migration (×0.12) applies only to legacy numeric-version stats files and only folds user-modified `defenseScaling` values, avoiding double application.
- Default race/form files are regenerated only when missing or invalid/outdated. Unknown custom races and user-defined form groups are loaded and preserved (`putIfAbsent` over defaults); custom races skip default form generation entirely.

Known dead code / traps inside ConfigManager:

- The `config_defaults` template mechanism (`saveDefaultFromTemplate` reading `/assets/dragonminez/config_defaults/<name>`) points at a resource directory that does not exist; first-run generation silently falls back to no-arg constructor defaults. Do not assume templates seed files.
- The 7-arg `applySyncedServerConfig(...)` batch method is unused dead code; real sync is the per-file `applySpecificSyncedConfig` path.

Client/server config sync:

- On login (and `/dmzreload`), the server sends EVERY config file except `general-user` (`CLIENT_ONLY_CONFIG`), one gzip-compressed `SyncServerConfigS2C` per file; the first packet carries `reset=true` which begins a fresh sync batch. Payloads route by path into `SERVER_SYNCED_*` fields, and all `get*Config` getters return synced values while `serverSyncActive`. The client clears sync on disconnect (`ConfigManager.clearServerSync()` in `ForgeClientEvents`).
- `/dmzconfig` (ConfigCommand) live-edits single keys via `updateConfigValue` → `reloadSpecificConfig`, which **bypasses** version checks, oldBackup, and the three-way merge (straight deserialize + single-file sync). The in-game editor `DMZConfigEditScreen` persists via `saveRawConfig`. Only full `reload()`/`initialize()` run the validation+merge pipeline.

## Quest System

Quest data is world-save JSON, loaded by `common/quest/QuestRegistry.loadAll(MinecraftServer)` from `ForgeCommonEvents.onServerStarting` and `/dmzreload`. Runtime world structure:

```text
<world>/dragonminez/
  sagas/        # saga manifest JSON (recursive scan)
  quests/<questFolder>/   # quest files, loaded alphabetically (01_...json ordering)
  sidequests/   # side-quest JSON (recursive scan)
```

Key behaviors:

- Default generation: sagas require BOTH `storyModeEnabled` AND `createDefaultSagas`; side quests require only `createDefaultSideQuests` (independent of story mode). Runtime load gates are separate: sagas load only when `storyModeEnabled`, side quests only when `sideQuestsEnabled`.
- Default files are now **version-upgraded in place** by `QuestUpgrader` (the quest-side analogue of the `ConfigManager` three-way merge). Every generated quest/saga/side-quest is stamped with `"defaultsVersion"` (`QuestUpgrader.DEFAULTS_VERSION`, a semver string). On load, `QuestDefaults`/`SagaDefaults`/`SideQuestDefaults` route through `QuestUpgrader.upgradeOrWrite`: missing → write fresh; stamp equals current → skip (fast path); stamp missing/older → three-way merge the user file against the shipped baseline (`resources/data/dragonminez/previousQuests/<same relative path>`) and the new code default. Merge rule (matches the config merge, plus a conflict flag): user==old-default → take new default (balance fix propagates); new==old → keep user; **both changed → keep the user's value and record a conflict** (never destructive). User-added keys preserved; arrays (objectives/rewards) merge only when structurally aligned, else whole-array conflict → keep user. The pre-merge file is copied to `<world>/dragonminez/oldBackup/<relative>` only when the merge made a substantive change (a pure re-stamp does not back up).
- **Per-release baseline workflow (required, like `previousConfigs`):** before shipping a release that changes default quest values, snapshot the *previous* release's generated quest tree (`<world>/dragonminez/{sagas,quests,sidequests}`) into `resources/data/dragonminez/previousQuests/` (same relative layout), then bump `QuestUpgrader.DEFAULTS_VERSION` and apply the new values. With no baseline for a file, the merge falls back to "preserve any user value differing from the new default" (non-destructive, but that release's balance fixes won't auto-apply to edited fields).
- Upgrade results accumulate in `QuestUpdateReport` (cleared each `loadAll`): a summary is logged and written to `<world>/dragonminez/quest_update_report.txt`. The whole feature is gated by the `autoUpdateQuests` server-config flag (default true; false = legacy generate-only-if-missing). The in-chat notification is no longer quest-specific — `loadAll` pushes a one-line summary into the unified `JsonLoadReport` (see below), which surfaces it to players on login.
- **Unified JSON load report (`common/diagnostics/JsonLoadReport`):** a single in-memory collector of load-time problems across all runtime JSON — configs, quests/sagas/sidequests, and wishes. Loaders record `ERROR` entries when a file is malformed/failed (`ConfigManager.loadAndValidate` parse failures, `ConfigLoader` form-file skips, `QuestRegistry` saga/quest/sidequest parse skips, `WishManager.loadWishConfig` skips — each carrying the deepest Gson cause with its `line/column/path`) and `UPDATE` entries when defaults were auto-upgraded. Each subsystem tags its entries with a `source` (`"config"`/`"quests"`/`"wishes"`) and clears only that source at the start of its own load, so `/dmzreload config` doesn't wipe quest findings. `server/events/DataReportEvents.onPlayerLogin` shows the report to **every** player on login (not just ops), gated by the `developer.reportJsonProblemsInChat` server-config flag (default on). Purpose: let server owners/addon authors who hand-edit these files see exactly which file broke and where, instead of a silent reset-to-defaults. Console logs and `oldBackup/`/report files remain authoritative.
- Six default sagas: saiyan → frieza → android, then future and buu both branch from android, movies requires buu.
- Saga quest key is `sagaId:numericId` (the JSON `id` int, not the filename — duplicate ids in one folder silently collide). Side-quest key is the JSON string id.
- Objective types: ITEM, KILL, INTERACT, STRUCTURE, BIOME, DIMENSION, COORDS, TALK_TO, DRAGON_SUMMON, SKILL. Reward types: TPS, ITEM, ALIGNMENT, COMMAND, SKILL, TRANSFORMATION, KI_TECHNIQUE.
- **Reward difficulty (per reward, in `QuestParser.parseReward`):** each reward carries an `EnumSet<Difficulty>` (`QuestReward.difficulties`, default = all) parsed from an optional `difficulty` field (aliases `difficulties`/`difficultyType`/`minDifficulty`). The value is an **explicit allow-set**, accepted as a JSON array (`["NORMAL","HARD"]`), a comma/space string (`"NORMAL, HARD"`), or a single name (`"HARD"`); unknown tokens are dropped, absent/empty = all difficulties. `isUnlockedFor(d)` is set membership — NOT a minimum threshold, so `"NORMAL"` means Normal-only and `["EASY","HARD"]` is expressible. The old `hard:`/`normal:` **type-string prefix was removed** (a leftover `"type":"normal:ITEM"` now fails to match and the reward is dropped). `SyncQuestRegistryS2C.serializeReward` mirrors this: plain type name + a `difficulty` array (only when the set is a strict subset), re-parsed client-side via `QuestParser`. `QuestService.claimAvailableRewards` gives a reward only when `isUnlockedFor(pqd.getDifficulty())`, scaling ITEM/TPS/ALIGNMENT amounts by `Difficulty.questRewardMultiplier()`; SKILL/TRANSFORMATION/KI_TECHNIQUE/COMMAND are unlocks and don't scale. Tree + NPC panels group rewards by difficulty-set via `QuestTextFormatter.groupRewardsByDifficulty`/`describeRewardDifficulties` (header lists the set, e.g. "Only: Normal, Hard"; locked groups greyed). Defaults (`QuestDefaults`/`SideQuestDefaults`) tag rewards with the `onlyOn(reward, "NORMAL", "HARD")` helper.
- `loadAll` builds indexes (objective type, quest giver, turn-in) used by `QuestService`. `loadAll` does NOT sync to clients — `SyncQuestRegistryS2C` is sent from `StatsCapability.syncToClient()` on login and from `ReloadCommand`; reloading quests outside those paths leaves clients stale until relog.
- `SyncQuestRegistryS2C` carries two JSON strings capped at 1 MiB each; the standalone-quests blob excludes SAGA-type quests (sagas travel in the sagas blob).
- Kill objectives: contiguous KillObjectives in a sequential quest unlock and count as one block (`QuestEvents.isKillObjectiveUnlocked` walks back to the block start). Default `CountMode.QUEST_SPAWNED_ONLY` requires the killed entity to carry `dmz_quest_key` + `dmz_quest_objective_index` + `dmz_quest_owner` tags (written by `QuestService.spawnKillObjectives`, which also writes stat/AI-tier/transformation-override tags).
- Quest failure: party wipe fails only quests that have at least one KillObjective; `failQuest()` increments failureCount and resets progress.
- `PlayerQuestData` NBT: `difficulty`, `questState`, `difficultyChosen`, `partyState`, with legacy migration from `difficultyStates`/`hardModeEnabled`. Party join adopts the leader's difficulty wholesale.
- Server code uses `getQuest()/getSaga()`; client GUI uses `getClientQuest()/getClientSaga()` — cross-side calls silently return empty.

Quest update rules: preserve filename ordering conventions, translation-key titles, and user-authored addon quests when changing the parser schema.

## Wish And Dragonball Systems

Two interleaved pipelines — definitions (ball sets, radars, dragons) and wishes — with three sources: hardcoded bootstrap, external addon packs, and per-world overrides. There is **no in-jar datapack wish/definition JSON**: `DragonDefinitionReloadListener` and `DragonWishRegistry` are `SimpleJsonResourceReloadListener`s rooted at `dragonminez/dragonballs` but both IGNORE the scanned datapack JSON and call `DragonBallPackManager.loadAll()` instead. Adding standard datapack JSON under that path does nothing.

- `DragonBallDefinitions` holds bootstrap maps (hardcoded earth/namek sets, shenron/porunga, radars, recipes) plus runtime maps set on reload. `syncRegisteredBlocks()` copies Forge `RegistryObject<Block>` refs from bootstrap into same-id runtime definitions after every reload — addon packs cannot introduce new blocks at runtime; new blocks need a `Main*` deferred register.
- `DragonBallPackManager.loadAll()` scans folder and `.zip` packs under the game-dir `dragonballs` root (plus sibling candidates), routing files by suffix (`definitions/{ballset,radar,dragon,wishes}.json`, `assets/...`). It runs three times per boot/reload (bootstrap static init, definition listener, wish listener).
- `WishManager.loadWishes(server)` runs at `onServerStarting` (not on datapack reload): starts from addon-pack wishes, then merges `<world>/dragonminez/wishes/*.json` (auto-creating default `shenron.json`/`porunga.json`). World-save wish files are **plain JSON arrays**; the dragon id is the filename stem, and a file fully replaces that dragon's wish list. The `{"dragon": ..., "wishes": [...]}` object wrapper is the addon-pack format only. Note: a vanilla `/reload` re-runs the listeners but NOT `WishManager.loadWishes` — world-save overrides need `/dmzreload` or restart.
- Wish types (registered in `common/util/WishTypeAdapter`, 10 total): `item`, `command`, `tps`, `multi_wish`, `skill`, `passivereset`, `recustomize`, `relocatestats`, `changedifficulty`, `resetstory`.
- Grant flow: owner right-clicks `DragonWishEntity` → wish screen (shenron 1 wish, porunga 3) → `GrantWishC2S` with indices → server validates against `WishManager.getAllWishes()` and grants → entity despawns after 100 ticks, resets weather/time, and rescatters balls via `DragonBallsHandler.scatterDragonBalls`.
- Scattering (`server/events/DragonBallsHandler`): first spawn scatters full sets, later scatters cap to `maxSets`; positions are XZ-random around world spawn, tracked as "pending" in `DragonBallSavedData` (per-level SavedData, key `dragon_balls_data`, with legacy Earth/Namek key migration) until the chunk loads or a player comes within 128 blocks, then placed on the heightmap. Radar sync (`RadarSyncS2C`) includes pending balls and fires on scatter, place/break, login, dimension change, and a 100-tick timer.
- `DragonBallDataPackResources` is a synthetic PackResources that only injects crafting recipes for addon radar items.

## Datapack Resource Systems

Space pod destinations:

- `common/spacepod/SpacePodDestinationRegistry` is a JSON reload listener; root key `destinations`, supports `replace`. Fields: id/name/translation, dimension, icon, optional coordinates, visibility, unlock rules (`SpacePodUnlockExpression`).

Weapon attributes:

- `WeaponRegistry.loadAttributes(resourceManager)` (wired as an inline reload listener) loads all `weapon_attributes/*.json` from all namespaces, rewrites `bettercombat:` animation namespaces to `dragonminez:`, fuzzy-matches missing animation names against an `animations/*.json` cache, resolves parent/override containers, and syncs the encoded registry to clients.

## Storage

`StorageManager` (`server/storage`) selects the player-data backend from `general-server.json` (`StorageConfig`, default `NBT`):

- `NBT`: `activeStorage == null`; the whole custom layer (StorageManager/DataSyncHandler/JsonStorage/DatabaseManager) is inert and persistence is purely the vanilla capability path (`StatsProvider` INBTSerializable → player `.dat`).
- `JSON`: `<world>/dragonminez/playerdata_json/<uuid>.json` (pretty-printed via NbtOps/JsonOps).
- `DATABASE`: MariaDB via HikariCP (`jdbc:mariadb` only); gzip NBT in a `MEDIUMBLOB` keyed by uuid. Hikari `poolSize` and async `threadPoolSize` are separate config keys.

Behavior and caveats:

- Vanilla `.dat` capability persistence is ALWAYS on. JSON/DATABASE modes double-persist: the custom store async-loads on login (`DataSyncHandler` on `PlayerLoggedInEvent`) and its data overwrites vanilla-loaded values on the server thread, then a full `StatsSyncS2C` is pushed. Logout saves via `DataSyncHandler`.
- The DB "fallback" is a log message, not a code path: on connection failure `DatabaseManager` stays the active storage and silently no-ops; durability comes only from the vanilla `.dat` path. Nothing switches to JsonStorage/NBT.
- `savePlayer` serializes NBT on the server thread and submits only the IO to the executor; it skips saving when `!isDataLoaded && !hasCreatedCharacter` (blank pre-character guard).
- Auto-save is a hardcoded 5-minute `scheduleAtFixedRate` (not config-driven), JSON/DATABASE only.
- `DMZEvent.PlayerDataLoadEvent` fires before apply (tag mutable), `PlayerDataSaveEvent` after serialize / before write.
- Login load is async and racy: briefly after login the player has vanilla-loaded values until the custom apply runs; a disconnect during load skips apply.
- `/dmzreload` reload saves online players, then `shutdown()`+`init()`; note the async executor is recreated without shutting the old one down (leak on repeated reloads).
- Never block the server thread with DB/filesystem work; never log credentials; preserve `StatsData` NBT key names (cross-backend contract).

## Worldgen And Dimensions

Split between Java registration, datagen output, and static resources:

- Custom dimensions (`server/world/dimension`): Namek, HTC (Hyperbolic Time Chamber), Otherworld, and Sacred Kai (`SacredKaiDimension`, with `SacredKaiFeatures`). `OtherworldRegionLoader` loads pre-generated `.mca` regions at server-about-to-start / server-started / Otherworld level load, gated by `worldGen.otherworldActive`.
- TerraBlender: `ModCommonEvents.commonSetup` registers `OverworldRegion` (weight 40; adds the rocky biome additively) and overworld surface rules.
- `server/world` also contains `biome`, `data` (SavedData like `DragonBallSavedData`, `RaidSavedData`), `feature`, `gen`, `npc` (alignment-driven placement/spawning), `raid` (Raid/RaidManager/RaidTypes/waves/rewards), `structure` (incl. `StructureSpawnPlanner` precompute, `VillagePoolInjector`, placement types, processors), `tree`.
- Generated resources (`src/generated/resources/`): dimensions, dimension types, biomes, configured/placed features, structures, structure sets, template pools, tags, recipes, loot tables, advancements, space pod destinations, dragon definitions/wishes.
- Static resources (`src/main/resources/`): structure `.nbt`, Otherworld regions, biome modifiers, worldgen JSON, weapon attributes.

## Commands And Permissions

`DMZServer.registerCommands()` registers 22 commands from `server/commands`: Stats, RacialSkill, Bonus, Effects, Skills, Tech, Forms, Points, Debug, Mastery, Locate, Party, Story, Revive, Reload, Config, Weight, Raid, Alignment, Tail, Halo, Restore.

- Permissions: Forge PermissionAPI `PermissionNode<Boolean>`s in `DMZPermissions` (~70 nodes, self/others split), default fallback permission level 2. `hasPermission()` contains a hard-coded name-based admin bypass for `ezShokkoh`, `ImYuseix`, `MrBrunoh` — do not replicate this pattern; flag it if touching permissions. `RaidCommand` checks `hasPermission(2)` directly instead of DMZPermissions.
- `/dmzreload [all|config|story|wishes]` (default all): config scope does `ConfigManager.clearServerSync()` + `reload()`, `StorageManager.reload()`, NPC alignment + placement reload/respawn; story scope reloads `QuestRegistry`; wishes scope reloads `WishManager`; then resyncs configs (per-file `SyncServerConfigS2C`), `ProgressionSyncS2C`, `SyncQuestRegistryS2C`, `SyncWishesS2C` to online players. Requires `DMZPermissions.RELOAD`.

## Integrations

- Forge, Minecraft, GeckoLib, TerraBlender, and Curios are mandatory (`mods.toml`).
- Legendary Tooltips, Epic Fight, and Better Combat are hard-incompatible (mods.toml + startup crash in the entry class).
- JEI APIs are compile-only (`JEIDragonMineZPlugin` in `common/compat`); runtime JEI is a client-only dev mod.
- WorldGuard compatibility (`WorldGuardCompat`) is reflective and defensive — no hard dependency.
- Lombok is used at compile time (`compileOnly` + annotationProcessor).
- Crowdin uses `assets/dragonminez/lang/en_us.json` as the source locale (`crowdin.yml` maps locale filenames).

## Localization

- Source of truth: `src/main/resources/assets/dragonminez/lang/en_us.json`. Keep keys stable unless intentionally renaming; add entries for any user-visible text.
- Validate with `scripts/check_lang.sh` (jq). CI runs it in `.github/workflows/languages.yml`, which also uploads the source file to Crowdin on lang changes, on a 2-day schedule, and manually.

## Datagen

`common/datagen/DatagenManager` (mod-bus `GatherDataEvent`) registers providers: recipes (`DMZRecipeProvider`, plus `DMZKikonoRecipeProvider` used by it), loot tables, block states, item models, block/item/entity-type tags, worldgen (`DMZWorldGenProvider`) + biome tags, advancements, space pod destinations, dragon definitions, and dragon wishes.

Run `.\gradlew.bat runData` after changing providers and review the `src/generated/resources/` diff (`.cache/` is excluded from packaging).

## Beta Access Verification

Beta/alpha builds (version contains `-beta`/`-alpha`) reject non-whitelisted players in `ForgeCommonEvents.onPlayerLogin`. `BetaWhitelist` loads asynchronously from `https://raw.githubusercontent.com/DragonMineZ/.github/refs/heads/main/allowed_betatesters.txt` at server start with a small hardcoded fallback list. The disconnect is detected client-side by `DisconnectedScreenMixin`, which adds a `Verify here` button opening `BetaAccessVerificationScreen` → browser to `https://downloads.dragonminez.com/beta-access/start?username=<minecraft_username>` (override with JVM property `dragonminez.betaAccessUrl`).

The matching `dragonminez-ai` bot flow is intentionally narrow: Discord OAuth → guild member → Patreon beta roles → existing Patreon link + whitelist PR auto-approval. Keep it a small verification bridge, not a general public API.

## CI And Release Automation

Workflows in `.github/workflows/`:

- `gradle.yml` — Java CI: builds on PRs and pushes to `main` touching Java/build files; submits the Gradle dependency graph.
- `languages.yml` — Crowdin check & upload (see Localization).
- `codeql.yml` — CodeQL analysis. PMD rulesets live in `.github/workflows/pmd/`.
- `dev-jar-upload.yml` — dev distribution: runs on pushes to **`main`** (paths-filtered to Java/resources/Gradle/workflow inputs) and manual dispatch. Builds and uploads ONLY the no-classifier jar `build/libs/dragonminez-<mod_version>.jar` (never `-slim`) via SSH/SCP to the VPS with pinned host keys (vars `DMZ_VPS_HOST`/`DMZ_VPS_PORT`; secrets `DMZ_VPS_USER`/`DMZ_VPS_SSH_KEY`/`DMZ_VPS_KNOWN_HOSTS`/`DMZ_VPS_UPLOAD_DIR`). Upload is atomic (temp name → move), chmod `0644`, SHA-256 verified, then prunes old artifacts keeping the two newest `dragonminez-*__*__*.jar`. After upload it notifies the Discord bot locally on the VPS at `http://127.0.0.1:8088/dmz-dev-jar` (bypassing Cloudflare) using `DMZ_RELEASE_BOT_WEBHOOK_SECRET`.
- `release-prepare.yml` — validates stable release candidates on `main`, builds the jar, writes manifest artifacts (via `scripts/release/prepare_release.py`), and posts the candidate payload to the Discord bot at `https://release-bot.dragonminez.com/dmz-release` (secret `DMZ_RELEASE_BOT_WEBHOOK_SECRET`).
- `release-publish.yml` — triggered by Discord-bot approval through `repository_dispatch` (`dragonminez_release_approved`) or manual dispatch with the same payload fields. Verifies the approved commit is still `origin/main` HEAD, rebuilds, compares the jar SHA-256 against the approved manifest (**mismatch is a warning, not a failure** — resource optimization is not byte-reproducible), publishes to Modrinth and CurseForge, updates `update.json` (via `scripts/release/update_forge_update_json.py`), and commits it back to `main`.

Rules:

- Do not publish GitHub Release jar assets from these workflows.
- Stable `main` releases only: versions containing `alpha`/`beta` are skipped or rejected.
- The Discord bot must not hold Modrinth/CurseForge tokens; those stay in GitHub Actions secrets.
- Python release helpers live in `scripts/release/` (`prepare_release.py`, `release_common.py`, `update_forge_update_json.py`, `release_tools_test.py`).

## Update Checklist For Future Agents

Before changing behavior:

- Identify whether data is code-owned, datapack-owned, config-owned, world-save-owned, or addon-owned.
- Prefer editing canonical loaders/default factories over hand-editing generated/runtime files.
- If a JSON schema changes, update parser, defaults, config version, sync packets, and docs together. For configs, also refresh the `previousConfigs` baseline at release time.
- If adding content, register it through the appropriate `Main*` class and ensure datagen/assets/lang are complete.
- If adding packets, add the packet class and APPEND its `NetworkHandler.register()` entry (never reorder).
- If adding reloadable data, wire a reload listener and a sync path if clients need it.
- If changing server config used by clients, remember sync is generic per-file, but nested layouts need routing in `applySpecificSyncedConfig`.
- If changing player state, update ctor/save/load/copyFrom/sync/storage expectations together.
- If changing worldgen/datagen, run `runData` and inspect generated resource diffs.
- If editing language files, run `scripts/check_lang.sh` or equivalent JSON validation.
- If editing code, run at least `.\gradlew.bat build` unless blocked, and state any blockers.
