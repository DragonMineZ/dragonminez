# Quest Addon API & FTB Quests Compat Design

Status: registry API shipped in v2.2; FTB Quests compat designed, not yet implemented.

## 1. Addon objective/reward registry (shipped)

`QuestObjectiveRegistry` and `QuestRewardRegistry` (`common/quest/`) let addons add quest content
types without touching DMZ's parser switches. Register during mod init (e.g. `FMLCommonSetupEvent`):

```java
QuestObjectiveRegistry.register("MEDITATE",
    json -> new MeditateObjective(json.get("seconds").getAsInt()),            // parse quest JSON
    (objective, out) -> out.addProperty("seconds", ((MeditateObjective) objective).getSeconds()), // client sync fields
    objective -> Component.translatable("myaddon.quest.meditate.obj"));       // HUD/journal text

QuestRewardRegistry.register("MY_CURRENCY",
    json -> new MyCurrencyReward(json.get("amount").getAsInt()),
    (reward, out) -> out.addProperty("amount", ((MyCurrencyReward) reward).getAmount()));
```

Contract:
- Objective subclasses use `QuestObjective(String customType, int required)`; rewards use
  `QuestReward(String customType)`. `getTypeKey()` then round-trips the JSON `"type"` through
  parsing (`QuestParser` falls back to the registry on unknown types), client sync
  (`SyncQuestRegistryS2C` writes `getTypeKey()` + registry sync-writer), and display
  (`QuestTextFormatter` falls back to the registry describer; rewards use their own
  `getDescription()`).
- Progress stays event-driven and server-authoritative: addons listen to their own game events
  and advance `PlayerQuestData.setObjectiveProgress(...)` exactly like `QuestEvents` does for
  built-ins, then call `QuestEvents`-style completion checks or rely on turn-in.
- Type keys are normalized to uppercase; keys are forever once quests ship — treat them like
  registry names.
- The old Gson `QuestObjectiveTypeAdapter`/`QuestRewardTypeAdapter` in `common/util` are dead
  code (only self-referenced) and were intentionally not extended.

Related extension points: `DMZTextPlaceholders.register(key, resolver)` for `%key%` text
placeholders, and the dialogue system's condition blocks reuse `QuestParser.parsePrerequisites`
so addon prerequisite conditions would extend that (not yet pluggable — candidate follow-up).

## 2. FTB Quests compat (design)

Goal: let modpack makers drive FTB Quests chapters with DragonMineZ progression and hand out DMZ
rewards, without a hard dependency.

### Dependency strategy
- `compileOnly` on the FTB Quests API (CurseMaven/Modrinth maven), same pattern as JEI.
- Runtime guard: `ModList.get().isLoaded("ftbquests")` gates classloading of a dedicated
  `com.dragonminez.common.compat.ftbquests` package (one entry class, initialized from
  `DMZCommon.init()` behind the guard). No reflection needed given a real API jar; keep every
  FTB import inside the guarded package (WorldGuardCompat precedent for the isolation rule).
- FTB Quests 1.20.1 needs Architectury; both stay optional in `mods.toml` (`ordering=AFTER`,
  `mandatory=false`).

### Direction A (priority): DMZ progression as FTB *tasks*
Register custom `TaskType`s via FTB's `TaskTypes.register(...)`:

| Task type | Completes when | Backed by |
|---|---|---|
| `dmz_quest` | player completes DMZ quest id | `DMZEvent.QuestCompletedEvent` |
| `dmz_saga` | all quests of a saga complete | same event + saga scan |
| `dmz_stat` | STR/DEF/PWR/... >= X | poll on FTB's task tick / stat sync |
| `dmz_level` | `StatsData.getLevel()` >= X | same |
| `dmz_alignment` | alignment in [min,max] | same |
| `dmz_skill` | skill at level >= X | same |
| `dmz_form` | form/mastery unlocked | form unlock path |

Event-driven where DMZ fires events; otherwise cheap polling in the task's `submitTask`/tick.

### Direction A2: DMZ grants as FTB *rewards*
Custom `RewardType`s: `dmz_tps`, `dmz_skill`, `dmz_form`, `dmz_alignment` — each delegates to the
same logic as the DMZ quest rewards (`TPSReward` etc.), so behavior stays identical.

### Direction B (later): FTB state inside DMZ quests
A new DMZ prerequisite condition `FTBQUEST` (`{"type": "FTBQUEST", "quest": "<ftb id>"}`)
evaluated through the compat module; absent FTB Quests, the condition parses but always fails
with a log warning. Needs the prerequisite condition switch to become registry-based first.

### Implementation checklist (one PR)
1. Gradle: add FTB Quests + Architectury `compileOnly` deps (mind offline mode).
2. `compat/ftbquests/FTBQuestsCompat.init()` guarded in `DMZCommon.init()`.
3. Task types (start with `dmz_quest`, `dmz_stat`, `dmz_level`, `dmz_alignment`).
4. Reward types (`dmz_tps`, `dmz_skill`).
5. Lang + icons (reuse form/skill icons in `textures/gui/icons/`).
6. Verify dedicated-server + FTB-absent startup, and `runData` unaffected.

Alternative for packs not on FTB: Heracles uses a similar pluggable task registry; the compat
module boundary (one guarded package per mod) keeps the door open.
