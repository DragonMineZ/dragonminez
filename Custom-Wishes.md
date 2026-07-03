# Custom Wishes

You can retune what a dragon grants when summoned entirely with JSON, no Java required. There are two different places wish JSON lives depending on what you're trying to do:

| Goal | Where | Format |
| :-- | :-- | :-- |
| Override the wish list for an **existing** dragon (Shenron, Porunga) on your server/world. | `<world save>/dragonminez/wishes/<dragon_id>.json` | Plain JSON **array** of wish objects. |
| Ship a **new dragon** (or add wishes as part of a distributable datapack/addon pack). | Datapack `dragonminez/dragonballs/...` or an external pack under `<game dir>/dragonballs/` | JSON **object** with `dragon` + `wishes` fields. |

Both formats use the same wish objects underneath — only the outer wrapper differs.

## Every wish shares three base fields

```json
{
  "type": "item",
  "name": "wish.shenron.senzu.name",
  "description": "wish.shenron.senzu.desc"
}
```

| Field | Notes |
| :-- | :-- |
| `type` | One of the wish types below. |
| `name` | Translation key for the wish's display name. |
| `description` | Translation key for the wish's description. |

Source: `Wish.java` (base class), `WishTypeAdapter.java` (type -> class mapping).

## Wish types

| `type` | Extra fields | Example |
| :-- | :-- | :-- |
| `item` | `itemId`, `count` | `{ "type": "item", "name": "wish.shenron.senzu.name", "description": "wish.shenron.senzu.desc", "itemId": "dragonminez:senzu_bean", "count": 16 }` |
| `multi_wish` | `items`: list of `{ "a": "<item_id>", "b": <count> }` | `{ "type": "multi_wish", "name": "...", "description": "...", "items": [{ "a": "dragonminez:kikono_shard", "b": 32 }, { "a": "minecraft:iron_ingot", "b": 64 }] }` |
| `tps` | `amount` | `{ "type": "tps", "name": "...", "description": "...", "amount": 5000 }` |
| `skill` | `skill`, `level` | `{ "type": "skill", "name": "...", "description": "...", "skill": "saiyan_zenkai", "level": 2 }` |
| `command` | `commands`: string array, `%player%` is replaced with the player's name | `{ "type": "command", "name": "wish.shenron.revive.name", "description": "wish.shenron.revive.desc", "commands": ["dmzrevive %player%"] }` |
| `passivereset` | *(none)* | Resets racial passive skill bonuses (Zenkai/Assimilation/Absorption). |
| `recustomize` | *(none)* | Opens the character customization screen. |
| `relocatestats` | *(none)* | Lets the player redistribute their spent stat points. |
| `changedifficulty` | *(none)* | Lets the player pick their story difficulty again. |
| `resetstory` | *(none)* | Resets all story quest progress. |

Source: classes under `src/main/java/com/dragonminez/common/wish/wishes/`, dispatch table in `WishTypeAdapter.java`.

## Overriding an existing dragon's wishes

Drop a file named after the dragon's id in `<world save>/dragonminez/wishes/`, e.g. `shenron.json` or `porunga.json`. It's a **plain array**, no wrapper object:

```json
[
  {
    "type": "item",
    "name": "wish.shenron.senzu.name",
    "description": "wish.shenron.senzu.desc",
    "itemId": "dragonminez:senzu_bean",
    "count": 16
  },
  {
    "type": "tps",
    "name": "wish.shenron.tps.name",
    "description": "wish.shenron.tps.desc",
    "amount": 5000
  },
  {
    "type": "command",
    "name": "wish.shenron.revive.name",
    "description": "wish.shenron.revive.desc",
    "commands": ["dmzrevive %player%"]
  }
]
```

**Important:** this file **completely replaces** that dragon's merged wish list — it's not additive on top of the datapack-defined wishes. If you only want to add one wish and keep the rest, copy the dragon's full existing wish list first and then add yours to it.

If `shenron.json`/`porunga.json` don't exist yet, DMZ creates them with the default wish list the first time wishes load, so you always have a working example to start from. Any other filename you create here becomes a new dragon id as far as the wish system is concerned — see [Adding a brand-new dragon](#adding-a-brand-new-dragon) below for what else that needs.

Source: `WishManager.loadWishes()` / `loadWishConfig()`.

## Adding a brand-new dragon

A wish override file alone only works for a dragon id that already has a **dragon definition** (entity, ball set, summon rules). To add a wholly new dragon you also need a datapack-style definition, either bundled in a datapack under `data/dragonminez/dragonballs/<ball_set_id>/definitions/`, or as an external pack (see below).

`dragon.json`:

```json
{
  "id": "my_dragon",
  "entity_registry_name": "my_dragon",
  "entity_width": 3.0,
  "entity_height": 17.0,
  "dimensions": ["minecraft:overworld"],
  "ball_set": "my_ball_set",
  "wish_screen_id": "my_dragon",
  "wish_count": 1,
  "asset_definition": "my_dragon"
}
```

`wishes.json` (note the wrapper — this is the datapack format, different from the config-folder array format above):

```json
{
  "dragon": "my_dragon",
  "wishes": [
    { "type": "item", "name": "wish.my_dragon.senzu.name", "description": "wish.my_dragon.senzu.desc", "itemId": "dragonminez:senzu_bean", "count": 16 }
  ]
}
```

DMZ guarantees every dragon it knows about ends up with at least an empty wish list, even if you forget `wishes.json` (`DragonWishRegistry` fills in `List.of()` for any dragon missing one).

Source: `DragonDefinition.java`, `DragonDefinitionReloadListener.java`, `DragonWishRegistry.java`.

## External dragon ball addon packs

For a distributable pack (not bundled in your own datapack), DMZ also scans `<game dir>/dragonballs/` for folder or `.zip` packs with the same internal layout:

```text
<pack_name>/
  definitions/
    dragon.json
    ballset.json
    radar.json
    wishes.json
  assets/
    dragon.json
    ballset.json
    radar.json
```

Packs are loaded alphabetically and merge over the bootstrap/default definitions by id — a pack defining `dragon.json` with an existing dragon id overrides that dragon's definition. See [[Locating specific folders|Locating-specific-folders]] for the full folder map.

Source: `DragonBallPackManager.java`.

## Translation keys

Convention: `wish.<dragon_id>.<wish_key>.name` / `wish.<dragon_id>.<wish_key>.desc`, e.g. `wish.shenron.senzu.name`. Add these to your `en_us.json` or your own language file — see [[Custom Races|Custom-Races]] for how custom translations merge with Crowdin ones.

## Reload and validate

1. Save your wish override file(s) under `<world save>/dragonminez/wishes/`, or your datapack/addon pack.
2. Validate the JSON syntax (see [[How to JSON|How-to-JSON]]).
3. Config-folder overrides: run `/dmzreload wishes` (or `/dmzreload all`) — see [[Reloading and updating changes|Reloading-and-updating-changes]]. Datapack/addon-pack dragon definitions need a full restart (or vanilla `/reload` for the datapack-only parts) since they aren't part of the `/dmzreload` scopes.
4. Summon the dragon in-game and confirm the wish list matches what you expect.
