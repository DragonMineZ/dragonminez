# AGENTS.md

## Fast orientation
- Forge mod for Minecraft `1.20.1` (Java 17), mod id `dragonminez` (`gradle.properties`, `src/main/java/com/dragonminez/Reference.java`).
- Main boot path: `DragonMineZ` -> `DMZCommon.init()` -> side init via `DistExecutor` (`DMZClient.init` / `DMZServer.init`).
- Most registrations are centralized in `DMZCommon` (`MainBlocks`, `MainItems`, `MainEntities`, `MainEffects`, `MainMenus`, etc.).
- Runtime data-heavy systems load from world/config folders, not only assets: quests, wishes, race/forms configs.

## Architecture map (what talks to what)
- **Entry + lifecycle**: `src/main/java/com/dragonminez/DragonMineZ.java`, `common/DMZCommon.java`, `common/events/ForgeCommonEvents.java`, `common/events/ModCommonEvents.java`.
- **Player state core**: `common/stats/StatsCapability.java` + `StatsProvider.java` + `StatsData` (capability attach/clone/tick/sync).
- **Networking contract**: `common/network/NetworkHandler.java` (all packet registration and send helpers).
- **Quest system**: `common/quest/QuestRegistry.java` loads/generates JSON under world folder `dragonminez/{sagas,quests,sidequests}` and syncs to client (`SyncQuestRegistryS2C`).
- **Wish system**: `common/wish/WishManager.java` loads/generates `dragonminez/wishes/{shenron,porunga}.json` per world.
- **Storage backend**: `server/storage/StorageManager.java` chooses `NBT`/`JSON`/`DATABASE`; `DatabaseManager` uses MariaDB + Hikari and can fall back when misconfigured.
- **Worldgen**: TerraBlender region/surface setup in `ModCommonEvents.commonSetup`, features in `server/world/*`.

## Build, run, and data workflows
- Use the Gradle wrapper from project root (PowerShell):
```powershell
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer
.\gradlew.bat runData
```
- `runClient`/`runServer` use working dir `run/`; data generation uses `run-data/` and writes to `src/generated/resources/` (`build.gradle.kts` `minecraft.runs`).
- `src/generated/resources/` is included as a resource source set; keep generated outputs in sync with data provider changes.
- No committed test suite is currently present under `src/test`; validate gameplay changes with targeted runClient/runServer scenarios.
- Language JSON validation helper exists: `scripts/check_lang.sh` (uses `jq` on `assets/dragonminez/lang/*.json`).

## Project-specific coding patterns
- Register new gameplay packets in `NetworkHandler.register()` with explicit direction and stable encode/decode/handle triplets.
- For new content, follow deferred register pattern in `common/init/Main*` classes and wire registration from `DMZCommon.init()`.
- Server-start loading pipeline is important: `ForgeCommonEvents.onServerStarting` initializes storage, beta whitelist, wishes, permissions, quests, WorldGuard compat.
- Configs are versioned and auto-regenerated with backup (`old_*`) in `config/dragonminez` via `ConfigManager`; preserve `configVersion` semantics when editing config models.
- Quest/wish defaults are code-generated when missing/corrupt; edits may require both Java defaults (`QuestDefaults`, `SagaDefaults`, `WishManager`) and JSON format compatibility.

## Integrations and boundaries
- Required/optional integration points are declared in `build.gradle.kts` + `META-INF/mods.toml` (Forge, GeckoLib required; TerraBlender optional; JEI compat in `common/compat/JEIDragonMineZPlugin.java`).
- Mixins are enabled through `dragonminez.mixins.json`; add entries there when introducing new mixin classes.
- WorldGuard support is reflective (`common/compat/WorldGuardCompat.java`) to avoid hard dependency; keep compatibility code defensive.
- Avoid editing generated/runtime folders unless task explicitly targets them: `build/`, `run/`, `run-data/`.
- Localization pipeline is Crowdin-based (`crowdin.yml`), source locale is `assets/dragonminez/lang/en_us.json`.

