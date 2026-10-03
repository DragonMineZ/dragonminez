# Custom Races

You can create a new race only with config files. No Java code or addon jar is required.

## How DMZ discovers races

DMZ loads its built-in races first (`human`, `saiyan`, `namekian`, `frostdemon`, `bioandroid`, `majin`, `glind`), then scans the subfolders of:

- `config/dragonminez/races/`

Any other folder name there is treated as a custom race key (use lowercase). Each race folder holds:

```text
config/dragonminez/races/<race_key>/
  character.json      # appearance, model, racial skill, form skill prices
  stats.json          # per-class base stats and scaling
  forms/*.json        # the race's transformation groups
```

If you create an empty folder, DMZ generates a `character.json` and a `stats.json` with neutral defaults the next time configs load. It does **not** generate any forms for a custom race; you add those yourself (see step 3).

For a good starting point, copy an existing race folder (for example `races/saiyan/`), rename it and tune the values inside.

Reference: `src/main/java/com/dragonminez/common/config/ConfigManager.java` (`loadAllRaces`, `createOrLoadRace`).

> **`configVersion`:** every file carries a `"configVersion"` string (currently `"2.2.0"`). A custom file with an older or missing version is backed up to `config/dragonminez/oldBackup/` and re-saved with the current version; your values are kept. A file with broken JSON syntax is never overwritten: it is reported (see [[Reloading and updating changes|Reloading-and-updating-changes]]) and skipped until you fix it.

## 1) Configure the character (`character.json`)

```json
{
  "configVersion": "2.2.0",
  "raceName": "saibaman",
  "hasGender": false,
  "useVanillaSkin": false,
  "customModel": "saibaman",
  "isLayered": true,
  "headBones": ["ears1"],
  "racialSkill": "namekian",
  "hasSaiyanTail": false,
  "auraType": "kakarot",
  "auraType3D": "userPreference",
  "defaultModelScaling": [0.9375, 0.9375, 0.9375],
  "defaultBodyType": 0,
  "slimBodyTypes": [],
  "defaultHairType": 0,
  "defaultEyesType": 0,
  "defaultNoseType": 0,
  "defaultMouthType": 0,
  "defaultTattooType": 0,
  "defaultBodyColor": "#3E8E2E",
  "defaultBodyColor2": "#2A5C20",
  "defaultBodyColor3": "#9BD36A",
  "defaultHairColor": "#222629",
  "defaultEye1Color": "#B40000",
  "defaultEye2Color": "#B40000",
  "defaultAuraColor": "#7FFF00",
  "formSkillsCosts": {
    "superforms": { "buyFromMaster": false, "prices": [20000, 45000, 80000] },
    "godforms": { "buyFromMaster": false, "prices": [] },
    "legendaryforms": { "buyFromMaster": false, "prices": [] }
  }
}
```

| Field | Type | Notes |
| :-- | :-- | :-- |
| `raceName` | String | Should match the folder key. |
| `hasGender` | Boolean | `true` gives the race a male/female choice. Custom models and textures then use `_male` / `_female` suffixes (see [[Custom Models and Assets\|Custom-Models-and-Assets]]). |
| `useVanillaSkin` | Boolean | When `true`, body type `0` renders the player's own Minecraft skin; the race's own body textures start at body type `1`. |
| `customModel` | String | Model key. Empty means the race looks like a Human. A built-in key (e.g. `namekian`, `majin`, `frostdemon`) reuses that model and its textures; any other key loads your own model. The built-in keys are listed on [[Custom Models and Assets\|Custom-Models-and-Assets]]. |
| `isLayered` | Boolean | `true` = the body is drawn from three tintable texture layers (body colors 1, 2 and 3) and the race uses its own face textures. `false` = one plain body texture and borrowed Human faces. |
| `headBones` | List of strings | Bone names the player can choose from in character customization (one at a time, plus a "none" option), e.g. ears, horns or antennas. Join bones with `+` to show several at once (`"ears1+horns2"`). The special entry `"hair"` enables the hair editor for the race. Bones are looked up in `geo/entity/raceparts.geo.json` first, then in the race model. |
| `racialSkill` | String | Which racial skill the race uses: `human`, `saiyan`, `namekian`, `frostdemon`, `bioandroid`, `majin` or `glind`. A custom race uses the same tuning as the race it borrows from (`general-server.json` → `racialSkills.<skill>`). See [[Skills & Abilities\|Abilities]]. |
| `hasSaiyanTail` | Boolean | Gives the race a Saiyan tail. |
| `auraType` | String | 2D aura sprite set: `kakarot` or `god`. |
| `auraType3D` | String | 3D aura style of the base form: `smooth`, `sparking` or `userPreference` (follows each player's own setting). |
| `aura3DStyle` | Object | Fine-tuning of the base 3D aura shape. The keys are listed on [[Custom Forms\|Custom-Forms]]. |
| `defaultModelScaling` | `[x, y, z]` | Base model size. |
| `defaultBodyType` | Integer | Body type selected by default in character creation. |
| `slimBodyTypes` | List of integers | Body types (other than `0`) that use slim arms. Body type `0` always follows the player's skin model. |
| `defaultHairType`, `defaultEyesType`, `defaultNoseType`, `defaultMouthType`, `defaultTattooType` | Integer | Default customization indices. |
| `defaultBodyColor`, `defaultBodyColor2`, `defaultBodyColor3`, `defaultHairColor`, `defaultEye1Color`, `defaultEye2Color`, `defaultAuraColor` | Hex string | Default colors in character creation. |
| `formSkillsCosts` | Object | One entry per form skill the race can level (see below). |

Schema source: `src/main/java/com/dragonminez/common/config/RaceCharacterConfig.java`.

### Form skill prices (`formSkillsCosts`)

Each key is a form skill: `superforms`, `legendaryforms`, `godforms` or `androidforms`. The value is either a plain array of prices or an object:

| Field | Notes |
| :-- | :-- |
| `prices` | TP price of each level, in order. The number of entries is the skill's maximum level, so an empty list means the race cannot level that skill. |
| `buyFromMaster` | `true` = the first level has to be bought from a master before the skill can be leveled from the Skills menu. |

A form unlocks when this skill reaches the form's `unlockOnSkillLevel` (see step 3).

### Android upgrade for custom races

Dr. Gero's android upgrade is offered to any race whose `formSkillsCosts` has at least one price under `androidforms` (by default only Humans). The upgrade puts the player into the `androidbase` form of the `androidforms` group, so the race also needs an `androidforms` form group in its `forms/` folder. The easiest way is to copy `races/human/forms/androidforms.json` into your race's `forms/` folder.

## 2) Configure the classes (`stats.json`)

`stats.json` holds a `classes` object with one block per class. DMZ generates the seven default classes: `warrior`, `spiritualist`, `martialartist`, `berserker`, `paladin`, `tank` and `cleric` (see [[Player Classes|Player-Classes]]). You can add your own class by duplicating a block under a new key.

Inside each class block:

| Field | Notes |
| :-- | :-- |
| `baseStats` | Starting stats: `STR`, `SKP`, `RES`, `VIT`, `PWR`, `ENE`. |
| `statScaling` | `STR_scaling`, `SKP_scaling`, `STM_scaling`, `DEF_scaling`, `VIT_scaling`, `PWR_scaling`, `ENE_scaling`. Optional `VIT_scaling_max` and `DEF_scaling_max`: the scaling grows from the normal value towards this maximum as the stat rises (up to the curve knee set in `general-server.json`); leave them out for flat scaling. |
| `baseHp5`, `hp5VitScaling` | Health regenerated every 5 seconds, plus a share of VIT. |
| `baseEp5`, `ep5EneScaling` | Ki regenerated every 5 seconds, plus a share of ENE. |
| `baseSp5`, `sp5StmScaling` | Stamina regenerated every 5 seconds, plus a share of stamina. |
| `tpCostMultiplier`, `tpGainMultiplier` | Class-wide multipliers on TP costs and TP gains. |
| `passive` | `enabled` plus a `values` map with the class passive's tuning (the keys depend on the class). |

Schema source: `src/main/java/com/dragonminez/common/config/RaceStatsConfig.java`. See [[Stats and Attributes|Stats-and-Attributes]] for what each stat does.

## 3) Add form groups (`forms/*.json`)

Each file in `races/<race_key>/forms/` defines one form group. Race forms use the exact same schema as stack forms, so the full field reference is on [[Custom Forms|Custom-Forms]]. The fields that matter most for a race group:

- `groupName`: the group key (lowercase). Groups are identified by this value, not by the filename.
- `formType`: the form skill that unlocks the group: `superforms`, `legendaryforms`, `godforms` or `androidforms`. The race needs prices for that skill in `formSkillsCosts`.
- `forms`: the forms of the group, in transformation order. Each form's `unlockOnSkillLevel` is the skill level that unlocks it.
- `customModel`: a model key for the form (looked up the same way as the race `customModel`).

Files whose name starts with `old_` are ignored.

## 4) Add race translation keys

Race and form names are translated with keys like:

- `race.dragonminez.<race_key>` (The name of the race)
- `race.dragonminez.<race_key>.desc` (A short description shown in character creation)
- `race.dragonminez.<race_key>.group.<group_key>` (The name of a form group)
- `race.dragonminez.<race_key>.form.<group_key>.<form_key>` (The name of a form)

Default language file example:

- `src/main/resources/assets/dragonminez/lang/en_us.json`

Feel free to edit your en_us.json or create a new language file for your custom race. You don't need to add translation keys for anything else, our code automatically will merge already-made translations in Crowdin with your own.

## 5) Reload and validate

1. Save your JSON files.
2. Run `/dmzreload config` — see [[Reloading and updating changes|Reloading-and-updating-changes]].
3. Open character creation and check your race appears.
4. Validate forms, colors, and scaling in-game.

Related: [[Custom Forms|Custom-Forms]] · [[Custom Models and Assets|Custom-Models-and-Assets]] · [[Races|Races]] · [[Transformations and Mastery|Transformations-and-Mastery]]
