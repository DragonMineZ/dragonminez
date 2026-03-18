# Custom Races

You can create a new race only with config files. No Java code or addon jar is required.

## How DMZ discovers races

DMZ loads built-in races first, then scans subfolders inside:

- `config/dragonminez/races/`

Any extra folder name there is treated as a custom race key.
For a good starting point, we recommend copying any folder desired and renaming it, 
then tuning values inside.

Reference: `src/main/java/com/dragonminez/common/config/ConfigManager.java` (`loadAllRaces`).

Example source folder:

- `run/config/dragonminez/races/saiyan/`

## 1) Configure character defaults (`character.json`)

Main fields you will use often:

- `raceName`: should match your folder key.
- `hasGender`: set `true` for male/female variants. For custom models, if true DMZ will look for model 
  `model_male.geo.json` and `model_female.geo.json`.
- `customModel`: optional model key used by DMZ model resolution, 
  located in assets/dragonminez/geo/entity/races/
- `canUseHair`: allows DMZ hair editor behavior for the race.
- Default colors/types: body, hair, eyes, nose, mouth, tattoos.
- `formSkillsCosts`: TP unlock costs per form group.
- '`racialSkill`: optional skill key for a racial skill, this can be different values:
  - `human`, `saiyan`, `namekian`, `frostdemon`, `bioandroid`, or `majin`.

Schema source:

- `src/main/java/com/dragonminez/common/config/RaceCharacterConfig.java`

## 2) Configure base stat classes (`stats.json`)

DMZ expects class blocks like `warrior`, `spiritualist`, and `martialartist`. You can make your own
classes and tune their stats, but these three are used by default for form unlocks and skill requirements.

Inside each class:

- `baseStats` (`STR`, `SKP`, `RES`, `VIT`, `PWR`, `ENE`)
- `statScaling` (`*_scaling` keys)
- Regen values (`healthRegenRate`, `energyRegenRate`, `staminaRegenRate`)

Schema source:

- `src/main/java/com/dragonminez/common/config/RaceStatsConfig.java`

## 3) Add form groups (`forms/*.json`)

Each file in `forms/` defines one group (example: `supersaibaman.json`).

Important per-form fields:

- `name`
- `formtype`: Default formtypes include: `super, legendary, god, android`.
- `customModel`: Your own custom model for this form, located in assets/dragonminez/geo/entity/forms/
- `forms`: Literally the custom forms you can make.
- multipliers and drains
- color/design overrides (`bodyColor1`, `hairColor`, `eye1Color`, etc)

Schema source:

- `src/main/java/com/dragonminez/common/config/FormConfig.java`

## 4) Add race translation keys

Race names and form names are translated with keys like:

- `race.dragonminez.<race_key>` (The name of the race)
- `race.dragonminez.<race_key>.desc` (A short description shown in character creation)
- `race.dragonminez.<race_key>.group.<group_key>` (The name of a form group, shown in character creation)
- `race.dragonminez.<race_key>.form.<group_key>.<form_key>` (The name of a form, shown in character creation)

Default language file example:

- `src/main/resources/assets/dragonminez/lang/en_us.json`

Feel free to edit your en_us.json or create a new language file for your custom race. You don't need to add translation
keys for anything else, our code automatically will merge already-made translations in Crowdin with your own.

## 5) Reload and validate

1. Save your JSON files.
2. Run `/dmzreload`.
3. Open character creation and check your race appears.
4. Validate forms, colors, and scaling in-game.

