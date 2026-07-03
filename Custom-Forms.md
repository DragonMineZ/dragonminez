# Custom Forms

DMZ has two places forms can live, and they use the exact same JSON schema and Java class (`FormConfig.java`) — only the folder they sit in changes what they mean:

| Location | What it is |
| :-- | :-- |
| `config/dragonminez/races/<race>/forms/*.json` | **Race forms** — belong to one race only (e.g. Super Saiyan only exists for Saiyans). Covered in [[Custom Races\|Custom-Races]]. |
| `config/dragonminez/forms/*.json` | **Stack forms** — shared across all races, and designed to layer on top of whatever race form is already active. |

This page is about the second kind: **stack forms**.

Loader: `ConfigManager.createOrLoadStackForms()` reads every file in `config/dragonminez/forms/` (`ConfigLoader.loadStackForms`).

## What a stack form actually is

A stack form doesn't replace your current transformation — it stacks its multipliers on top of it. The clearest example is Kaioken: a Saiyan can be Base or Super Saiyan and *also* be Kaioken x2 at the same time, multiplying whatever form they're already in. Built-in stack form groups: `kaioken` and `ultimate` (`StackForms.java`; Ultra Instinct and Ultra Ego groups exist in the same class but are currently disabled/commented out).

## File structure

One file per group, e.g. `config/dragonminez/forms/kaioken.json`:

```json
{
  "configVersion": 21.2,
  "groupName": "kaioken",
  "formType": "kaioken",
  "forms": {
    "x2": {
      "name": "x2",
      "unlockOnSkillLevel": 1,
      "keepBaseFormHeadBones": true,
      "strMultiplier": 1.1,
      "skpMultiplier": 1.1,
      "defMultiplier": 1.1,
      "pwrMultiplier": 1.1,
      "speedMultiplier": 1.1,
      "healthDrain": 0.03,
      "attackSpeed": 1.1,
      "auraColor": "#DB182C",
      "auraLayer": 1,
      "formStackable": true,
      "canAlwaysTransform": true,
      "incompatibleWith": ["ultimate.ultimate"],
      "shareMasteryWith": ["kaioken.x3"],
      "shareMasteryMultiplier": 0.25
    }
  }
}
```

`groupName` should match the filename (lowercase). Everything nested under `forms` is one form entry, keyed by its own `name`.

## Field reference

**Unlock & identity**

| Field | Type | Notes |
| :-- | :-- | :-- |
| `name` | String | Form key within the group (e.g. `x2`, `x3`, `ultimate`). |
| `unlockOnSkillLevel` | Integer | Skill level required to unlock this form. |
| `formCombo` | String | Optional key-combo id to trigger the transform. |
| `formRequisite` / `formRequisiteType` | String | Other form(s) required to use this one, as `"group.form"` (space-separated for multiple). `formRequisiteType` is `all` or `any`. |

**Visuals**

| Field | Type | Notes |
| :-- | :-- | :-- |
| `customModel` | String | Custom model id, resolved under `assets/dragonminez/geo/entity/forms/`. |
| `keepBaseFormHeadBones` | Boolean | Keep the head bones of whatever form this is stacking on. |
| `transformationAnimation` | String | Animation key played on transform. |
| `bodyColor1/2/3`, `hairColor`, `eye1Color`, `eye2Color` | Hex string | Color overrides. |
| `hairType`, `forcedHairCode` | String | Hair model/override. |
| `extraFormLayer`, `extraFormColor` | String/Hex | Extra visual overlay layer. |
| `auraType`, `auraColor`, `auraLayer` | String/Hex/Int | Primary aura. |
| `extraAuraType`, `extraAuraColor`, `extraAuraLayer` | String/Hex/Int | Secondary aura (`-1` = none). |
| `hasLightnings`, `lightningColor` | Boolean/Hex | Electricity effect. |
| `modelScaling` | `[x, y, z]` floats | Model scale factors. |
| `tintColor`, `tintIntensity` | Hex/Double | Screen tint while transformed. |
| `outlineShader` | Object (`enabled`, `primaryColor`, `secondaryColor`, `outlineThickness`) | Outline/glow shader. |

**Stats & drains** (multipliers stack on top of whatever base form is active)

| Field | Type | Notes |
| :-- | :-- | :-- |
| `strMultiplier`, `skpMultiplier`, `defMultiplier`, `vitMultiplier`, `pwrMultiplier`, `eneMultiplier`, `speedMultiplier` | Double | Stat multipliers. |
| `attackSpeed` | Double | Attack speed multiplier. |
| `staminaDrainMultiplier` | Double | Modifier applied to stamina drain per action while transformed. |
| `energyDrain`, `staminaDrain`, `healthDrain` | Double | Passive drain per second. |
| `otherworldTimeDrain` | Double | Time-limit drain multiplier while in Other World mode. |

**Mastery**

| Field | Type | Notes |
| :-- | :-- | :-- |
| `maxMastery` | Double | Max mastery obtainable (`0.0` = no mastery progression). |
| `masteryPerHitDealt`, `masteryPerHitReceived` | Double | Mastery gained per hit. |
| `passiveMasteryEveryFiveSeconds` | Double | Passive mastery tick gain. |
| `unlockOnMastery`, `stackOnMastery`, `instantTransformOnMastery`, `allowAlwaysTransformOnMastery`, `directTransformIfUsedOnMastery` | Double | Mastery-percent thresholds that unlock progressively easier access to the form. |
| `maxCostMultiplier`, `maxStatsMultiplier` | Double | Caps applied to skill/stat upgrades while this form is mastered. |
| `shareMasteryWith`, `shareMasteryMultiplier` | List/Double | Other forms (`"group.form"`) that share this form's mastery pool, and the rate they share at. |

**Stacking behavior**

| Field | Type | Notes |
| :-- | :-- | :-- |
| `formStackable` | Boolean | Whether this stack form can be combined with a race form at all. |
| `stackDrainMultiplier` | Double | Extra drain multiplier applied specifically while stacked. |
| `canAlwaysTransform` | Boolean | Can transform at any time, no cooldown gate. |
| `directTransformationIfUsed` | Boolean | Skip intermediate forms and transform straight to this one when triggered. |
| `incompatibleWith` | List of `"group.form"` | Forms that cannot be active at the same time as this one. |

**Costs & effects**

| Field | Type | Notes |
| :-- | :-- | :-- |
| `triggerItemCosts` | List of `{ itemId/itemTag, nbt, count, consume }` | Items required (and optionally consumed) to trigger the transform. |
| `durationItemCosts` | List of `{ itemId/itemTag, nbt, count, durationSeconds }` | Items consumed periodically to sustain the transform. |
| `mobEffects` | List of `{ effectId, amplifier, durationTicks, ambient, visible, showIcon }` | Potion effects applied while transformed. |

Schema source: `src/main/java/com/dragonminez/common/config/FormConfig.java`.

## Translation keys

```
race.dragonminez.stack.group.<groupName>       # e.g. "race.dragonminez.stack.group.kaioken" -> "Kaioken"
race.dragonminez.stack.form.<groupName>.<name>  # e.g. "race.dragonminez.stack.form.kaioken.x2" -> "x2"
```

Add these to your `en_us.json` or your own language file — see [[Custom Races|Custom-Races]] for how custom translations merge with Crowdin ones.

## Reload and validate

1. Save your file under `config/dragonminez/forms/`.
2. Validate the JSON syntax (see [[How to JSON|How-to-JSON]]).
3. Run `/dmzreload config` — see [[Reloading and updating changes|Reloading-and-updating-changes]].
4. Transform in-game and confirm stats, drains, visuals, and stacking behave as expected.
