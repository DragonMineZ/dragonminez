# general-server.json

`general-server.json` controls server-wide and world-wide behavior for DragonMineZ. This file matters for dedicated servers, LAN worlds, and singleplayer saves where you want tighter control over progression, combat, world generation, racial systems, and data storage.

This page explains every setting in the file, what each value does, safe ranges to use, and what kinds of admins or players should change each option.

## Note about comments

As you may have seen, the original file made by the mod automatically adds `//` comments. Well, in JSON, these comments should not exist. Feel free to delete those if you are comfortable. For practical uses, the comments here have been deleted.

## File overview

Top-level sections:

- `configVersion`
- `worldGen`
- `gameplay`
- `combat`
- `racialSkills`
- `storage`

These sections cover different parts of the mod:

| Section | Purpose |
| --- | --- |
| `configVersion` | Internal version used for config migration and default updates. DO NOT CHANGE PLS.|
| `worldGen` | Controls structures, Dragon Ball generation, Otherworld behavior, and generation counts. |
| `gameplay` | Controls progression, TP gain, stat costs, story mode, food restoration, fusion, and item cooldowns. |
| `combat` | Controls stamina, attack pacing, blocking, parrying, dashing, combo attacks, and ki weapon tuning. |
| `racialSkills` | Enables racial passives and active skills, then configures each race-specific mechanic. |
| `storage` | Chooses how player data is stored, either locally or in a database backend. |

## Full example

```json
{
  "configVersion": 3,
  "worldGen": {
    "generateCustomStructures": true,
    "generateDragonBalls": true,
    "otherworldActive": true,
    "dbSpawnRange": 1000,
    "dragonBallSets": 1
  },
  "gameplay": {
    "commandOutputOnConsole": true,
    "reviveCooldownSeconds": 300,
    "tpGainMultiplier": 1.0,
    "globalTPCostMultiplier": 1.0,
    "minTPCost": 16,
    "maxTPDiscount": 140,
    "tpHealthRatio": 0.1,
    "tpPerHit": 2,
    "HTCTpMultiplier": 2.5,
    "maxStatValue": 10000,
    "storyModeEnabled": true,
    "createDefaultSagas": true,
    "senzuCooldownTicks": 240,
    "foodRegenerations": {
      "dragonminez:frog_legs_cooked": [0.10, 0.10, 0.10],
      "dragonminez:might_tree_fruit": [0.35, 0.35, 0.35],
      "dragonminez:raw_dino_meat": [0.10, 0.10, 0.10],
      "dragonminez:dino_tail_raw": [0.15, 0.15, 0.15],
      "dragonminez:frog_legs_raw": [0.05, 0.05, 0.05],
      "dragonminez:heart_medicine": [1.0, 1.0, 1.0],
      "dragonminez:cooked_dino_meat": [0.15, 0.15, 0.15],
      "dragonminez:dino_tail_cooked": [0.20, 0.20, 0.20],
      "dragonminez:senzu_bean": [1.0, 1.0, 1.0]
    },
    "mightFruitPower": 1.2,
    "majinPower": 1.3,
    "metamoruFusionThreshold": 0.5,
    "fusionBoosts": ["STR", "SKP", "PWR"],
    "fusionDurationSeconds": 900,
    "fusionCooldownSeconds": 1800,
    "multiplicationInsteadOfAdditionForMultipliers": false
  },
  "combat": {
    "killPlayersOnCombatLogout": true,
    "staminaConsumptionRatio": 0.125,
    "baselineFormDrain": 200,
    "respectAttackCooldown": true,
    "enableBlocking": true,
    "enableParrying": true,
    "effectiveDefenseOnGuardBreak": 0.33,
    "enableComboAttacks": true,
    "comboAttacksCooldownSeconds": 8,
    "enablePerfectEvasion": true,
    "parryWindowMs": 150,
    "blockDamageReductionCap": 0.8,
    "blockDamageReductionMin": 0.4,
    "poiseDamageMultiplier": 0.25,
    "poiseRegenCooldown": 100,
    "blockBreakStunDurationTicks": 60,
    "perfectEvasionWindowMs": 150,
    "dashCooldownSeconds": 4,
    "doubleDashCooldownSeconds": 12,
    "kiBladeConfig": [1.0, 0.05],
    "kiScytheConfig": [1.5, 0.075],
    "kiClawLanceConfig": [2.0, 0.125]
  },
  "racialSkills": {
    "enableRacialSkills": true,
    "humanRacialSkill": true,
    "humanKiRegenBoost": 1.4,
    "saiyanRacialSkill": true,
    "saiyanZenkaiAmount": 3,
    "saiyanZenkaiHealthRegen": 0.2,
    "saiyanZenkaiStatBoost": 0.1,
    "saiyanZenkaiBoosts": ["STR", "SKP", "PWR"],
    "saiyanZenkaiCooldownSeconds": 900,
    "namekianRacialSkill": true,
    "namekianAssimilationAmount": 4,
    "namekianAssimilationHealthRegen": 0.35,
    "namekianAssimilationStatBoost": 0.15,
    "namekianAssimilationBoosts": ["STR", "SKP", "PWR"],
    "namekianAssimilationOnNamekNpcs": true,
    "frostDemonRacialSkill": true,
    "frostDemonTPBoost": 1.25,
    "bioAndroidRacialSkill": true,
    "bioAndroidCooldownSeconds": 180,
    "bioAndroidDrainRatio": 0.25,
    "majinAbsoprtionSkill": true,
    "majinReviveSkill": true,
    "majinAbsorptionAmount": 3,
    "majinAbsorptionHealthRegen": 0.3,
    "majinAbsorptionStatsCopy": 0.1,
    "majinAbsorptionBoosts": ["STR", "SKP", "PWR"],
    "majinAbsorptionOnMobs": true,
    "majinReviveCooldownSeconds": 3600,
    "majinReviveHealthRatioPerBlop": 0.25
  },
  "storage": {
    "storageType": "NBT",
    "host": "localhost",
    "port": 3306,
    "database": "dragonminez",
    "table": "player_data",
    "username": "root",
    "password": "password",
    "poolSize": 10,
    "threadPoolSize": 4
  }
}
```

## configVersion

### `configVersion`

| Key | Type | Default | Description | Change this? |
| --- | --- | --- | --- | --- |
| `configVersion` | Integer | `3` | Internal config schema version. Used by the mod to detect when new defaults or migrations are needed. | No. Leave this alone. |

### Notes

- Do not edit `configVersion` manually.
- If the mod updates and expects a newer version, the loader or config updater will usually handle migration.
- Changing this by hand risks missing new keys or breaking upgrade logic.

## worldGen

The `worldGen` section controls what content gets placed into the world and whether special progression systems tied to world generation stay active.

### Settings

| Key | Type | Default | What it does | Recommended use |
| --- | --- | --- | --- | --- |
| `generateCustomStructures` | Boolean | `true` | Enables DragonMineZ structures in world generation. | Keep `true` for normal play. Disable for challenge maps or custom world packs. |
| `generateDragonBalls` | Boolean | `true` | Enables Dragon Ball generation in the world. Also disables Dragon Ball re-scattering after Shenron or Porunga if set to `false`. | Disable only if your server handles wishes another way or you don't want wishes/dragon-balls at all. |
| `otherworldActive` | Boolean | `true` | Keeps Otherworld gameplay systems active. If `false` then the otherworld won't exist at all. Normal revive-system for Minecraft will be used instead of DMZ's. | Good for servers that want to remove afterlife from progression. |
| `dbSpawnRange` | Integer | `1000` | Dragon Ball spawn radius from world origin `(0,0)`. | Lower for smaller maps, raise for exploration-heavy servers. The limit is the max value of INT. (Why would you do it?) |
| `dragonBallSets` | Integer | `1` | Number of Dragon Ball sets generated in the world, from `0` to `10`. | This is pretty self-explanatory. |

## gameplay

The `gameplay` section is the main progression and balance block. It controls TP gain, stat growth cost, story content, cooldowns, fusion, and restorative items.

### Core settings

| Key | Type | Default | What it does |
| --- | --- | --- | --- |
| `commandOutputOnConsole` | Boolean | `true` | Sends command output to server console. Players still receive output regardless of this setting. |
| `reviveCooldownSeconds` | Integer | `300` | Time a player must wait before reviving in seconds. (300seconds = 5min) |
| `tpGainMultiplier` | Number | `1.0` | Global multiplier for TP gain from all sources. |
| `globalTPCostMultiplier` | Number | `1.0` | Global multiplier for stat TP costs across races and classes. |
| `minTPCost` | Integer | `16` | Base minimum TP cost used in stat cost calculation. |
| `maxTPDiscount` | Integer | `140` | Threshold for the early-game TP discount system. |
| `tpHealthRatio` | Number | `0.1` | TP gained from killing an entity, based on target max health. |
| `tpPerHit` | Integer | `2` | Flat TP gained per successful hit. |
| `HTCTpMultiplier` | Number | `2.5` | TP multiplier applied in the Hyperbolic Time Chamber. |
| `maxStatValue` | Integer | `10000` | Hard cap for stat values. |
| `storyModeEnabled` | Boolean | `true` | Turns Story Mode systems on or off. |
| `createDefaultSagas` | Boolean | `true` | Generates the default sagas used by Story Mode. |
| `senzuCooldownTicks` | Integer | `240` | Cooldown for Senzu use, in game ticks. |

### TP cost formula

- `TotalStats = STR + SKP + RES + VIT + PWR + ENE`
- `Multiplier = globalTPCostMultiplier * raceTpCostMultiplier`
- `Discount = maxTPDiscount - TotalStats`, but only if `TotalStats < maxTPDiscount`
- `BaseCost = minTPCost + (TotalStats * 1.5)`

Final cost:

```text
FinalCost = (BaseCost * Multiplier) - Discount
```

### What the formula means

- `minTPCost` sets the floor for early stat upgrades.
- `globalTPCostMultiplier` scales all races at once.
- `maxTPDiscount` helps the early game move faster, then fades out as total stats increase.
- Higher total stats always push upgrade cost upward.

### Story settings

| Key | Type | Default | What it does | Notes |
| --- | --- | --- | --- | --- |
| `storyModeEnabled` | Boolean | `true` | Enables story systems. | If `false` then there will not be any story at all. :( |
| `createDefaultSagas` | Boolean | `true` | Creates built-in sagas. | Usually keep aligned with `storyModeEnabled`. |

### Item and food restoration

`foodRegenerations` maps an item registry name to a 3-value array:

```text
[item] = [healthRestored, kiRestored, staminaRestored]
```

Each value appears to be a ratio or percentage-like amount, where `1.0` means a full refill effect for that resource.

### `foodRegenerations` entries

| Item | Health | Ki | Stamina | Notes |
| --- | --- | --- | --- | --- |
| `dragonminez:frog_legs_cooked` | `0.10` | `0.10` | `0.10` | Low general recovery food. |
| `dragonminez:might_tree_fruit` | `0.35` | `0.35` | `0.35` | Stronger all-purpose recovery item. |
| `dragonminez:raw_dino_meat` | `0.10` | `0.10` | `0.10` | Basic raw recovery item. |
| `dragonminez:dino_tail_raw` | `0.15` | `0.15` | `0.15` | Better than basic raw meat. |
| `dragonminez:frog_legs_raw` | `0.05` | `0.05` | `0.05` | Weak recovery item. |
| `dragonminez:heart_medicine` | `1.0` | `1.0` | `1.0` | Full restore item. |
| `dragonminez:cooked_dino_meat` | `0.15` | `0.15` | `0.15` | Mid-tier cooked recovery. |
| `dragonminez:dino_tail_cooked` | `0.20` | `0.20` | `0.20` | Strong cooked recovery item. |
| `dragonminez:senzu_bean` | `1.0` | `1.0` | `1.0` | Full restore item. |

### Adding custom food

You can add vanilla or modded items using registry names.

Example:

```json
"foodRegenerations": {
  "minecraft:cooked_beef": [0.1, 0.0, 0.0],
  "mymod:energy_bar": [0.0, 0.25, 0.15]
}
```

### Special power multipliers

| Key | Type | Default | What it does |
| --- | --- | --- | --- |
| `mightFruitPower` | Number | `1.2` | Multiplier applied to the Might Fruit effect. |
| `majinPower` | Number | `1.3` | Multiplier tied to Majin power effects. |

### Fusion settings

| Key | Type | Default | What it does |
| --- | --- | --- | --- |
| `metamoruFusionThreshold` | Number | `0.5` | Threshold used by Metamoru fusion checks. |
| `fusionBoosts` | String Array | `["STR", "SKP", "PWR"]` | Stats affected by fusion bonuses. |
| `fusionDurationSeconds` | Integer | `900` | Fusion duration in seconds. |
| `fusionCooldownSeconds` | Integer | `1800` | Cooldown before fusion can be used again (yes, in real-time). |
| `multiplicationInsteadOfAdditionForMultipliers` | Boolean | `false` | Switches stat multipliers from additive to multiplicative behavior. |

### Additive vs multiplicative multipliers

If `multiplicationInsteadOfAdditionForMultipliers` is `false`:

- Multipliers stack additively.
- Stronger in early and mid game.
- Easier to balance for casual servers.

If `multiplicationInsteadOfAdditionForMultipliers` is `true`:

- Multipliers stack multiplicatively.
- Weaker at low values.
- Stronger when several multipliers combine at high progression levels.
- Better for servers that want synergy-heavy endgame scaling.

## combat

The `combat` section defines pacing, defensive mechanics, stamina efficiency, poise behavior, mobility cooldowns, and ki weapon tuning.

### Core combat settings

| Key | Type | Default | What it does |
| --- | --- | --- | --- |
| `killPlayersOnCombatLogout` | Boolean | `true` | If a player logs out during PvP combat, they die on next login. |
| `staminaConsumptionRatio` | Number | `0.125` | Stamina cost relative to damage dealt. |
| `baselineFormDrain` | Integer | `200` | Base value used to calculate form energy drain. |
| `respectAttackCooldown` | Boolean | `true` | Uses vanilla-style attack cooldown timing. |
| `enableBlocking` | Boolean | `true` | Enables block mechanics. |
| `enableParrying` | Boolean | `true` | Enables parry mechanics. Blocking must also be enabled. |
| `effectiveDefenseOnGuardBreak` | Number | `0.33` | Fraction of defense still applied after guard break. |
| `enableComboAttacks` | Boolean | `true` | Enables combo attack systems. |
| `comboAttacksCooldownSeconds` | Integer | `8` | Cooldown between combo attack uses. |
| `enablePerfectEvasion` | Boolean | `true` | Enables perfect evasion timing mechanic. |
| `parryWindowMs` | Integer | `150` | Parry timing window in milliseconds. |
| `blockDamageReductionCap` | Number | `0.8` | Maximum damage reduction while blocking. |
| `blockDamageReductionMin` | Number | `0.4` | Minimum damage reduction while blocking. |
| `poiseDamageMultiplier` | Number | `0.25` | Converts incoming damage into poise damage while blocking. |
| `poiseRegenCooldown` | Integer | `100` | Delay before poise starts regenerating again. |
| `blockBreakStunDurationTicks` | Integer | `60` | Stun time after guard or block break. |
| `perfectEvasionWindowMs` | Integer | `150` | Perfect evasion timing window in milliseconds. |
| `dashCooldownSeconds` | Integer | `4` | Cooldown for dash. |
| `doubleDashCooldownSeconds` | Integer | `12` | Cooldown for double dash. |

### Form drain formula

The config comments define drain like this:

```text
finalDrain = formDrain * baselineFormDrain
```

Example:

- If a form has `energyDrain = 0.15`
- And `baselineFormDrain = 200`
- Then `finalDrain = 30` energy per second

### Defensive mechanics notes

- `enableParrying` needs `enableBlocking` to work.
- `blockDamageReductionMin` and `blockDamageReductionCap` define the range of block effectiveness.
- `effectiveDefenseOnGuardBreak` prevents guard breaks from becoming full-defense removal.
- `poiseDamageMultiplier` affects how fast defensive play collapses under pressure.

### Ki weapon configs

Each ki weapon uses a 2-value array:

```text
[damageMultiplier, kiCostMultiplier]
```

### Ki weapon values

| Key | Damage Multiplier | Ki Cost Multiplier | Meaning |
| --- | --- | --- | --- |
| `kiBladeConfig` | `1.0` | `0.05` | Baseline ki weapon. |
| `kiScytheConfig` | `1.5` | `0.075` | Higher damage and higher ki cost. |
| `kiClawLanceConfig` | `2.0` | `0.125` | Highest damage and highest ki cost of the three. |

## racialSkills

The `racialSkills` section enables and tunes race-specific mechanics. This is one of the most important balance sections for multiplayer servers.

### Master toggle

| Key | Type | Default | What it does |
| --- | --- | --- | --- |
| `enableRacialSkills` | Boolean | `true` | Global on or off switch for racial skills. |

If `enableRacialSkills` is `false`, race-specific settings may remain in the file but should not take effect.

---

### Human

| Key | Type | Default | What it does |
| --- | --- | --- | --- |
| `humanRacialSkill` | Boolean | `true` | Enables the Human racial skill. |
| `humanKiRegenBoost` | Number | `1.4` | Human ki regeneration multiplier. `1.4` means 40 percent (40%) more ki regen. |

---

### Saiyan

| Key | Type | Default | What it does |
| --- | --- | --- | --- |
| `saiyanRacialSkill` | Boolean | `true` | Enables Saiyan racial skill systems. |
| `saiyanZenkaiAmount` | Integer | `3` | Maximum number of Zenkai activations available before reset. |
| `saiyanZenkaiHealthRegen` | Number | `0.2` | Health restored when Zenkai activates. |
| `saiyanZenkaiStatBoost` | Number | `0.1` | Stat boost multiplier applied on Zenkai activation. |
| `saiyanZenkaiBoosts` | String Array | `["STR", "SKP", "PWR"]` | Stats boosted by Zenkai. |
| `saiyanZenkaiCooldownSeconds` | Integer | `900` | Cooldown between Zenkai activations. |

### Namekian

| Key | Type | Default | What it does |
| --- | --- | --- | --- |
| `namekianRacialSkill` | Boolean | `true` | Enables Namekian racial skill systems. |
| `namekianAssimilationAmount` | Integer | `4` | Maximum number of assimilation uses. |
| `namekianAssimilationHealthRegen` | Number | `0.35` | Health restored on assimilation. |
| `namekianAssimilationStatBoost` | Number | `0.15` | Stat boost from assimilation. |
| `namekianAssimilationBoosts` | String Array | `["STR", "SKP", "PWR"]` | Stats boosted by assimilation. |
| `namekianAssimilationOnNamekNpcs` | Boolean | `true` | Allows assimilation on Namek NPCs such as traders and villagers. |

### Frost Demon

| Key | Type | Default | What it does |
| --- | --- | --- | --- |
| `frostDemonRacialSkill` | Boolean | `true` | Enables Frost Demon racial skill systems. |
| `frostDemonTPBoost` | Number | `1.25` | TP gain multiplier for Frost Demons. `1.25` means 25 percent more TP gain, and the config notes this stacks additively with other TP gain multipliers. |

### Bio-Android

| Key | Type | Default | What it does |
| --- | --- | --- | --- |
| `bioAndroidRacialSkill` | Boolean | `true` | Enables Bio-Android racial skill systems. |
| `bioAndroidCooldownSeconds` | Integer | `180` | Cooldown for the Bio-Android drain skill. |
| `bioAndroidDrainRatio` | Number | `0.25` | Damage dealt as a fraction of the target HP. User heals for the same amount. |

### Majin

| Key | Type | Default | What it does |
| --- | --- | --- | --- |
| `majinAbsoprtionSkill` | Boolean | `true` | Enables Majin absorption skill. Note the key name is spelled `Absoprtion` in the file, so keep that exact spelling unless the code changes. |
| `majinReviveSkill` | Boolean | `true` | Enables Majin revive skill. Config comment says this is still work in progress. |
| `majinAbsorptionAmount` | Integer | `3` | Maximum number of absorptions. |
| `majinAbsorptionHealthRegen` | Number | `0.3` | Health regen gained on absorption. |
| `majinAbsorptionStatsCopy` | Number | `0.1` | Fraction of target stats copied during absorption. |
| `majinAbsorptionBoosts` | String Array | `["STR", "SKP", "PWR"]` | Stats affected by absorption bonuses. |
| `majinAbsorptionOnMobs` | Boolean | `true` | Allows absorption on mobs. |
| `majinReviveCooldownSeconds` | Integer | `3600` | Cooldown for Majin revive. |
| `majinReviveHealthRatioPerBlop` | Number | `0.25` | Health ratio restored per blop on revive. |

## storage

The `storage` section controls where player data is saved.

### Supported storage types

The valid values for storing DMZ information and data are:

- `NBT`
- `JSON`
- `DATABASE`

Case matters. (case sensitive)

### Settings

| Key | Type | Default | What it does |
| --- | --- | --- | --- |
| `storageType` | String | `NBT` | Selects the backend used for player data storage. |
| `host` | String | `localhost` | Database host, used when `storageType` is `DATABASE`. |
| `port` | Integer | `3306` | Database port, used when `storageType` is `DATABASE`. |
| `database` | String | `dragonminez` | Database name. |
| `table` | String | `player_data` | Table name for stored player records. |
| `username` | String | `root` | Database user. |
| `password` | String | `password` | Database password. Change this in production. |
| `poolSize` | Integer | `10` | Connection pool size for database access. |
| `threadPoolSize` | Integer | `4` | Thread pool size for storage-related tasks. |