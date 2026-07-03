# Weapon Attributes

Weapon Attributes is a datapack system that tells DMZ's combat engine how a specific item should behave in combat — attack range, animation, hitbox shape, damage multipliers, conditional attack variants (dual-wielding, sneaking, mounted, etc), and critical stats. This is the system third-party weapon mods need to hook into for proper DMZ combat behavior.

Loader: `WeaponRegistry.loadAttributes(resourceManager)`, `src/main/java/com/dragonminez/common/combat/logic/weapon/WeaponRegistry.java`. Reloads via vanilla datapack reload (`/reload` or restart) — **not** covered by `/dmzreload`.

## File location and item mapping

```text
data/<namespace>/weapon_attributes/<item_path>.json
```

The namespace + path becomes the target item id. `data/dragonminez/weapon_attributes/axe.json` applies to `dragonminez:axe`. A third-party mod would define its own weapons under its own namespace, e.g. `data/mymod/weapon_attributes/great_axe.json` for `mymod:great_axe`.

**Compatibility note:** any `bettercombat:` namespace found in these JSON files is automatically rewritten to `dragonminez:` at load time, so weapon-attribute datapacks written for the "Better Combat" mod largely work as-is.

## Top-level schema

```json
{
  "attributes": { ... }
}
```

or, to inherit from another item's attributes and only override specific fields:

```json
{
  "parent": "dragonminez:sword",
  "attributes": {
    "crit_chance": 0.10,
    "crit_damage": 0.15
  }
}
```

`parent` chains can go multiple levels deep (a parent can itself have a parent). Cycles are detected — a weapon whose parent chain loops back on itself fails to resolve and is skipped (logged as an error), so double check your `parent` references.

## `attributes` fields

| Field | Type | Notes |
| :-- | :-- | :-- |
| `attack_range` | Number | Reach in blocks (Minecraft default `3.0` if omitted). |
| `pose` | String | GeckoLib animation pose for the main-hand weapon. |
| `off_hand_pose` | String | GeckoLib animation pose for the off-hand weapon. |
| `two_handed` | Boolean | If `true`, disables off-hand usage while wielded. |
| `category` | String | Free-form category tag. Used for two things: matching `DUAL_WIELDING_SAME_CATEGORY` conditions between main/off hand, **and** as the target category for [[Combat|Combat]]'s `fallbackCompatibility` regex system. |
| `crit_chance` | Number | Innate crit chance (`0.0`-`1.0`), applied as an attribute modifier. |
| `crit_damage` | Number | Innate crit damage multiplier, applied as an attribute modifier. |
| `attacks` | Array | One or more attack variants — see below. Required. |

## `attacks` entries

Each entry describes one swing variant. DMZ checks `conditions` top-to-bottom and uses the first attack whose conditions all pass.

| Field | Type | Notes |
| :-- | :-- | :-- |
| `conditions` | List of condition strings | All must be true for this variant to fire. Omit for an unconditional attack. |
| `hitbox` | String | `FORWARD_BOX` (box extending forward, closest to vanilla), `VERTICAL_PLANE` (vertical sweeping slash), or `HORIZONTAL_PLANE` (wide horizontal sweep). |
| `damage_multiplier` | Number | Multiplier on the weapon's base damage (default `1.0`). |
| `angle` | Number | Swing arc in degrees. |
| `upswing` | Number | Cooldown multiplier: `cooldownTicks = (1 / (4 - attackSpeed)) * 20 * upswing`. Lower = faster follow-up. |
| `animation` | String | GeckoLib animation name. Validated against `animations/*.json`; if there's no exact match, DMZ fuzzy-matches (Levenshtein distance) after stripping known prefixes (`dragonminez:`, `bettercombat:`, `combat.`). |
| `swing_sound` / `impact_sound` | `{ "id", "volume"*, "pitch"*, "randomness"* }` | Sound played on swing/impact. `volume`/`pitch` default `1.0`, `randomness` defaults `0.1`. |

### Attack conditions (complete list)

| Condition | Meaning |
| :-- | :-- |
| `NOT_DUAL_WIELDING` | Off-hand is empty/air. |
| `DUAL_WIELDING_ANY` | Both hands hold something. |
| `DUAL_WIELDING_SAME` | Both hands hold the exact same item id. |
| `DUAL_WIELDING_SAME_CATEGORY` | Both hands hold items sharing the same `category`. |
| `NO_OFFHAND_ITEM` | Off-hand is empty. |
| `OFF_HAND_SHIELD` | Off-hand holds a shield. |
| `MAIN_HAND_ONLY` | Attack only applies to the main-hand weapon. |
| `OFF_HAND_ONLY` | Attack only applies to the off-hand weapon. |
| `MOUNTED` | Player is riding an entity. |
| `NOT_MOUNTED` | Player is not riding. |
| `SNEAKING` | Player is crouching. |
| `NOT_SNEAKING` | Player is standing. |

## Full example (dagger, multiple attacks + dual-wield variant)

```json
{
  "attributes": {
    "attack_range": 2,
    "two_handed": false,
    "category": "dagger",
    "attacks": [
      {
        "hitbox": "HORIZONTAL_PLANE",
        "damage_multiplier": 0.8,
        "angle": 150,
        "upswing": 0.5,
        "animation": "combat.one_handed_slash_horizontal_right_1",
        "swing_sound": { "id": "dragonminez:dagger_slash" }
      },
      {
        "hitbox": "HORIZONTAL_PLANE",
        "damage_multiplier": 0.8,
        "angle": 150,
        "upswing": 0.5,
        "animation": "combat.one_handed_slash_horizontal_right_2",
        "swing_sound": { "id": "dragonminez:dagger_slash" }
      },
      {
        "conditions": ["DUAL_WIELDING_SAME_CATEGORY", "MAIN_HAND_ONLY"],
        "hitbox": "FORWARD_BOX",
        "damage_multiplier": 1.4,
        "angle": 150,
        "upswing": 0.5,
        "animation": "combat.dual_handed_stab",
        "swing_sound": { "id": "dragonminez:dagger_slash" }
      }
    ]
  }
}
```

And an inheritance example (`brave_sword.json`, overriding only crit stats):

```json
{
  "parent": "dragonminez:sword",
  "attributes": {
    "crit_chance": 0.10,
    "crit_damage": 0.15
  }
}
```

## For third-party weapon mods

If your mod adds weapons and you don't ship a Weapon Attributes entry for them, DMZ falls back to regex-matching the item id against [[Combat|Combat]]'s `fallbackCompatibility` list (e.g. an item id containing `axe` gets treated like `dragonminez:axe`). This gives a reasonable default, but a real datapack entry — matching your weapon's actual moveset/animations — is always more accurate and always takes priority over the fallback guess.

## Reload and validate

1. Add/edit files under `data/<namespace>/weapon_attributes/`.
2. Validate the JSON syntax (see [[How to JSON|How-to-JSON]]).
3. Run vanilla `/reload`, or restart — this is a plain datapack reload listener, not part of `/dmzreload` (see [[Reloading and updating changes|Reloading-and-updating-changes]]).
4. Test the weapon in-game: range, animation, hitbox shape, and any conditional variants (dual-wielding, sneaking, mounted).
