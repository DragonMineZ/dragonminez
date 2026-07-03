# combat.json

`combat.json` controls stamina/blocking/parrying, damage mitigation, combat flight, ki weapon tuning, and third-party weapon-mod compatibility. It is its own file — despite older wiki text implying otherwise, **this is not part of `general-server.json`.**

Location: `config/dragonminez/combat.json`. Schema source: `src/main/java/com/dragonminez/common/config/CombatConfig.java`.

## Core combat & status

| Key | Type | Default | What it does |
| :-- | :-- | :-- | :-- |
| `staminaConsumptionRatio` | Number | `0.083` | Stamina cost relative to actions taken. |
| `blockStaminaCost` | Number | `0.25` | Stamina cost per block action. |
| `knockdownDurationSeconds` | Integer | `30` | How long a player stays knocked down after being hit hard enough to trigger it. |
| `baselineFormDrain` | Integer | `80` | Baseline value forms scale their energy drain against (see [[Custom Forms|Custom-Forms]] for per-form `energyDrain`). |
| `killPlayersOnCombatLogout` | Boolean | `true` | If a player logs out mid-combat, they die on next login. |

## Blocking, parrying, poise

| Key | Type | Default | What it does |
| :-- | :-- | :-- | :-- |
| `enableBlocking` | Boolean | `true` | Master toggle for blocking. |
| `enableParrying` | Boolean | `true` | Master toggle for parrying (requires blocking enabled). |
| `parryWindowMs` | Integer | `150` | Timing window (ms) to land a parry. |
| `parryStaminaCostPenalty` | Number | `2.0` | Stamina cost multiplier applied when parrying. |
| `blockBreakStunDurationTicks` | Integer | `60` | Stun duration after a guard break. |
| `poiseDamageMultiplier` | Number | `0.25` | Fraction of incoming damage converted to poise damage while blocking. |
| `poiseRegenCooldown` | Integer | `100` | Ticks before poise starts regenerating again after taking damage. |
| `blockDamageReductionCap` | Number | `0.65` | Maximum damage reduction while blocking. |
| `blockDamageReductionMin` | Number | `0.05` | Minimum damage reduction while blocking. |

## Damage mitigation caps

| Key | Type | Default | What it does |
| :-- | :-- | :-- | :-- |
| `baseDamageReductionCap` | Number | `0.75` | Hard cap on damage reduction from armor/defense. |
| `enchantmentDamageReductionCap` | Number | `0.85` | Hard cap on damage reduction from enchantments. |
| `defenseDecayOnGuardBreak` | Number | `0.66` | Fraction of defense retained after a guard break. |
| `flatMitigationFactor` | Number | `0.10` | Flat damage-reduction multiplier (clamped to `>= 0.0`). |
| `flatMitigationMaxAbsorbFraction` | Number | `0.5` | Max fraction of a hit flat mitigation can absorb (clamped `0.0`-`1.0`). |
| `defenseReductionScale` | Number | `0.25` | Scale factor for defense reduction effects (clamped to `>= 0.01`). |

## Ki protection & infusion

| Key | Type | Default | What it does |
| :-- | :-- | :-- | :-- |
| `kiProtectionMitigationPerLevel` | Number | `0.01` | Damage reduction granted per level of ki protection. |
| `kiProtectionCostRatio` | Number | `0.5` | Ki cost ratio to apply ki protection. |
| `kiInfusionDamagePerLevel` | Number | `0.025` | Bonus damage per level of ki infusion. |
| `kiInfusionBaseCostPct` | Number | `2.5` | Base ki cost percentage to infuse. |
| `kiInfusionMaxCostPct` | Number | `7.5` | Max ki cost percentage as infusion scales. |

## Mobility

| Key | Type | Default | What it does |
| :-- | :-- | :-- | :-- |
| `enablePerfectEvasion` | Boolean | `true` | Master toggle for the perfect-evasion timing mechanic. |
| `perfectEvasionWindowMs` | Integer | `200` | Timing window (ms) for perfect evasion. |
| `dashCooldownSeconds` | Integer | `4` | Cooldown for a single dash. |
| `doubleDashCooldownSeconds` | Integer | `12` | Cooldown for a double dash. |
| `teleportCooldownSeconds` | Integer | `30` | Cooldown between Instant Transmission-style teleports. |

## Combat flight

| Key | Type | Default | What it does |
| :-- | :-- | :-- | :-- |
| `combatFlyAutoSwitchOnDamage` | Boolean | `true` | Automatically switches the player into flight mode when they take damage. |
| `combatFlyLockSeconds` | Integer | `8` | How long the player stays locked in flight mode after an auto-switch. |
| `combatFlyBaseSpeed` | Number | `0.30` | Base combat-flight speed. |
| `combatFlySprintSpeed` | Number | `0.50` | Combat-flight speed while sprinting. |
| `combatFlyHoldSpeedMultiplier` | Number | `1.6` | Speed multiplier while holding the movement key. |
| `combatFlyDrainMultiplier` | Number | `0.5` | Ki drain rate multiplier while combat-flying. |
| `combatFlyImpulseKiCostPct` | Number | `0.05` | Ki cost (%) for a flight impulse/boost. |
| `combatFlyImpulseCooldownTicks` | Integer | `25` | Cooldown (ticks) between flight impulses. |

## Attack pacing

| Key | Type | Default | What it does |
| :-- | :-- | :-- | :-- |
| `upswingMultiplier` | Number | `0.5` | Damage multiplier for upswing attacks (clamped `0.2`-`1.0`). |
| `allowAttackingMount` | Boolean | `false` | Whether players can attack while mounted. |
| `attackIntervalCap` | Integer | `2` | Minimum ticks required between attacks. |

## Ki weapons (`kiWeaponsConfig`)

A map keyed by weapon type (`blade`, `scythe`, `clawlance`). Each entry:

```json
"kiWeaponsConfig": {
  "blade": {
    "baseDamage": 0.0,
    "kiScalingDamage": 0.25,
    "baseKiCost": 0.0,
    "kiScalingCost": 0.075,
    "attackSpeed": -2.4,
    "forcedColor": "#FFFFFF",
    "weaponCombo": "dragonminez:sword"
  },
  "scythe": { "baseDamage": 0.0, "kiScalingDamage": 0.45, "baseKiCost": 0.0, "kiScalingCost": 0.105, "attackSpeed": -2.8, "forcedColor": "#FFFFFF", "weaponCombo": "dragonminez:scythe" },
  "clawlance": { "baseDamage": 0.0, "kiScalingDamage": 0.65, "baseKiCost": 0.0, "kiScalingCost": 0.175, "attackSpeed": -2.6, "forcedColor": "#FFFFFF", "weaponCombo": "dragonminez:trident" }
}
```

| Field | Type | Notes |
| :-- | :-- | :-- |
| `baseDamage` | Number | Flat base damage before ki scaling. |
| `kiScalingDamage` | Number | Damage added per point of scaling (ki-based). |
| `baseKiCost` | Number | Flat ki cost per attack. |
| `kiScalingCost` | Number | Extra ki cost as damage scaling increases. |
| `attackSpeed` | Number | Vanilla-style attack-speed attribute modifier (negative = slower). |
| `forcedColor` | Hex string | Particle/render color for the weapon's ki effect. |
| `weaponCombo` | String | Combo/animation set id the weapon uses (see [[Implementation & Events|Implementation-&-Events]] and [[Weapon Attributes|Weapon-Attributes]]). |

## Weapon registry

| Key | Type | Default | What it does |
| :-- | :-- | :-- | :-- |
| `weaponRegistryLogging` | Boolean | `false` | Logs weapon-registry resolution for debugging. |
| `weaponRegistryCompression` | Boolean | `true` | Compresses the synced weapon registry payload sent to clients. |

## Player combat relations

| Key | Type | Default | What it does |
| :-- | :-- | :-- | :-- |
| `playerRelations` | Map (entity id -> `HOSTILE`/`NEUTRAL`/`FRIENDLY`) | `minecraft:player: HOSTILE`, `minecraft:villager: NEUTRAL`, `minecraft:iron_golem: NEUTRAL`, `guardvillagers:guard: NEUTRAL` | Per-entity override for how combat targeting treats that entity. |
| `masteryBlacklistEntities` | List of entity ids | `minecraft:silverfish`, `dummmmmmy:target_dummy` | Entities excluded from mastery/combat-progress gains (e.g. training dummies). |
| `playerRelationToPassives` | `HOSTILE`/`NEUTRAL`/`FRIENDLY` | `HOSTILE` | Default relation toward passive mobs not otherwise listed. |
| `playerRelationToHostiles` | same | `HOSTILE` | Default relation toward hostile mobs not otherwise listed. |
| `playerRelationToOther` | same | `HOSTILE` | Default relation toward anything else. |

## Fallback compatibility for third-party weapons

When a weapon comes from another mod and has no dedicated [[Weapon Attributes|Weapon-Attributes]] entry, DMZ matches its item id against a list of regexes to infer a sensible weapon category:

| Key | Type | Default | What it does |
| :-- | :-- | :-- | :-- |
| `fallbackCompatibilityEnabled` | Boolean | `true` | Master toggle for this system. |
| `blacklistItemIdRegex` | String | `"pickaxe"` | Item ids matching this regex are never treated as a DMZ-style weapon. |
| `fallbackCompatibility` | List of `{ item_id_regex, weapon_attributes }` | 20 default entries | Ordered regex -> category mappings, first match wins. |

Default mappings (`item_id_regex` -> `weapon_attributes` category):

| Regex | Category |
| :-- | :-- |
| `claymore\|great_sword\|greatsword` | `dragonminez:claymore` |
| `great_hammer\|greathammer\|war_hammer\|warhammer\|maul` | `dragonminez:hammer` |
| `double_axe\|doubleaxe\|war_axe\|waraxe\|great_axe\|greataxe` | `dragonminez:double_axe` |
| `scythe` | `dragonminez:scythe` |
| `halberd\|glaive\|pike\|lance\|naginata` | `dragonminez:halberd` |
| `spear\|trident\|pitchfork\|javelin` | `dragonminez:spear` |
| `battlestaff\|staff\|quarterstaff\|pole` | `dragonminez:battlestaff` |
| `katana\|uchigatana\|nodachi\|tachi` | `dragonminez:katana` |
| `rapier\|foil` | `dragonminez:rapier` |
| `dagger\|knife\|shiv\|dirk\|kunai\|karambit\|wakizashi\|tanto` | `dragonminez:dagger` |
| `sickle\|kama` | `dragonminez:sickle` |
| `soul_knife` | `dragonminez:soul_knife` |
| `claw\|katar` | `dragonminez:claw` |
| `wand` | `dragonminez:wand` |
| `mace\|hammer\|flail` | `dragonminez:mace` |
| `axe` | `dragonminez:axe` |
| `coral_blade` | `dragonminez:coral_blade` |
| `twin_blade\|twinblade` | `dragonminez:twin_blade` |
| `cutlass\|scimitar\|machete` | `dragonminez:cutlass` |
| `sword\|blade` | `dragonminez:sword` (catch-all) |

If you're adding a weapon mod's item and want precise control instead of relying on regex inference, give it a real [[Weapon Attributes|Weapon-Attributes]] entry — the datapack entry always wins over the fallback guess.

## Reload and validate

1. Edit `config/dragonminez/combat.json`.
2. Validate the JSON syntax (see [[How to JSON|How-to-JSON]]).
3. Run `/dmzreload config` (or `/dmzreload`) — see [[Reloading and updating changes|Reloading-and-updating-changes]].
4. Test in-game: block/parry timing, ki weapon damage/cost, and (if relevant) a third-party weapon's inferred category.
