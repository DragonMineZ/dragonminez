# DragonMineZ AI Context

This file captures project context that future AI agents should read after `AI/Agents.md` and `AI/Memory.md`. Keep it factual and update it when the architecture, workflows, or important repo conventions change.

## Project Snapshot

DragonMineZ is a Minecraft Forge mod for Minecraft `1.20.1`, Java 17, mod id `dragonminez`. The root project name is `DragonMineZ`; archives are named from `mod_id`.

This repo is data-heavy and player-customizable. Many systems load JSON from runtime folders in `config/dragonminez`, world save folders, datapacks, and external addon folders. Do not assume JSON is internal-only or safe to rewrite destructively.

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
- `runClient` and `runServer` use `run/` as working directory.
- `runData` uses `run-data/` and writes to `src/generated/resources/`.
- `src/generated/resources/` is included as a main resource source set.
- `build` depends on reobfuscation tasks.
- There is no committed `src/test` suite currently. Validate gameplay changes with `build`, `runClient`, `runServer`, or targeted in-game scenarios.
- `scripts/check_lang.sh` validates language JSON with `jq`.

## Lifecycle

Primary boot path:

- `DragonMineZ` is the `@Mod` entry point.
- Constructor logs startup, calls `DMZCommon.init()`, then side-specific init through `DistExecutor`.
- Client side calls `DMZClient.init()`.
- Dedicated server side calls `DMZServer.init()`.

`DMZCommon.init()` is the central registration and setup point:

- Initializes `ConfigManager`.
- Initializes quest and wish systems.
- Registers network packets through `NetworkHandler.register()`.
- Initializes GeckoLib.
- Registers most deferred registries: attributes, blocks, block entities, items, fluids, sounds, creative tabs, entities, particles, recipes, menus, effects, enchantments, structure placement types, and overworld features.
- Adds `ModCommonEvents.commonSetup`.
- Registers game rules and damage types.

Server event flow:

- `ForgeCommonEvents.onServerStarting` initializes storage, beta whitelist, wishes, permissions, quests, NPC alignment, NPC placement, WorldGuard compatibility, and first Dragon Ball spawning.
- `ForgeCommonEvents.onServerStarted` loads Otherworld regions and spawns configured NPC placements.
- `ForgeCommonEvents.onServerAboutToStart` preloads Otherworld regions when enabled.
- `ForgeCommonEvents.onAddReloadListeners` registers datapack/resource reload listeners for space pod destinations, dragonball definitions, dragon wishes, and weapon attributes.
- `ForgeCommonEvents.onRegisterCommands` delegates to `DMZServer.registerCommands`.

## Source Layout

Main packages:

- `com.dragonminez.client`: rendering, GUI, model, animation, flight, collision, Crowdin/resource-pack support, client events.
- `com.dragonminez.common`: shared config, registries, stats, networking, quests, wishes, dragonballs, combat, datapack/data generation, compatibility.
- `com.dragonminez.server`: commands, recipes, storage, worldgen, dimensions, server events, utilities.
- `com.dragonminez.mixin`: common and client mixins declared in `src/main/resources/dragonminez.mixins.json`.

Registration pattern:

- New content usually belongs in a `common/init/Main*` class and must be registered from `DMZCommon.init()`.
- New packets must be registered explicitly in `NetworkHandler.register()` with a stable encode/decode/handle triplet and correct direction.
- New mixins require both a Java class and an entry in `dragonminez.mixins.json`.

## Player State

Player state is capability-based:

- Core classes: `StatsCapability`, `StatsProvider`, `StatsData`.
- Capability attaches to players in `AttachCapabilitiesEvent<Entity>`.
- Clone logic copies old data on death/dimension clone and has client cache handling for character creation state.
- Server player ticks call `StatsData.tick()`.
- Login sync sends server config, quests, and stats.
- Dimension changes mark visited dimensions and resync stats.
- Respawn restores health, energy, stamina, and sends resource sync.

`StatsData` owns these major state groups:

- `Stats`
- `Status`
- `Cooldowns`
- `Character`
- `Resources`
- `Skills`
- `Effects`
- `PlayerQuestData`
- `BonusStats`
- `Training`
- `Techniques`

Race, class, form, stack form, effect, mastery, TP, and stat calculations depend heavily on `ConfigManager`.

## Networking

`NetworkHandler` creates a Forge `SimpleChannel` named `dragonminez:network`.

Major packet categories:

- C2S: character creation/update, stat changes, skill changes, quests, wishes, space pod travel, actions, flight/dash/ki blasts, party, techniques, combat.
- S2C: stat/resource/progression/appearance sync, server config sync, quest/wish sync, radar sync, animations, UI open messages, party toasts, weapon registry sync, space pod destination sync.

Use helper send methods:

- `sendToServer`
- `sendToPlayer`
- `sendToAllPlayers`
- `sendToTrackingEntityAndSelf`

## Config System

Runtime config root: `config/dragonminez`.

`ConfigManager` loads and generates:

- `general-user.json`
- `general-server.json`
- `combat.json`
- `skills.json`
- `entities.json`
- race folders under `config/dragonminez/races/<race>/`
- race `character.json`
- race `stats.json`
- race form groups under `races/<race>/forms/*.json`
- stack forms under `config/dragonminez/forms/*.json`

Versioning rules:

- Config classes have `CURRENT_VERSION` and a `configVersion` field.
- Outdated, missing-version, corrupt, or unparsable default config files are moved to `old_<filename>` and regenerated.
- Default race/form files are regenerated only when missing or invalid/outdated.
- Unknown custom race/form files are loaded and preserved.
- Do not remove user-defined form groups or races while regenerating defaults.

Client/server config sync:

- Server sends general server config, combat config, skills config, race stats, race character config, forms, and stack forms to clients.
- `ConfigManager` has server-synced override fields used client-side after sync.
- `/dmzreload` clears server sync, reloads config, reloads storage, quests, NPC data, wishes, and resyncs online players.

## Quest System

Quest data is world-save JSON, not normal assets only.

Runtime world structure:

```text
<world>/dragonminez/
  sagas/
  quests/
  sidequests/
```

`QuestRegistry.loadAll(server)`:

- Resolves the overworld save root.
- Generates default quest files via `QuestDefaults` if story mode and default saga creation are enabled.
- Loads saga manifests from `dragonminez/sagas`.
- Saga manifests reference `questFolder`, then quest files load alphabetically from `dragonminez/quests/<questFolder>`.
- Loads sidequest JSON files when side quests are enabled.
- Builds indexes by objective type, quest giver, and turn-in.
- Syncs loaded sagas and quests to clients through `SyncQuestRegistryS2C`.

Quest update rules:

- Preserve filename ordering conventions for saga quests, such as `01_...json`.
- Preserve translation-key titles/descriptions when used.
- Existing world quest files are not overwritten by default.
- If changing parser schema, keep user-authored addon quests in mind.

## Wish And Dragonball Systems

Wishes are loaded from multiple places and merged.

`DragonWishRegistry`:

- Resource reload listener rooted at `dragonminez/dragonballs`.
- Loads external dragonball packs through `DragonBallPackManager`.
- Ensures every loaded dragon has a wish list.
- Syncs wishes on datapack sync.

`WishManager`:

- Merges datapack/dragonball-system wishes with config overrides from `config/dragonminez/wishes/*.json`.
- Config wish files override by dragon id when they contain `dragon` and `wishes`.
- Supported wish types include `item`, `command`, `tps`, `multi_wish`, `skill`, `passivereset`, and `recustomize`.

`DragonBallPackManager`:

- External addon root is `dragonballs` under game dir and candidate nearby roots.
- Supports folder packs and `.zip` packs.
- External definitions merge over bootstrap definitions in `DragonDefinitionReloadListener`.

## Datapack Resource Systems

Space pod destinations:

- `SpacePodDestinationRegistry` is a JSON reload listener.
- JSON root has `destinations`.
- Supports `replace` to clear previous destination list.
- Destination fields include id/name/translation, dimension, icon, optional coordinates, visibility, and unlock rules.

Weapon attributes:

- `WeaponRegistry.loadAttributes(resourceManager)` loads all `weapon_attributes/*.json` resources from all namespaces.
- It rewrites `bettercombat:` animation namespace strings to `dragonminez:`.
- It builds an animation-name cache from `animations/*.json` and fuzzy-matches missing animation names.
- It resolves parent/override containers and syncs the encoded registry to clients.

## Storage

`StorageManager` chooses player storage from server config:

- `NBT`: vanilla capability storage.
- `JSON`: world folder `dragonminez/playerdata_json/<uuid>.json`.
- `DATABASE`: MariaDB/MySQL through HikariCP.

Storage behavior:

- `DataSyncHandler` loads custom storage on server player login and saves on logout.
- Custom storage uses async executor threads from config.
- Auto-save runs periodically for JSON/DATABASE modes.
- Save/load posts player data events.
- Database mode can fall back when credentials are missing or connection fails.

Be careful when changing storage:

- Do not block the server thread with database or filesystem work.
- Preserve NBT key names unless performing an intentional migration.
- Avoid saving blank data before character creation unless the existing guard is intentionally changed.

## Worldgen And Dimensions

Worldgen is split between Java registration, datagen output, and static resources.

Important pieces:

- `ModCommonEvents.commonSetup` registers TerraBlender region/surface rules.
- `server/world` contains biome, dimension, feature, gen, NPC, region, structure, placement, and tree logic.
- Generated resources include dimensions, dimension types, biomes, configured/placed features, structures, structure sets, template pools, tags, recipes, loot tables, and advancements.
- Static resources include structures, regions, biome modifiers, worldgen JSON, and weapon attributes.

Dimensions include Namek, HTC, and Otherworld. Otherworld has pre-generated region loading during server startup/level load paths when enabled.

## Commands And Permissions

`DMZServer.registerCommands()` registers server commands from `server/commands`, including stats, racial skills, bonuses, effects, skills, techniques, forms, points, debug, mastery, locate, party, story, revive, and reload.

`/dmzreload` is important for development and server operators:

- Requires reload permission.
- Reloads config, storage subsystem, quest registry, NPC alignment/placement, and wishes.
- Resyncs config, progression, quest registry, and wishes to online players.

## Integrations

- Forge and Minecraft are required.
- GeckoLib is required.
- TerraBlender is used for regions/surface rules.
- JEI APIs are compile-only; runtime JEI is a client-only dev mod unless excluded for server/data tasks.
- WorldGuard compatibility is reflective and defensive. Do not introduce a hard dependency unless explicitly requested.
- Mixins are enabled by `dragonminez.mixins.json`.
- Crowdin config uses `assets/dragonminez/lang/en_us.json` as source.

## Localization

Primary language file:

- `src/main/resources/assets/dragonminez/lang/en_us.json`

Crowdin config:

- `crowdin.yml` maps Crowdin locales to Minecraft locale filenames.
- Keep new translation keys in `en_us.json`.
- Validate language JSON with `scripts/check_lang.sh` when editing lang files.

## Datagen

`DatagenManager` registers providers for recipes, loot tables, block states, item models, block/item tags, worldgen, biome tags, advancements, space pod destinations, dragon definitions, and dragon wishes.

Run `.\gradlew.bat runData` after changing providers and review `src/generated/resources/`.

## Update Checklist For Future Agents

Before changing behavior:

- Identify whether data is code-owned, datapack-owned, config-owned, world-save-owned, or addon-owned.
- Prefer editing canonical loaders/default factories over hand-editing generated/runtime files.
- If a JSON schema changes, update parser, defaults, config version, sync packets, and docs together.
- If adding content, register it through the appropriate `Main*` class and ensure datagen/assets/lang are complete.
- If adding packets, add both packet class and `NetworkHandler.register()` entry.
- If adding reloadable data, wire a reload listener and sync path if clients need it.
- If changing server config used by clients, update config sync and apply logic.
- If changing player state, update save/load/copy/sync/storage expectations.
- If changing worldgen/datagen, run `runData` and inspect generated resource diffs.
- If editing language files, run `scripts/check_lang.sh` or equivalent JSON validation.
- If editing code, run at least `.\gradlew.bat build` unless blocked, and state any blockers.

## Release Automation

Release automation is split between dev distribution and stable release publishing.

## Beta Access Verification

Beta/alpha builds reject non-whitelisted players during `ForgeCommonEvents.onPlayerLogin`. The disconnect reason is detected client-side by `DisconnectedScreenMixin`, which adds a `Verify here` button for DragonMineZ beta-access disconnects only.

The button opens `BetaAccessVerificationScreen`, which launches the browser to `https://downloads.dragonminez.com/beta-access/start?username=<minecraft_username>` by default. The URL can be overridden for local/dev testing with the JVM system property `dragonminez.betaAccessUrl`.

The matching `dragonminez-ai` bot flow is intentionally narrow: a GET start route redirects through Discord OAuth, resolves the Discord guild member, checks the configured Patreon beta access roles, and then reuses the existing Patreon OAuth/link plus whitelist PR auto-approval logic. This should stay a small verification bridge, not a general public API.

Development jar uploads:

- `.github/workflows/dev-jar-upload.yml` runs on pushes to non-`main` branches when Java, resource, Gradle, or workflow inputs change.
- It runs `./gradlew build` and uploads only the no-classifier jar, `build/libs/dragonminez-<mod_version>.jar`; it must not upload the `-slim.jar`.
- Upload uses SSH/SCP to the VPS with pinned host key checking. Required GitHub repository variables are `DMZ_VPS_HOST` and `DMZ_VPS_PORT`; required GitHub secrets are `DMZ_VPS_USER`, `DMZ_VPS_SSH_KEY`, `DMZ_VPS_KNOWN_HOSTS`, and `DMZ_VPS_UPLOAD_DIR`.
- The jar is uploaded to a temporary remote filename, moved into place atomically, chmodded `0644`, and verified by SHA-256 after upload.
- After a verified upload, the workflow prunes old dev artifacts in `DMZ_VPS_UPLOAD_DIR`, keeping only the two newest files matching `dragonminez-*__*__*.jar`.
- The Discord bot owns download authorization, key rotation, and any future public domain routing around the uploaded jar.

Stable release automation is split into two GitHub Actions workflows:

- `.github/workflows/release-prepare.yml` validates stable release candidates on `main`, builds the jar, writes release manifest artifacts, and posts a release candidate payload to the Discord bot at the constant URL `https://release-bot.dragonminez.com/dmz-release`.
- `.github/workflows/release-publish.yml` is triggered by Discord bot approval through `repository_dispatch` event `dragonminez_release_approved`; it rebuilds the approved `main` commit, verifies the jar checksum, publishes to Modrinth and CurseForge, updates `update.json`, and commits that file back to `main`.

Important release workflow rules:

- Do not publish GitHub Release jar assets from these workflows.
- Stable `main` releases only: versions containing `alpha` or `beta` are skipped or rejected.
- The Discord bot must not hold Modrinth or CurseForge tokens; those stay in GitHub Actions secrets.
- Release candidate webhooks use the Discord bot tunnel host `release-bot.dragonminez.com` and GitHub secret `DMZ_RELEASE_BOT_WEBHOOK_SECRET`. Dev jar upload notifications intentionally bypass Cloudflare: after the jar upload, `.github/workflows/dev-jar-upload.yml` SSHes to the VPS and posts to the bot locally at `http://127.0.0.1:8088/dmz-dev-jar` using the same `DMZ_RELEASE_BOT_WEBHOOK_SECRET`.
