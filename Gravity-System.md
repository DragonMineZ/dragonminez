# Gravity System

The Gravity system (dimension gravity, the Gravity Device block, and weight-based training zones) is configured under the `gravity` key of `general-server.json`. It's one of the largest single sections in that file (60+ fields), so it gets its own page rather than bloating [[General Server|general-server]].

Schema source: `GravityConfig` nested class in `src/main/java/com/dragonminez/common/config/GeneralServerConfig.java`. Applied mostly in `GravityLogic.java` and `GravityDeviceBlockEntity.java`.

## How it works, in short

A player experiences **net gravity** from three stacking sources: their current dimension, proximity to a Kaio NPC, and any active Gravity Device block. Net gravity is reduced by the player's Resistance stat. Higher net gravity means: more TP/mastery gain (up to a point), stat penalties, movement/attack/jump/fly penalties, and higher ki/stamina drain. There's also a separate **weight-based training zone** system layered on top, based on how much "weight" the player is carrying relative to an ideal for their level — that's what produces the Comfort/Ideal/Overload TP multiplier bands.

## Base gravity sources

| Key | Type | Default | Notes |
| :-- | :-- | :-- | :-- |
| `enabled` | Boolean | `true` | Master toggle for the whole system. |
| `gravityPerWorld` | Map (dimension id -> Number) | `overworld: 1.0, nether: 8.0, end: 20.0, time_chamber: 10.0, otherworld: 1.0, namek: 1.0` | Base gravity per dimension. |
| `defaultWorldGravity` | Number | `1.0` | Fallback for dimensions not listed above. |
| `npcGravityValue` | Number | `10.0` | Gravity applied near a Kaio NPC. |
| `npcGravityRange` | Number | `100.0` | Radius (blocks) to detect a nearby Kaio NPC. |
| `machineGravityEnabled` | Boolean | `true` | Whether Gravity Device blocks contribute to gravity at all. |

## Resistance negation

| Key | Type | Default | Notes |
| :-- | :-- | :-- | :-- |
| `resistanceStatDivisorRatio` | Number | `0.9` | Resistance divisor, as a ratio of `maxValue` (from `general-server.json`'s `gameplay` section). Higher = RES negates gravity less per point. |
| `resistanceScale` | Number | `100.0` | Scales how much the negation term matters. |

```text
netGravity = rawGravity - [(avg of 6 stats × multipliers) / (maxValue * resistanceStatDivisorRatio)] * resistanceScale
```

## Stat reduction curve

| Key | Type | Default | Notes |
| :-- | :-- | :-- | :-- |
| `statReductionEnabled` | Boolean | `true` | Master toggle. |
| `affectedStats` | String list | `["STR", "SKP", "PWR", "DEF", "STM"]` | Which stats get penalized. |
| `statReductionPerGravity` | Number | `0.01` | Reduction fraction per 1 point of net gravity. |
| `minStatReduction` | Number | `0.0` | Floor. |
| `maxStatReduction` | Number | `0.9` | Ceiling (i.e. never more than 90% reduction). |

## Movement / attack / jump / fly penalties

| Key | Type | Default | Notes |
| :-- | :-- | :-- | :-- |
| `hardStopThreshold` | Number | `75.0` | Net gravity above which flight is fully disabled. |
| `maxMovementPenalty` | Number | `0.95` | Max movement-speed reduction. |
| `maxAttackPenalty` | Number | `0.9` | Max attack-speed reduction. |
| `penaltyCurveFactor` | Number | `1.6` | Curve steepness: `penalty = sqrt(netGravity / 100) * penaltyCurveFactor`, clamped to the relevant max above. |
| `physicalEnabled` | Boolean | `true` | Master toggle for jump/fall/fly penalties specifically. |
| `maxJumpPenalty` | Number | `0.95` | Max jump-height reduction. |
| `extraFallPerGravity` | Number | `0.02` | Extra fall-damage multiplier per point of net gravity. |
| `maxExtraFall` | Number | `0.6` | Ceiling on extra fall damage. |
| `maxFlyPenalty` | Number | `0.95` | Max flight-speed reduction (before the hard stop kicks in entirely). |

## TP/mastery bonus from gravity

| Key | Type | Default | Notes |
| :-- | :-- | :-- | :-- |
| `tpEnabled` | Boolean | `true` | Master toggle. |
| `tpPeakMultiplier` | Number | `2.0` | Cap on the gravity-derived TP multiplier. |
| `tpGravityBonusPerGravity` | Number | `0.025` | TP multiplier gained per point of net gravity (capped at `tpPeakMultiplier`). |
| `masteryBonusPerGravity` | Number | `0.0025` | Form mastery XP bonus per point of net gravity. |

## Weight training zones (Comfort / Ideal / Overload)

This is a separate multiplier layered on top of the gravity-derived TP bonus, based on carried "weight" relative to an ideal for the player's level.

| Key | Type | Default | Notes |
| :-- | :-- | :-- | :-- |
| `tpIdealBaseDivisor` | Number | `2.0` | Ideal weight = level-based capacity / this. |
| `gravitySensitivity` | Number | `1.0` | How much net gravity inflates effective weight: `loadFactor = 1.0 + (gravity - 1.0) * gravitySensitivity`. |
| `tpComfortRatioLow` | Number | `0.5` | Below this load ratio: comfort zone. |
| `tpIdealRatioLow` | Number | `0.75` | Ramp-up zone starts here. |
| `tpIdealRatioHigh` | Number | `1.25` | Peak zone ends here. |
| `tpOverloadRatio` | Number | `2.0` | Descent from peak starts here. |
| `tpOverloadHardRatio` | Number | `2.5` | At/above this ratio, multiplier bottoms out to `1.0`. |
| `tpComfortMultiplier` | Number | `1.5` | Multiplier in the comfort zone. |
| `tpHeavyMultiplier` | Number | `2.5` | Multiplier at the overload peak. |
| `maxWeightPenalty` | Number | `0.6` | Max stat penalty from being overloaded on weight. |

Load ratio = effective weight (base weight x `loadFactor`) / ideal weight. The multiplier interpolates smoothly between the ratio thresholds: `1.0` at ratio `0` -> `tpComfortMultiplier` at `tpComfortRatioLow` -> peak (`2.0`) across the ideal band -> `tpHeavyMultiplier` at `tpOverloadRatio` -> back down to `1.0` at `tpOverloadHardRatio`. This produces 5 named training zones players can see in-game: none, comfort, ideal, overload, hard overload.

## Ki/stamina drain from load

| Key | Type | Default | Notes |
| :-- | :-- | :-- | :-- |
| `loadDrainComfort` | Number | `1.15` | Drain multiplier in the comfort zone. |
| `loadDrainIdeal` | Number | `1.3` | Drain multiplier in the ideal zone. |
| `loadDrainHeavy` | Number | `1.5` | Drain multiplier in the overload zone. |
| `loadDrainOverload` | Number | `2.0` | Drain multiplier in the hard-overload zone. |
| `consumptionPerGravity` | Number | `0.04` | Additional flat drain per point of net gravity: `consumptionMultiplier = 1.0 + netGravity * consumptionPerGravity`. |

## Gravity Device block

| Key | Type | Default | Notes |
| :-- | :-- | :-- | :-- |
| `deviceMinRoomSize` | Integer | `5` | Minimum enclosed room dimension (blocks) for a valid chamber. |
| `deviceMaxRoomSize` | Integer | `25` | Maximum enclosed room dimension. |
| `deviceMaxGravity` | Integer | `1000` | Gravity cap the device can be set to. |
| `deviceEnergyCapacity` | Integer | `20000` | Forge Energy storage capacity. |
| `deviceEnergyPerGravityPerSecond` | Number | `1.0` | Energy consumption per second at gravity `1.0` (scales linearly with target gravity). |
| `deviceShaderGravityForMax` | Number | `60.0` | Gravity value at which the client-side visual shader reaches full intensity. |

The device validates its room with a flood-fill from its center: the enclosed space must be fully sealed (no gaps to the outside), and every dimension must fall between `deviceMinRoomSize` and `deviceMaxRoomSize`.

## Reload and validate

1. Edit the `gravity` block in `config/dragonminez/general-server.json`.
2. Validate the JSON syntax (see [[How to JSON|How-to-JSON]]).
3. Run `/dmzreload config` — see [[Reloading and updating changes|Reloading-and-updating-changes]].
4. Test in a Gravity Device or a high-gravity dimension: check TP gain rate, movement/attack penalties, and drain.
