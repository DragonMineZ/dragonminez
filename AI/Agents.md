# AI Agent Onboarding: DragonMineZ

This is the first local AI entry point for DragonMineZ work. Read this before inspecting code, planning changes, or answering implementation questions. Then read `AI/Memory.md` and `AI/Context.md` before acting.

## Operating Rules

- Work from the local repository first. Do not rely on memory when repo files can answer the question.
- Follow the user request exactly. If the user asks for a draft, review, or explanation, do not edit files.
- If you edit, add, delete, or regenerate files, stage changed tracked files before finishing unless the user explicitly says not to stage that file.
- Treat the working tree as shared. Never discard, overwrite, or revert user changes unless explicitly instructed.
- Do not assume backward compatibility is required unless the user asks for it or the change affects public data, save data, player-editable JSON, configs, or addons.
- When backward compatibility is not requested, prefer replacing old code with the new approach instead of layering parallel legacy paths.
- Ask when requirements are ambiguous, destructive, risky, or when compatibility expectations affect the design.
- Preserve user-created content. Many JSON/config systems are intentionally editable by players, addon authors, and server owners.
- Avoid destructive commands such as `git reset --hard`, forced checkout, broad cleanup commands, or recursive deletion.
- Do not edit generated/runtime folders unless the task explicitly targets them: `build/`, `run/`, `run-data/`.
- Keep responses practical, direct, and evidence-based. Avoid vague claims.

## First Steps For Every Task

1. Read this file.
2. Read `AI/Memory.md` for durable user instructions and prohibitions.
3. Read `AI/Context.md` for current project architecture and data-system notes.
4. Check `git status --short` before editing.
5. Inspect only the files needed for the task.
6. Identify whether the task touches code, assets, generated data, runtime/player-editable JSON, configs, networking, storage, worldgen, localization, or dependencies.
7. Ask for clarification only when the answer changes behavior, compatibility, migration, or risk.
8. Make the smallest coherent change that solves the request.
9. Run fresh verification before claiming success.
10. Stage changed tracked files before finishing, respecting explicit user staging limits.
11. Final response should summarize what changed, what was verified, and any remaining risk.

## Tone And Collaboration

Be concise, technical, and direct.

- State assumptions explicitly.
- Prefer concrete file references over broad descriptions.
- Explain tradeoffs when a choice affects maintainability, compatibility, player data, or addon behavior.
- Do not over-explain routine edits.
- If something cannot be verified locally, say so clearly.
- For reviews, list findings first, ordered by severity, with file and line references.

## Repo Snapshot

DragonMineZ is a Forge mod for Minecraft `1.20.1`.

- Java: `17`
- Mod id: `dragonminez`
- Main package: `com.dragonminez`
- Build system: Gradle wrapper
- Main boot path: `DragonMineZ` -> `DMZCommon.init()` -> side init through `DistExecutor`
- Client init: `DMZClient.init()`
- Server init: `DMZServer.init()`
- Registrations are mostly centralized in `DMZCommon` and `common/init/Main*` classes.
- Runtime data-heavy systems load from world/config folders, not only assets.
- Mandatory dependencies: GeckoLib, TerraBlender, Curios. Hard-incompatible (startup crash): Legendary Tooltips, Epic Fight, Better Combat.

## Build And Run Commands

Use the Gradle wrapper from the repository root.

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer
.\gradlew.bat runData
```

Notes:

- `runClient` and `runServer` use `run/`.
- `runData` uses `run-data/` and writes to `src/generated/resources/`.
- `src/generated/resources/` is included as a main resource source set.
- There is no full committed `src/test` suite. Validate gameplay changes with targeted build/run scenarios when possible.
- Language JSON validation helper: `scripts/check_lang.sh`, which expects `jq`.

## Git Safety

- Check status before editing and before final response.
- Do not revert unrelated changes.
- Do not amend commits unless explicitly requested.
- Do not create commits unless requested.
- If unexpected changes appear, inspect whether they conflict with the task. If they do, stop and ask.
- If the user explicitly says not to stage a file, do not stage it.
- If files were changed and not excluded by the user, stage them before finishing.

Useful commands:

```powershell
git status --short
git diff
git diff --staged
git add <changed-files>
```

## DragonMineZ-Specific Rules

### Registrations

- Add new gameplay content through the existing deferred-register pattern in `common/init/Main*`.
- Wire registration through `DMZCommon.init()` when needed.
- Keep registry names stable once exposed to worlds, saves, recipes, assets, or addons.

### Networking

- Register all gameplay packets in `NetworkHandler.register()`.
- Use explicit direction and stable encode/decode/handle triplets.
- Packet ids come from a sequential counter: APPEND new registrations at the end; never reorder or insert mid-list.
- Validate server-side packet handling. Never trust client input for stats, unlocks, permissions, quests, wishes, or progression.

### Player State

- Player state centers on `StatsCapability`, `StatsProvider`, and `StatsData`.
- Be careful with login, clone, death, dimension change, tick, sync, and save/load behavior.
- Avoid client-only authority for persistent gameplay data.

### Runtime JSON And Addons

Many JSON files are not internal-only. Players and addon authors create and edit them.

- Keep JSON human-readable.
- Preserve clear field names and stable semantics.
- Avoid unnecessary nesting, minification, or opaque encoded values.
- When changing a runtime JSON format, consider migration, defaults, validation errors, and backup behavior.
- Do not silently delete unknown user fields unless strict schema enforcement is explicitly required.
- Prefer helpful logging for malformed user files.

Important systems:

- Quests and sagas load/generate under world-save folders `<world>/dragonminez/{sagas,quests,sidequests}`; existing files are never overwritten by defaults.
- Wishes load/generate under `<world>/dragonminez/wishes/*.json` (world save, NOT `config/`); plain JSON arrays, dragon id = filename stem.
- Config files live under `config/dragonminez`, use semver `configVersion` regeneration, and back up outdated files into `config/dragonminez/oldBackup/`.

### Configs

- Preserve `configVersion` meaning.
- If a config schema changes, handle backup/regeneration intentionally.
- Remember configs may be manually edited by server owners or players.

### Storage

- Storage supports `NBT`, `JSON`, and `DATABASE`.
- Inspect `StorageManager` before changing persistence behavior.
- `DatabaseManager` uses MariaDB and HikariCP. On DB failure there is NO active fallback switch — the broken backend silently no-ops and durability comes from the always-on vanilla capability `.dat` persistence.
- Do not log credentials or connection secrets.
- Treat storage changes as high-risk because they can affect existing worlds.

### Worldgen

- Inspect `server/world/*` and common setup before changing regions, surfaces, features, or structures.
- Run data generation when changing providers.
- Keep generated resources in sync when provider changes require it.

### Mixins

- Add mixin entries to `src/main/resources/dragonminez.mixins.json`.
- Keep mixins minimal and defensive.
- Verify dedicated server compatibility for anything touching client classes.

### Optional Integrations

- WorldGuard support is reflective to avoid a hard dependency.
- Keep optional integration code defensive.
- Check `build.gradle.kts` and `META-INF/mods.toml` together when changing dependencies.

### Localization

- Source locale: `src/main/resources/assets/dragonminez/lang/en_us.json`.
- Keep localization keys stable unless intentionally renaming.
- Update language entries when adding user-visible text.
- Validate language JSON when possible.

## Verification Expectations

Run the strongest practical verification for the change.

- Use `.\gradlew.bat build` for broad code validation.
- Use `.\gradlew.bat runData` for datagen/provider changes.
- Use `.\gradlew.bat runClient` or `.\gradlew.bat runServer` for gameplay-sensitive checks when practical.
- Use `scripts/check_lang.sh` or equivalent JSON validation for language changes.
- If a check cannot be run, state why.
- Do not say the build passes unless it passed in this session.

## When To Ask The User

Ask before proceeding when:

- The task may require backward compatibility or migration.
- A change could alter player-created JSON, configs, saves, or addon behavior.
- A change could break existing worlds.
- The best validation requires launching Minecraft and the user did not request that cost.
- Dependency/version work conflicts with offline Gradle/cache state.
- Requirements imply deleting, regenerating, or overwriting large file sets.
- Unrelated local changes conflict with the task.

## Final Response Checklist

Before finishing:

- Confirm whether files were edited.
- If edited, stage changed tracked files unless explicitly told not to.
- State what changed.
- State what verification was run and its result.
- State any verification not run.
- Mention risks or follow-ups only when they matter.
