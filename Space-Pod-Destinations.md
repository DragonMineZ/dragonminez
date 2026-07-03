# Space Pod Destinations

Space Pod destinations (the list of places a player can warp to from the Space Pod menu) are a datapack system, not a `config/dragonminez` file — that makes them a good fit for datapacks and addons that want to add their own destinations (e.g. a custom dimension mod adding its own entry).

Loader: `SpacePodDestinationRegistry` (`src/main/java/com/dragonminez/common/spacepod/SpacePodDestinationRegistry.java`), a vanilla `SimpleJsonResourceReloadListener` over the `spacepod` directory. Reloads via vanilla `/reload` or restart — **not** covered by `/dmzreload`; it's synced to clients automatically on datapack sync.

## File location

```text
data/<namespace>/spacepod/destinations.json
```

Any namespace can contribute a `destinations.json` (or any filename under that folder) — they're all loaded together, sorted by resource location. DMZ's own bundled file is `data/dragonminez/spacepod/destinations.json`.

## Top-level shape

```json
{
  "replace": false,
  "destinations": [ ... ]
}
```

`replace` (optional, default `false`) — if `true`, clears every previously-loaded destination before adding this file's own. Use this if you want your pack to be the sole authority on the destination list instead of appending to others.

## Destination fields

| Field | Type | Notes |
| :-- | :-- | :-- |
| `id` | String | Unique destination id. |
| `name` | String | Translation key, or a literal string if `translate` is `false`. |
| `translate` | Boolean | Defaults to `true` (treat `name` as a translation key). |
| `dimension` | String | Target dimension id. |
| `icon_index` | Integer | Index into the built-in space-pod icon sheet. Exactly one of `icon_index`/`icon_texture` is required. |
| `icon_texture` | String | Custom texture resource location, as an alternative to `icon_index`. |
| `x`, `y`, `z` | Number | Target coordinates. Either provide **all three**, or **only `y`** (special case: keeps the player's current X/Z and only changes their Y), or omit all three. Providing just `x` or just `z` without the others is an error. |
| `show_when_locked` | Boolean | Defaults to `true`. If `true`, a locked destination still shows (grayed out); if `false`, it's hidden entirely until unlocked. |
| `unlock_rules` | String or object | The unlock condition — see below. |

## Unlock rule grammar

Primitives (bare strings):

| Rule | Unlocked when |
| :-- | :-- |
| `"ALWAYS"` | Always. |
| `"NEVER"` | Never — effectively disables the destination unless overridden by another pack. |
| `"KAIO_UNLOCKED"` | Player has unlocked Kaio's planet. |
| `"OTHERWORLD_ENABLED"` | Otherworld generation is enabled in server config. |

Conditions (object forms):

| Rule | Shape | Unlocked when |
| :-- | :-- | :-- |
| Quest | `{ "quest": "<quest_id>" }` | Player has completed that quest. Sidequest/saga quest ids both work (e.g. `"buu_saga:23"`). |
| Level | `{ "level": N }` | Player level >= `N`. |
| Race | `{ "race": "<race_name>" }` | Player's race matches (case-insensitive). |
| Tag | `{ "tag": "<player_tag>" }` | Player has this Minecraft scoreboard tag. |
| Visited dimension | `{ "visited_dimension": "<dimension_id>" }` | Player has previously visited that dimension. |
| Stat | `{ "stat": { "name": "<STAT_NAME>", "min": N } }` | Player's stat (e.g. `STRENGTH`) >= `N`. |

Composite operators (nest to arbitrary depth, mixing primitives/conditions/other composites freely):

```json
{ "and": [ <rule>, <rule>, ... ] }
{ "or":  [ <rule>, <rule>, ... ] }
{ "not": <rule> }
```

## Examples

Always available:

```json
{
  "id": "overworld",
  "name": "gui.dragonminez.spacepod.overworld",
  "dimension": "minecraft:overworld",
  "icon_index": 0,
  "unlock_rules": "ALWAYS"
}
```

Gated by an AND of a global toggle and a quest, with fixed coordinates:

```json
{
  "id": "otherworld",
  "name": "gui.dragonminez.spacepod.otherworld",
  "dimension": "dragonminez:otherworld",
  "icon_index": 2,
  "x": 54.0,
  "y": 210.0,
  "z": 1082.0,
  "unlock_rules": {
    "and": ["OTHERWORLD_ENABLED", { "quest": "bulma_otherworld_drive" }]
  }
}
```

Gated by a single quest, with the "keep X/Z, change Y only" coordinate form:

```json
{
  "id": "time_chamber",
  "name": "gui.dragonminez.spacepod.time_chamber",
  "dimension": "dragonminez:time_chamber",
  "icon_index": 2,
  "y": 130.0,
  "unlock_rules": { "quest": "bulma_time_chamber_link" }
}
```

A destination that's never unlockable through this pack (useful if you want it hidden unless another pack overrides it):

```json
{
  "id": "cereal",
  "name": "gui.dragonminez.spacepod.cereal",
  "dimension": "dragonminez:cereal_planet",
  "icon_index": 4,
  "unlock_rules": "NEVER"
}
```

A deeper nested example combining level/race with a tag exclusion:

```json
{
  "unlock_rules": {
    "and": [
      { "or": [{ "level": 10 }, { "race": "saiyan" }] },
      { "not": { "tag": "tutorial_skipped" } }
    ]
  }
}
```

## Client UI

Players browse destinations in `SpacePodScreen` (client-side), which shows up to 7 entries at once and grays out or hides locked ones per `show_when_locked`.

## Reload and validate

1. Add/edit `data/<namespace>/spacepod/destinations.json`.
2. Validate the JSON syntax (see [[How to JSON|How-to-JSON]]).
3. Run vanilla `/reload`, or restart — this isn't covered by `/dmzreload` (see [[Reloading and updating changes|Reloading-and-updating-changes]]).
4. Open the Space Pod menu in-game and confirm the destination appears/unlocks as expected.
