# Dynamic Growth

Dynamic Growth is an alternate, parallel progression system to the standard TP-purchase model: instead of (or alongside) spending TP to buy stat points, players earn practice XP toward each stat just by using it — dealing damage, spending stamina/energy, training on the right kind of target. It's configured under the `dynamicGrowth` key of `general-server.json`; it gets its own page since it's a 20+ field subsystem with its own anti-farming logic.

Schema source: `DynamicGrowthConfig` nested class in `src/main/java/com/dragonminez/common/config/GeneralServerConfig.java`. Applied in `DynamicGrowthService.java`.

## Master toggles

| Key | Type | Default | Notes |
| :-- | :-- | :-- | :-- |
| `enabled` | Boolean | `true` | Master toggle for the whole system. |
| `debugChat` | Boolean | `false` | Sends chat messages showing practice XP gains — useful while tuning, noisy otherwise. |
| `practiceCurveEnabled` | Boolean | `true` | If `false`, practice XP gains use a flat `1.0x` instead of the curve. |
| `practiceXpMultiplier` | Number | `2.0` | Global multiplier applied to all practice XP before any per-stat or per-source multiplier. |

## Per-stat practice multipliers

| Key | Type | Default |
| :-- | :-- | :-- |
| `strPracticeMultiplier` | Number | `1.0` |
| `skpPracticeMultiplier` | Number | `1.0` |
| `resPracticeMultiplier` | Number | `1.0` |
| `vitPracticeMultiplier` | Number | `1.0` |
| `pwrPracticeMultiplier` | Number | `1.0` |
| `enePracticeMultiplier` | Number | `1.0` |

## Stamina/energy spending -> XP

| Key | Type | Default | Notes |
| :-- | :-- | :-- | :-- |
| `staminaSpentXpRatio` | Number | `0.1` | RES practice XP per point of stamina spent. |
| `energySpentXpRatio` | Number | `0.1` | ENE practice XP per point of ki spent. |
| `kiWeaponMeleePwrShare` | Number | `0.25` | Reserved for ki-weapon melee PWR XP share. |

These two award XP directly (via `awardStaminaSpent`/`awardEnergySpent`) and bypass the repeat-target anti-farm check below, since they aren't tied to a specific target entity.

## Combat TP interplay

| Key | Type | Default | Notes |
| :-- | :-- | :-- | :-- |
| `naturalCombatTpMultiplier` | Number | `1.0` | Multiplier for TP earned through Dynamic Growth during active combat. |
| `manualTpPurchasesEnabled` | Boolean | `true` | Whether players can still manually buy stat points with TP alongside practice growth. |
| `attributeTpCostMultiplier` | Number | `1.0` | Multiplier on manual TP purchase cost. |

## Practice-source multipliers

These scale XP down for "easy" or low-effort training so the system doesn't trivially outpace intended progression:

| Key | Type | Default | Applies when... |
| :-- | :-- | :-- | :-- |
| `passiveAnimalPracticeMultiplier` | Number | `0.5` | Training on passive mobs (cows, sheep, etc). |
| `villagerPracticeMultiplier` | Number | `0.1` | Training on protected NPCs (villagers, Namek traders, quest NPCs, masters). |
| `lowDamagePracticeMultiplier` | Number | `0.2` | The hit dealt less than 0.5 damage. |
| `shadowDummyPracticeMultiplier` | Number | `0.35` | Training on a shadow/training dummy entity. |
| `noRiskPracticeMultiplier` | Number | `0.5` | The target isn't targeting the player back (no real risk). |

Damage-based XP is also capped at 20% of the target's max health per hit, regardless of multipliers.

## Repeat-target anti-farming

Grinding the same target repeatedly falls off over a rolling time window:

| Key | Type | Default | Notes |
| :-- | :-- | :-- | :-- |
| `repeatTargetWindowSeconds` | Integer | `30` | Rolling window used to count hits on the same target. |
| `repeatTargetSoftCap` | Integer | `8` | Hit count where the soft falloff begins. |
| `repeatTargetHardCap` | Integer | `24` | Hit count where the hard floor is reached. |
| `repeatTargetSoftMultiplier` | Number | `0.5` | Multiplier at the soft cap. |
| `repeatTargetHardMultiplier` | Number | `0.15` | Multiplier at (and past) the hard cap. |

Between 0-8 hits on the same target within the window: full multiplier (`1.0`). Between 8-24 hits: linearly interpolated down from `1.0` to `0.5`. At 24+ hits: floors at `0.15`.

## Reload and validate

1. Edit the `dynamicGrowth` block in `config/dragonminez/general-server.json`.
2. Validate the JSON syntax (see [[How to JSON|How-to-JSON]]).
3. Run `/dmzreload config` — see [[Reloading and updating changes|Reloading-and-updating-changes]].
4. Test in-game: fight a mob and confirm practice XP accrues at the expected rate, and that repeated hits on the same target fall off as expected.
