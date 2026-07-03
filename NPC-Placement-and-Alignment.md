# NPC Placement & Alignment Rules

Two related per-world JSON files, both under `<world save>/dragonminez/npcs/`:

| File | Controls |
| :-- | :-- |
| `placements.json` | Where master/trainer NPCs spawn (position, dimension, initial alignment/relation). |
| `alignment_rules.json` | How a **player's** alignment stat affects each NPC's hostility/friendliness/interaction availability. |

Both load at server start and reload with `/dmzreload config` (or `/dmzreload all`) — see [[Reloading and updating changes|Reloading-and-updating-changes]].

## NPC Placement (`placements.json`)

Source: `src/main/java/com/dragonminez/server/world/npc/NPCPlacementManager.java`.

```json
{
  "schema": 1,
  "placements": [
    {
      "id": "master_roshi",
      "entity": "dragonminez:master_roshi",
      "dimension": "minecraft:overworld",
      "structure": "roshi_house",
      "x": 0.0,
      "y": 0.0,
      "z": 0.0,
      "yaw": 225.0,
      "pitch": 0.0,
      "surface": true,
      "relative_to_spawn": false,
      "enabled": true,
      "override": false
    }
  ]
}
```

| Field | Type | Notes |
| :-- | :-- | :-- |
| `id` | String | Unique placement id. Tagged onto the spawned entity so DMZ can find/update it later — don't reuse an id for two different placements. |
| `entity` | String | Entity type registry id. |
| `dimension` | String | Defaults to `minecraft:overworld`. |
| `npc_id` | String | Only meaningful for `dragonminez:quest_npc` entities — links to the quest system's NPC id. |
| `model` / `texture` | String | Only used for `quest_npc` entities. |
| `structure` | String | If set, `x`/`y`/`z` become an **offset** from that structure's origin instead of world coordinates. Valid values: `roshi_house`, `goku_house`, `elder_guru`, `timechamber`, `kamilookout`, `gero_lab`. An unrecognized value falls back to treating `x`/`y`/`z` as raw world coordinates (with a warning logged). |
| `x`, `y`, `z` | Number | World coordinates, or structure-relative offset (see above). Defaults `0.5`, `64.0`, `0.5`. |
| `yaw`, `pitch` | Number | Facing direction in degrees. |
| `surface` | Boolean | If `true`, snaps Y to the terrain surface at load time instead of using the given Y. |
| `relative_to_spawn` | Boolean | If `true`, adds the world's spawn point to `x`/`z` (applied after any structure offset). |
| `enabled` | Boolean | If `false`, this placement is skipped entirely. |
| `override` | Boolean | If `true`, always force-respawns the entity even if one already exists for this `id` — otherwise an existing entity is just updated in place. |
| `alignment` | Integer (0-100) | Optional initial alignment for this specific NPC instance. |
| `relation` | String | Optional per-instance override (`HOSTILE`/`NEUTRAL`/`FRIENDLY`), stored on the entity itself rather than looked up from alignment rules. |

### Quest NPCs vs. master NPCs

The default file only actively spawns **master/trainer NPCs** (Roshi, Goku, Karin, Dende, Popo, Gero, Guru, Kaiosama, Enma, Baba, Toribot). Quest NPCs (Bulma, Krillin, Yamcha, Tien, Piccolo, Gohan, Vegeta, Trunks, Chi-Chi, Videl, Shin, the Namek Elder) appear in the default file too, but with `"enabled": false` — they're no longer spawned at runtime through this system. They're baked directly into structure NBT so they live at fixed, hand-placed spots instead of piling up at world spawn. Their entries here are reference/documentation only.

Otherworld masters (Kaiosama, Enma, Baba, Toribot) always force `override: true` internally regardless of the file's value, since they need to reliably exist in a generated dimension.

## NPC Alignment Rules (`alignment_rules.json`)

Source: `src/main/java/com/dragonminez/common/alignment/NpcAlignmentRules.java`.

```json
{
  "npcs": {
    "goku": {
      "default_relation": "FRIENDLY",
      "interaction": { "min_alignment": 61 },
      "hostile_below": 25
    },
    "gero": {
      "default_relation": "NEUTRAL",
      "interaction": { "max_alignment": 60 }
    }
  }
}
```

Keys in `npcs` are the NPC id **with any namespace stripped** (`dragonminez:roshi` -> key `roshi`).

| Field | Type | Notes |
| :-- | :-- | :-- |
| `default_relation` | `HOSTILE` / `NEUTRAL` / `FRIENDLY` | Base relation when no threshold below overrides it. |
| `hostile_below` | Integer | If the player's alignment is below this, the NPC turns hostile regardless of `default_relation`. |
| `hostile_above` | Integer | Same, but for alignment above this value. |
| `interaction.min_alignment` | Integer | Player needs at least this alignment to talk/trade with the NPC — even if the NPC isn't hostile, low-enough alignment still blocks interaction. |
| `interaction.max_alignment` | Integer | Same, but as an upper bound. |

All four threshold fields are optional — omit whichever don't apply. Example reading of the `goku` entry above: below 25 alignment Goku is hostile; from 25-60 he's present but won't talk (alignment under the 61 `min_alignment`); at 61+ he's friendly and interactable.

Mechanically, this is checked by `NpcDispositionService.getRelation()` — hostility thresholds are checked first (they can override `default_relation` in either direction), then interaction bounds gate whether dialogue/trading is available at all. A per-instance `relation` set in `placements.json` overrides all of this for that specific NPC.

### Default-covered NPC ids

`goku`, `roshi`, `karin`, `guru`, `dende`, `popo`, `kingkai`, `gero`, `enma`, `baba`, `toribot`, `bulma`, `krillin`, `yamcha`, `tien`, `chiaotzu`, `gohan`, `trunks`, `chi_chi`, `videl`, `namek_elder`, `shin`, `piccolo`, `vegeta` — most default to `FRIENDLY` with `min_alignment: 41` (or `61` for the more "sacred" masters like Guru/Dende/Kingkai/Namek Elder) and `hostile_below: 25`; a few (`gero`, `enma`, `baba`, `toribot`, `vegeta`) default to `NEUTRAL` with no thresholds, and `piccolo` defaults `NEUTRAL` with its own thresholds (`min_alignment: 41`, `hostile_below: 20`).

## Reload and validate

1. Edit the file(s) under `<world save>/dragonminez/npcs/`.
2. Validate the JSON syntax (see [[How to JSON|How-to-JSON]]).
3. Run `/dmzreload config` (or `/dmzreload all`) — see [[Reloading and updating changes|Reloading-and-updating-changes]].
4. Confirm the NPC spawned/moved as expected, and that alignment-based hostility/interaction behaves as configured (test at a couple of alignment values if you can).
