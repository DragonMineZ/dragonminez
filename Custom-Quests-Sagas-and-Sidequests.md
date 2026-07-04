# Custom Quests, Sagas & Sidequests

You can add your own story content with JSON alone — no Java code or addon jar required. This is per-**world** data (unlike races/forms, which live in `config/dragonminez`), so it lives inside the world save.

## Where files go

```text
<world save>/dragonminez/
  sagas/                     # saga manifests
  quests/<questFolder>/      # quest files for one saga, loaded alphabetically
  sidequests/<category>/     # standalone side quests
```

Reference: `QuestRegistry.loadAll(server)` (`src/main/java/com/dragonminez/common/quest/QuestRegistry.java`). See [[Locating specific folders|Locating-specific-folders]] for the full folder map.

DMZ generates the default sagas/quests/sidequests on first load if these toggles in `general-server.json` are on (all default to `true`):

| Field | Purpose |
| :-- | :-- |
| `storyModeEnabled` | Master switch for the saga/quest system. |
| `createDefaultSagas` | Auto-generate DMZ's default saga files if `sagas/` is empty. |
| `sideQuestsEnabled` | Master switch for the sidequest system. |
| `createDefaultSideQuests` | Auto-generate DMZ's default sidequests if `sidequests/` is empty. |

Your own custom saga/quest/sidequest files are never overwritten — DMZ only fills in its own defaults when the folders are missing content.

## 1) Create a saga manifest

One file per saga in `dragonminez/sagas/`, e.g. `my_saga.json`:

```json
{
  "id": "my_saga",
  "name": "dmz.saga.my_saga",
  "requirements": {
    "previousSaga": "saiyan_saga"
  },
  "questFolder": "saga_my_saga"
}
```

| Field | Type | Notes |
| :-- | :-- | :-- |
| `id` | String | Unique saga id. |
| `name` | String | Translation key for the saga's display name. |
| `requirements.previousSaga` | String | Saga id that must be completed first, or `""` for none. |
| `questFolder` | String | Subfolder name under `dragonminez/quests/` holding this saga's quest files. |

Source: `SagaDefaults.java`, `Saga.java`.

## 2) Add quest files for that saga

Inside `dragonminez/quests/saga_my_saga/`, one file per quest, named so they sort in play order (`01_...json`, `02_...json`, ...). Filename order **is** quest order within the saga.

```json
{
  "id": 1,
  "title": "dmz.quest.my_saga1.name",
  "description": "dmz.quest.my_saga1.desc",
  "type": "SAGA",
  "category": "saga_my_saga",
  "parallel_objectives": false,
  "party_scaling": true,
  "secret": false,
  "claim_mode": "TREE_OR_NPC",
  "requirements": {
    "operator": "AND",
    "conditions": [
      { "type": "LEVEL", "minLevel": 1 },
      { "type": "DIMENSION", "dimension": "minecraft:overworld" }
    ]
  },
  "objectives": [
    { "type": "KILL", "entity": "dragonminez:saga_raditz", "count": 1, "health": 450.0, "meleeDamage": 22.0, "kiDamage": 39.0 }
  ],
  "rewards": [
    { "type": "TPS", "amount": 2400 },
    { "type": "ITEM", "item": "dragonminez:broken_scouter", "count": 1 }
  ]
}
```

Saga quests and sidequests share the exact same schema — the fields below apply to both.

| Field | Type | Notes |
| :-- | :-- | :-- |
| `id` | Number or String | **Numeric** for saga quests. Sidequests use a **string** id instead. |
| `title` / `description` | String | Translation keys. |
| `type` | String | `SAGA`, `SIDEQUEST`, `DAILY`, or `EVENT`. |
| `category` | String | Free-form grouping label, defaults to `"general"`. |
| `parallel_objectives` | Boolean | If `true`, all objectives must progress simultaneously instead of in sequence. |
| `party_scaling` | Boolean | If `true`, objective counts scale up per extra party member (`defaultQuestPartyMultiplier` in `general-server.json`, default `1.45`). |
| `secret` | Boolean | Hides the quest from the quest log until started/available. |
| `claim_mode` | String | `TREE_OR_NPC` (claim from the quest UI or an NPC) or `NPC_ONLY` (must talk to the turn-in NPC). |
| `quest_giver` | String or `null` | NPC id that offers the quest (see [Quest giver / turn-in](#quest-giver--turn-in-npcs)). |
| `turn_in` | String or `null` | NPC id that accepts the completed quest. |
| `prerequisites` | Object | Gate on whether the quest is **unlockable** at all. Same schema as `requirements` (see below). |
| `requirements` | Object | Gate on whether the quest can be **started** right now (e.g. current dimension/biome/level). |
| `objectives` | Array | See objective types below. |
| `rewards` | Array | See reward types below. |

Source: `QuestParser.parseQuest`, `Quest.java`.

## Objective types

Each entry in `objectives` needs a `type` plus that type's fields:

| `type` | Fields | Notes |
| :-- | :-- | :-- |
| `ITEM` | `item`, `count` | Turn in/collect an item. |
| `KILL` | `entity`, `count`, `health`*, `meleeDamage`*, `kiDamage`*, `spawn`*, `count_mode`*, `AITier`*, `TextureVariant`*, `CanTransform`* | `entity` can be a registry id or an entity tag (`#minecraft:zombies`). `spawn`: `QUEST` (spawned for the quest) or `NATURAL`. `count_mode`: `QUEST_SPAWNED_ONLY` or `ANY_MATCHING`. |
| `INTERACT` | `entity`*, `entityName`* | Right-click interact with a matching entity. |
| `STRUCTURE` | `structure` | Player must enter a generated structure. |
| `BIOME` | `biome` | Player must be standing in this biome (or biome tag). |
| `DIMENSION` | `dimension` | Player must be in this dimension. |
| `COORDS` | `x`, `y`, `z`, `radius`* | Player must be within `radius` blocks (default `10`) of a point. |
| `TALK_TO` | `npcId` | Player must talk to a specific NPC/master. |
| `DRAGON_SUMMON` | `dragon`*, `ball_set`* | Player must summon a specific dragon and/or ball set (aliases: `dragon_id`/`dragonId`, `ballSet`/`ball_set_id`/`ballSetId`/`set`). |
| `SKILL` | `skill`, `level`* | Player must reach a skill level (aliases: `skillId`/`id`, `minLevel`/`required`). |

`*` optional, sensible defaults apply. Source: `QuestParser.parseObjective`, classes under `common/quest/objectives/`.

## Reward types

Each entry in `rewards` needs a `type` plus its fields. Prefix the type with `hard:` or `normal:` (e.g. `"hard:ITEM"`) to only grant it on a specific difficulty — no prefix grants it on all difficulties.

| `type` | Fields | Notes |
| :-- | :-- | :-- |
| `ITEM` | `item`, `count`* (default `1`) | Grants an item stack. |
| `TPS` | `amount` | Grants training points. |
| `ALIGNMENT` | `amount` | Adjusts player alignment. |
| `SKILL` | `skill`, `level` | Sets/raises a skill to at least this level. |
| `TRANSFORMATION` | `formGroup`, `formName`, `mastery`* (default `100.0`), `stack`* (default `false`) | Unlocks a race form (or stack form if `stack: true`) and grants at least that much mastery. |
| `KI_TECHNIQUE` | `code` | Grants a ki technique from an exported technique code (aliases: `techniqueCode`/`technique_code`). |
| `COMMAND` | `command`, `translationKey`* | Runs a command server-side with `%player%` replaced by the player's name. |

Source: `QuestParser.parseReward`, classes under `common/quest/rewards/`.

## Prerequisites & requirements

Both `prerequisites` (can this quest ever be unlocked) and `requirements` (can it be started right now) use the same condition-tree schema:

```json
{
  "operator": "AND",
  "conditions": [
    { "type": "LEVEL", "minLevel": 30 },
    { "type": "SAGA_QUEST", "sagaId": "saiyan_saga", "questId": 8 },
    { "operator": "OR", "conditions": [ ... ] }
  ]
}
```

`operator` is `AND` or `OR`. `conditions` can nest another `{ "operator": ..., "conditions": [...] }` block.

| `type` | Fields |
| :-- | :-- |
| `SAGA_QUEST` | `sagaId`, `questId` (int) — a specific saga quest must be completed. |
| `QUEST` | `questId` (string) — a specific sidequest must be completed. |
| `LEVEL` | `minLevel` |
| `STAT` | `stat` (`STR`/`SKP`/`RES`/`VIT`/`PWR`/`ENE`), `minValue` |
| `RACE` | `race` (aliases: `raceName`/`race_name`) — the player's race id, case-insensitive. Built-in ids: `human`, `saiyan`, `namekian`, `frostdemon`, `bioandroid`, `majin`. **Watch out:** it's `frostdemon` (not `frieza`/`colddemon`) and `bioandroid` (not `android`/`cell`). Custom races use whatever id they register in `config/dragonminez`. |
| `CLASS` | `class` (aliases: `className`/`class_name`/`characterClass`) — the player's class id, case-insensitive. |
| `SKILL` | `skill`, `minLevel`* (aliases: `skillId`/`id`, `level`/`required`) |
| `BIOME` | `biome` |
| `DIMENSION` | `dimension` |
| `STRUCTURE` | `structure`, `hint`* (`{ "dimension", "x", "y", "z" }`, all optional — helps point the player toward it) |
| `ALIGNMENT` | `min`*, `max`* |
| `TIME` | `mode` (`GAME_TIME` + `ticks`, or `REAL_TIME` + `milliseconds`) — elapsed time since the quest became eligible. |

Source: `QuestParser.parseCondition`, `QuestPrerequisites.java`.

## Sidequests

Sidequests live in `dragonminez/sidequests/<category>/` and use the **same quest schema** as saga quests, with a few practical differences:

- `"id"` is a **string** instead of a number (e.g. `"bulma_gero_blueprints"`).
- `"type"` is `"SIDEQUEST"` instead of `"SAGA"`.
- No enclosing "saga folder" or manifest — each file stands alone.
- `quest_giver`/`turn_in` are used more heavily (sidequests are usually NPC-driven).
- `prerequisites` commonly reference `SAGA_QUEST`/`QUEST` conditions to gate a sidequest behind story progress.

Example (`dragonminez/sidequests/collection/bulma_gero_blueprints.json`):

```json
{
  "id": "bulma_gero_blueprints",
  "title": "dmz.sidequest.bulma_gero.name",
  "description": "dmz.sidequest.bulma_gero.desc",
  "type": "SIDEQUEST",
  "category": "collection",
  "party_scaling": true,
  "claim_mode": "TREE_OR_NPC",
  "quest_giver": "bulma",
  "turn_in": "bulma",
  "prerequisites": {
    "operator": "AND",
    "conditions": [
      { "type": "SAGA_QUEST", "sagaId": "android_saga", "questId": 6 }
    ]
  },
  "objectives": [
    { "type": "STRUCTURE", "structure": "dragonminez:gero_lab" },
    { "type": "ITEM", "item": "minecraft:redstone", "count": 32 },
    { "type": "TALK_TO", "npcId": "bulma" }
  ],
  "rewards": [
    { "type": "TPS", "amount": 56000 }
  ]
}
```

## Quest giver / turn-in NPCs

`quest_giver` and `turn_in` reference an NPC by id — the same id used by quest NPC entities and master/trainer NPCs. DMZ indexes quests by both fields so an NPC can show "available quest" markers and accept turn-ins without extra wiring on your end. Both fields are optional; omit or set `null` if the quest isn't tied to an NPC (e.g. a purely exploration/kill-based saga quest).

## Translation keys

Follow the existing naming convention so your content matches the rest of the story:

- Saga name: `dmz.saga.<saga_id>`
- Saga quest title/description: `dmz.quest.<key>.name` / `dmz.quest.<key>.desc`
- Sidequest title/description: `dmz.sidequest.<key>.name` / `dmz.sidequest.<key>.desc`

Add these keys to your `en_us.json` (or your own language file) — see [[Custom Races|Custom-Races]] for how DMZ merges custom translations with Crowdin ones.

## Reload and validate

1. Save your saga/quest/sidequest JSON in the world's `dragonminez/` folder.
2. Validate the JSON syntax (see [[How to JSON|How-to-JSON]]).
3. Run `/dmzreload story` (or `/dmzreload all`) — see [[Reloading and updating changes|Reloading-and-updating-changes]].
4. Check the quest log in-game and confirm your saga/quest/sidequest appears with the right requirements, objectives, and rewards.
