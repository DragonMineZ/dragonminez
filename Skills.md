# Skills

Skill costs, cooldowns/level requirements, and per-race learnability all live in one file: `config/dragonminez/skills.json`.

Schema source: `src/main/java/com/dragonminez/common/config/SkillsConfig.java`.

## Top-level structure

| Key | Type | Purpose |
| :-- | :-- | :-- |
| `configVersion` | Number | Internal schema version. Do not hand-edit (see [[How to JSON\|How-to-JSON]]). |
| `kiSkills` | List of strings | Skills classified as ki-based (e.g. `kamehameha`, `final_flash`). |
| `formSkills` | List of strings | The transformation-tier skill names (`superforms`, `legendaryforms`, `godforms`, `androidforms`). |
| `stackSkills` | List of strings | The stack-form skill names (`kaioken`, `ultimate`). |
| `androidBlacklistedForms` | List of strings | Form skill names Androids are barred from learning (e.g. `superforms`, `legendaryforms`). |
| `strikeSkills` | List of strings | Melee/strike technique names (e.g. `meteor`, `dragon_fist`). |
| `skills` | Map | `skillName -> { costs, allowedRaces }`, see below. |
| `skillOfferings` | Map | `raceOrMasterName -> [skillName, ...]`, see below. |

There is no separate `skill-offerings.json` — `skillOfferings` lives inside `skills.json` alongside `skills`.

## `skills` — per-skill cost/eligibility

```json
"skills": {
  "kamehameha": { "costs": [2000], "allowedRaces": [] },
  "kaioken": { "costs": [1000, 1500, 2500, 4000, 7500], "allowedRaces": [] },
  "potentialunlock": { "costs": [500, 1000, 2000, 4000, 8000, 16000, 32000, 64000, 128000, -1] }
}
```

| Field | Type | Notes |
| :-- | :-- | :-- |
| `costs` | List of integers | Training-point cost to upgrade to each level. Index 0 = level 1's cost, and so on. A `-1` entry marks that level as not purchasable (locked). |
| `allowedRaces` | List of strings | Whitelist of races allowed to learn this skill. Empty list = all races can learn it. |

## `skillOfferings` — who can pick which skills

```json
"skillOfferings": {
  "goku": ["fly", "instant_transmission", "fusion", "kamehameha", "spiritbomb", "dragon_fist", "super_god_fist", "oozaru_fist"],
  "kingkai": ["kaioken", "potentialunlock", "kimanipulation", "kaioken_attack", "spiritbomb"],
  "roshi": ["jump", "meditation", "kicontrol", "kamehameha"],
  "default": ["jump"]
}
```

Keys are either a **race name** or a **master/trainer NPC name** — whichever entity is offering the skill decides which list applies. `"default"` is the fallback list used for anything not explicitly mapped.

## What you can and can't do via JSON

- **Retune existing skills:** yes — change `costs`, tighten/loosen `allowedRaces`, add/remove a skill from a race's or master's `skillOfferings` list, or move a skill between `kiSkills`/`strikeSkills`/etc.
- **Add a brand-new skill:** **no**, not via JSON alone. A new skill needs actual skill-behavior Java code and registration before you can reference its name here. JSON only controls cost/eligibility/offering for skills that already exist in code.

## Reload and validate

1. Edit `config/dragonminez/skills.json`.
2. Validate the JSON syntax (see [[How to JSON|How-to-JSON]]).
3. Run `/dmzreload config` (or `/dmzreload`) — see [[Reloading and updating changes|Reloading-and-updating-changes]]. Connected players get their skill limits and offerings resynced automatically.
4. Check the skill menu in-game for the race/master you changed.
