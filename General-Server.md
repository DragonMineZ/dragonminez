# general-server.json

`general-server.json` controls server-wide and world-wide behavior for DragonMineZ. This file matters for dedicated servers, LAN worlds, and singleplayer saves where you want tighter control over progression, world generation, racial systems, and data storage.

This page explains every setting in the file, what each value does, and safe ranges to use.

## Note about comments

The file the mod generates automatically includes `//` comments. These aren't valid JSON — feel free to delete them if you're editing by hand. The examples on this page have them removed.

## File overview

Top-level sections:

| Section | Purpose |
| :-- | :-- |
| `configVersion` | Internal version used for config migration and default updates. Do not hand-edit. |
| `worldGen` | Structures, Dragon Ball generation, Otherworld toggle, and structure spacing. |
| `gameplay` | Progression, TP gain/cost, story/side-quest toggles, food restoration, fusion, party, difficulty multipliers, capsules. |
| `mutant` | The random "legendary form holder" lottery mechanic. |
| `racialSkills` | Enables and tunes each race's passive/active racial skill. |
| `storage` | Player data storage backend (NBT/JSON/database). |

Two subsystems live in this same file but are large enough to have their own dedicated pages: [[Gravity System|Gravity-System]] (`gravity` key) and [[Dynamic Growth|Dynamic-Growth]] (`dynamicGrowth` key).

**Combat is not part of this file.** Stamina, blocking/parrying, mitigation, combat flight, and ki weapon tuning live in their own file — see [[Combat|Combat]] (`config/dragonminez/combat.json`).

## configVersion

| Key | Type | Change this? |
| :-- | :-- | :-- |
| `configVersion` | Number | No. DMZ uses this to detect when defaults/migrations are needed — see [[How to JSON|How-to-JSON]] for what happens if it's stale. |

## worldGen

| Key | Type | Default | What it does |
| :-- | :-- | :-- | :-- |
| `generateCustomStructures` | Boolean | `true` | Enables DMZ structures in world generation. |
| `generateDragonBalls` | Boolean | `true` | Enables Dragon Ball generation/re-scattering after a wish. |
| `otherworldActive` | Boolean | `true` | Keeps Otherworld gameplay systems active. If `false`, Otherworld doesn't exist and vanilla respawn is used instead. |
| `dbSpawnRange` | Integer | `1000` | Dragon Ball spawn radius from world origin `(0,0)`. |
| `dragonBallSets` | Integer | `1` | Number of Dragon Ball sets generated, `0`-`10`. |
| `structureMinDistanceFromSpawn` | Integer | `0` | Minimum distance DMZ structures spawn from world origin. |
| `structureMaxDistanceFromSpawn` | Integer | `4000` | Maximum distance DMZ structures spawn from world origin (enforced minimum of `3000` internally). |
| `structureMinDistanceBetween` | Integer | `250` | Minimum distance kept between individual DMZ structures. |

## gameplay

The largest section — progression, TP, story, food, fusion, party, and difficulty tuning.

### Core progression

| Key | Type | Default | What it does |
| :-- | :-- | :-- | :-- |
| `forceCharacterCreation` | Boolean | `true` | Forces new players through character creation on first login. |
| `commandOutputOnConsole` | Boolean | `true` | Sends command output to the server console (players get their output regardless). |
| `reviveCooldownSeconds` | Integer | `300` | Cooldown before a player can revive. |
| `tpGainMultiplier` | Number | `1.0` | Global multiplier applied to TP from every source. |
| `globalTPCostMultiplier` | Number | `1.0` | Global multiplier on stat TP costs. |
| `minTPCost` | Integer | `16` | Base minimum TP cost in the stat-cost formula. |
| `maxTPDiscount` | Integer | `140` | Early-game TP discount threshold — fades out as total stats rise. |
| `tpHealthRatio` | Number | `0.25` | TP earned from a kill, scaled by the target's max health. |
| `tpPerHit` | Integer | `2` | Flat TP per successful hit. |
| `passiveTpGain` | Integer | `1` | Flat TP gained passively over time. |
| `tpPer20BlocksTraveled` | Integer | `1` | TP gained per 20 blocks traveled. |
| `tpPerBlockMined` | Integer | `1` | TP gained per block mined. |
| `tpPerItemCrafted` | Integer | `1` | TP gained per item crafted. |
| `gravityBonusEnabled` | Boolean | `true` | Enables the [[Gravity System|Gravity-System]] TP bonus. |
| `HTCTpMultiplier` | Number | `2.5` | TP multiplier inside the Hyperbolic Time Chamber. |
| `maxLevelValueInsteadOfStats` | Boolean | `true` | If `true`, `maxValue` caps player **level** instead of raw stat totals. |
| `maxValue` | Integer | `10000` | The cap itself — read together with `maxLevelValueInsteadOfStats`. (Older DMZ versions called this `maxStatValue` — that key no longer exists.) |

### TP cost formula

```text
TotalStats = STR + SKP + RES + VIT + PWR + ENE
Multiplier = globalTPCostMultiplier * raceTpCostMultiplier
Discount = maxTPDiscount - TotalStats   (only if TotalStats < maxTPDiscount)
BaseCost = minTPCost + (TotalStats * 1.5)

FinalCost = (BaseCost * Multiplier) - Discount
```

- `minTPCost` sets the floor for early upgrades.
- `globalTPCostMultiplier` scales every race/class at once.
- `maxTPDiscount` speeds up the early game and fades as total stats climb.

### Story & side quests

| Key | Type | Default | What it does |
| :-- | :-- | :-- | :-- |
| `storyModeEnabled` | Boolean | `true` | Master toggle for sagas/quests. See [[Custom Quests, Sagas & Sidequests|Custom-Quests-Sagas-and-Sidequests]]. |
| `createDefaultSagas` | Boolean | `true` | Auto-generates DMZ's default sagas if missing. |
| `sideQuestsEnabled` | Boolean | `true` | Master toggle for side quests. |
| `createDefaultSideQuests` | Boolean | `true` | Auto-generates DMZ's default side quests if missing. |
| `defaultQuestPartyMultiplier` | Number | `1.45` | Party-size difficulty multiplier for quest objectives (clamped `1.0`-`5.0`). |

### Senzu

| Key | Type | Default | What it does |
| :-- | :-- | :-- | :-- |
| `senzuCooldownTicks` | Integer | `240` | Cooldown between Senzu Bean uses. |
| `senzuGiftCooldownTicks` | Integer | `18000` | Cooldown for the periodic Senzu "gift". |
| `senzuGiftAmount` | Integer | `5` | How many Senzu Beans are gifted per cycle. |

### Food restoration (`food`)

Reworked from the old flat per-item map into a hunger/saturation-point formula:

```json
"food": {
  "minHungerPoints": 2,
  "maxHungerPoints": 20,
  "minSaturationPoints": 0.4,
  "maxSaturationPoints": 2.0,
  "healthPercentageRecoveredPerHungerPoint": 0.01,
  "kiPercentageRecoveredPerHungerPoint": 0.01,
  "staminaPercentageRecoveredPerHungerPoint": 0.02,
  "healthPercentageRecoveredPerSaturationPoint": 0.001,
  "kiPercentageRecoveredPerSaturationPoint": 0.001,
  "staminaPercentageRecoveredPerSaturationPoint": 0.004,
  "blacklistedNamespaces": [],
  "blacklistedItems": []
}
```

| Field | Type | Default | Notes |
| :-- | :-- | :-- | :-- |
| `minHungerPoints` / `maxHungerPoints` | Integer | `2` / `20` | Hunger-point range eaten food is evaluated against. |
| `minSaturationPoints` / `maxSaturationPoints` | Number | `0.4` / `2.0` | Saturation range eaten food is evaluated against. |
| `healthPercentageRecoveredPerHungerPoint` | Number | `0.01` | Health % restored per hunger point restored by the food. |
| `kiPercentageRecoveredPerHungerPoint` | Number | `0.01` | Ki % restored per hunger point. |
| `staminaPercentageRecoveredPerHungerPoint` | Number | `0.02` | Stamina % restored per hunger point. |
| `healthPercentageRecoveredPerSaturationPoint` | Number | `0.001` | Health % restored per saturation point. |
| `kiPercentageRecoveredPerSaturationPoint` | Number | `0.001` | Ki % restored per saturation point. |
| `staminaPercentageRecoveredPerSaturationPoint` | Number | `0.004` | Stamina % restored per saturation point. |
| `blacklistedNamespaces` | List of strings | `[]` | Whole namespaces excluded from this recovery formula entirely. |
| `blacklistedItems` | List of strings | `[]` | Specific item ids excluded. |

In effect, any food item's normal hunger/saturation restoration now also restores a proportional slice of health/ki/stamina, rather than each item needing an explicit entry in a map.

### Power multipliers & fusion

| Key | Type | Default | What it does |
| :-- | :-- | :-- | :-- |
| `mightFruitPower` | Number | `1.2` | Might Fruit effect multiplier. |
| `majinPower` | Number | `1.3` | Majin power-effect multiplier. |
| `metamoruFusionThreshold` | Number | `0.5` | Stat threshold used by Metamoru fusion eligibility. |
| `fusionBoosts` | String list | `["STR", "SKP", "PWR"]` | Stats boosted by fusion. |
| `fusionDurationSeconds` | Integer | `900` | Fusion duration. |
| `fusionCooldownSeconds` | Integer | `1800` | Cooldown before fusing again. |
| `multiplicationInsteadOfAdditionForMultipliers` | Boolean | `false` | `false` = multipliers stack additively (stronger early/mid-game, easier to balance). `true` = multiplicative (weaker at low values, synergy-heavy at high progression). |
| `ultimateFormFixedValue` | Boolean | `false` | If `true`, the Ultimate stack form uses a fixed damage value instead of scaling normally. |

### Capsules (`capsules`)

Stat-selection capsule items let a player pick which stat(s) a capsule boosts.

```json
"capsules": {
  "statSeparator": ", ",
  "values": {
    "<CAPSULE_TYPE>": { "stats": "STR, PWR", "points": 5 }
  }
}
```

| Field | Type | Notes |
| :-- | :-- | :-- |
| `statSeparator` | String | Separator used when a capsule lists multiple stats. |
| `values` | Map (`CapsuleType` -> `{ stats, points }`) | Per capsule type: which stat(s) it boosts, and how many points it grants (default `5`). |

### Party

| Key | Type | Default | What it does |
| :-- | :-- | :-- | :-- |
| `partyMaxMembers` | Integer | `-1` | Max party size (`-1` = unlimited). |
| `partyMaxLevelGap` | Integer | `500` | Max level difference allowed between party members (`-1` = unlimited). |
| `partyTpShareRatio` | Number | `0.5` | Fraction of TP shared with non-active party members. |
| `enemyHealthPerPartyPlayer` | Number | `1.25` | Enemy HP multiplier per additional party member. |
| `enemyDamagePerPartyPlayer` | Number | `1.1` | Enemy damage multiplier per additional party member. |

### Teleportation

| Key | Type | Default | What it does |
| :-- | :-- | :-- | :-- |
| `instantTransmissionPlayerRangePerLevel` | Integer | `200` | Instant Transmission range (blocks) granted per player level. |

### Difficulty multipliers

| Key | Type | Default | What it does |
| :-- | :-- | :-- | :-- |
| `easyModeHPMultiplier` | Number | `0.75` | Enemy HP multiplier on Easy. |
| `easyModeDamageMultiplier` | Number | `0.5` | Enemy damage multiplier on Easy. |
| `easyModeTPMultiplier` | Number | `1.25` | TP gain multiplier on Easy. |
| `easyModeQuestRewardMultiplier` | Number | `1.0` | Quest reward multiplier on Easy. |
| `hardModeHPMultiplier` | Number | `2.0` | Enemy HP multiplier on Hard. |
| `hardModeDamageMultiplier` | Number | `1.5` | Enemy damage multiplier on Hard. |
| `hardModeTPMultiplier` | Number | `1.25` | TP gain multiplier on Hard. |
| `hardModeQuestRewardMultiplier` | Number | `1.25` | Quest reward multiplier on Hard. |

These correspond to the difficulty the player picks at character creation (client-side toggle documented in [[General User|general-user]]).

### Cosmetics

| Key | Type | Default | What it does |
| :-- | :-- | :-- | :-- |
| `helmetsThatKeepHair` | List of item ids | `["dragonminez:invencible_armor_helmet", "dragonminez:invencible_blue_armor_helmet"]` | Helmets that don't hide the player's hair when worn. |

### TP gain boosts (`tpGainBoosts`)

Controls which multiplier *sources* apply to each TP-gain *source*. Keys are `TpSource` values (`STORY`, `PASSIVE`, `TRAVEL`, `MINED`, `CRAFTED`, `KILL`, `HIT`); values are lists of `TpBoost` values (`CLASS`, `RACIALSKILL`, `HTC`, `GRAVITY`, `WEIGHTS`, `GLOBAL`, `POTION`, `MUTANT`, `DIFFICULTY`).

```json
"tpGainBoosts": {
  "STORY":   ["CLASS", "RACIALSKILL", "HTC", "GRAVITY", "WEIGHTS", "GLOBAL", "POTION", "MUTANT", "DIFFICULTY"],
  "PASSIVE": ["CLASS", "RACIALSKILL", "HTC", "GRAVITY", "WEIGHTS", "GLOBAL", "POTION", "MUTANT"],
  "TRAVEL":  ["CLASS", "RACIALSKILL", "HTC", "GRAVITY", "WEIGHTS", "GLOBAL", "POTION", "MUTANT"],
  "MINED":   ["CLASS", "RACIALSKILL", "HTC", "GRAVITY", "WEIGHTS", "GLOBAL", "POTION", "MUTANT"],
  "CRAFTED": ["CLASS", "RACIALSKILL", "HTC", "GRAVITY", "WEIGHTS", "GLOBAL", "POTION", "MUTANT"],
  "KILL":    ["CLASS", "RACIALSKILL", "HTC", "GRAVITY", "WEIGHTS", "GLOBAL", "POTION", "MUTANT"],
  "HIT":     ["CLASS", "RACIALSKILL", "HTC", "GRAVITY", "WEIGHTS", "GLOBAL", "POTION", "MUTANT"]
}
```

By default, only `STORY`-sourced TP (quest rewards) is affected by the difficulty multiplier (`DIFFICULTY`) — every other source ignores it. Remove a boost from a source's list to stop that multiplier from applying to it (e.g. remove `GRAVITY` from `MINED` if you don't want mining TP boosted by gravity training).

## mutant

The `mutant` block controls a periodic server-wide lottery: every interval, a handful of players are rolled for a small chance to become a "Mutant" — gaining a boosted legendary form and TP/mastery multipliers.

| Key | Type | Default | What it does |
| :-- | :-- | :-- | :-- |
| `enabled` | Boolean | `true` | Master toggle. |
| `rollIntervalMinutes` | Integer | `30` | Minutes between lottery rolls. |
| `playersPerRoll` | Integer | `1` | Players considered per roll. |
| `chance` | Number | `0.20` | Chance (0-1) a considered player becomes Mutant. |
| `maxHolders` | Integer | `1` | Max concurrent Mutant holders server-wide. |
| `legendaryGroupName` | String | `"legendaryforms"` | The [[Custom Forms|Custom-Forms]] group id Mutant holders can access. |
| `tpGainMultiplier` | Number | `1.25` | TP multiplier while holding Mutant status. |
| `masteryGainMultiplier` | Number | `1.50` | Form mastery gain multiplier while Mutant. |
| `powerBonusReductionNoSkill` | Number | `0.67` | Without the `legendaryforms` skill, a Mutant's legendary-form stat bonus is reduced to roughly a third of normal (`1.0 + (mult-1.0) * (1 - 0.67)`). |
| `powerBonusBoostWithSkill` | Number | `0.33` | With the skill, the bonus is boosted instead (`1.0 + (mult-1.0) * (1 + 0.33)`). |
| `keepMutantOnDeath` | Boolean | `false` | If `false`, dying revokes Mutant status. |

## racialSkills

Enables and tunes race-specific passive/active mechanics. One of the most important balance sections for multiplayer.

| Key | Type | Default | What it does |
| :-- | :-- | :-- | :-- |
| `enableRacialSkills` | Boolean | `true` | Global on/off switch. |

### Human

| Key | Type | Default |
| :-- | :-- | :-- |
| `humanRacialSkill` | Boolean | `true` |
| `humanKiRegenBoost` | Number | `1.40` (40% more ki regen) |

### Saiyan

| Key | Type | Default |
| :-- | :-- | :-- |
| `saiyanRacialSkill` | Boolean | `true` |
| `saiyanZenkaiAmount` | Integer | `3` |
| `saiyanZenkaiHealthRegen` | Number | `0.20` |
| `saiyanZenkaiStatBoost` | Number | `0.10` |
| `saiyanZenkaiBoosts` | String list | `["STR", "SKP", "PWR"]` |
| `saiyanZenkaiCooldownSeconds` | Integer | `900` |

### Namekian

| Key | Type | Default |
| :-- | :-- | :-- |
| `namekianRacialSkill` | Boolean | `true` |
| `namekianAssimilationAmount` | Integer | `4` |
| `namekianAssimilationHealthRegen` | Number | `0.35` |
| `namekianAssimilationStatBoost` | Number | `0.15` |
| `namekianAssimilationBoosts` | String list | `["STR", "SKP", "PWR"]` |
| `namekianAssimilationOnNamekNpcs` | Boolean | `true` |

### Frost Demon

| Key | Type | Default |
| :-- | :-- | :-- |
| `frostDemonRacialSkill` | Boolean | `true` |
| `frostDemonTPBoost` | Number | `1.25` (stacks additively with other TP multipliers) |

### Bio-Android

| Key | Type | Default |
| :-- | :-- | :-- |
| `bioAndroidRacialSkill` | Boolean | `true` |
| `bioAndroidCooldownSeconds` | Integer | `180` |
| `bioAndroidDrainRatio` | Number | `0.25` (damage dealt as fraction of target HP; user heals the same amount) |

### Majin

| Key | Type | Default |
| :-- | :-- | :-- |
| `majinAbsoprtionSkill` | Boolean | `true` (note: the key is spelled `Absoprtion`, keep that exact spelling) |
| `majinReviveSkill` | Boolean | `true` |
| `majinAbsorptionAmount` | Integer | `3` |
| `majinAbsorptionHealthRegen` | Number | `0.30` |
| `majinAbsorptionStatsCopy` | Number | `0.04` |
| `majinAbsorptionBoosts` | String list | `["STR", "SKP", "PWR"]` |
| `majinAbsorptionOnMobs` | Boolean | `true` |
| `majinReviveCooldownSeconds` | Integer | `3600` |
| `majinReviveHealthRatioPerBlop` | Number | `0.25` |

## storage

Chooses where player data is saved. Case matters for `storageType`.

| Key | Type | Default | What it does |
| :-- | :-- | :-- | :-- |
| `storageType` | String | `"NBT"` | `NBT`, `JSON`, or `DATABASE`. |
| `host` | String | `"localhost"` | Database host (DATABASE only). |
| `port` | Integer | `3306` | Database port (DATABASE only). |
| `database` | String | `"dragonminez"` | Database name. |
| `table` | String | `"player_data"` | Table name. |
| `username` | String | `"root"` | Database user. |
| `password` | String | `"password"` | Database password — change this in production. |
| `poolSize` | Integer | `10` | Connection pool size. |
| `threadPoolSize` | Integer | `4` | Thread pool size for storage tasks. |

### Operational notes

- If `DATABASE` credentials are missing or the connection fails, DMZ falls back rather than hard-crashing — check server logs if player data doesn't seem to be persisting as expected.
- `NBT` mode bypasses DMZ's custom storage path entirely — player data rides on vanilla's own save format.
- Don't switch `storageType` on a server with players currently online; do it with the server empty, and take a backup first. See [[Notice to Server Owners/Developers|Notice-to-server-owners-developers]].

## Reload and validate

1. Edit `config/dragonminez/general-server.json`.
2. Validate the JSON syntax (see [[How to JSON|How-to-JSON]]).
3. Run `/dmzreload config` (or `/dmzreload`) — see [[Reloading and updating changes|Reloading-and-updating-changes]].
4. Check server logs for a `Regenerating general-server.json` warning — that means something didn't parse and DMZ fell back to defaults (see [[How to JSON|How-to-JSON]]).
