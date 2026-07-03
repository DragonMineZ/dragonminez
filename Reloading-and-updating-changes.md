# Reloading and Updating Changes

Not every change needs a server restart, but not every change can be picked up live either. This page breaks down what `/dmzreload` actually does and when you still need a restart or a resource reload.

## `/dmzreload [target]`

Source: `src/main/java/com/dragonminez/server/commands/ReloadCommand.java`. Requires the reload permission (`DMZPermissions.RELOAD`).

```
/dmzreload            -> reloads everything (same as "all")
/dmzreload all        -> config + story (quests/sagas) + wishes
/dmzreload config     -> general/race/form/skill/combat/entity config only
/dmzreload story      -> quests + sagas + sidequests only
/dmzreload wishes     -> dragon wishes only
```

| Scope | What it reloads |
| :-- | :-- |
| `config` | `ConfigManager.reload()`, `StorageManager.reload()`, NPC alignment rules, NPC placement (and respawns configured NPCs), then resyncs config + progression to every online player. |
| `story` | `QuestRegistry.loadAll(server)` — re-reads sagas/quests/sidequests from the world save, then resyncs the quest registry to every online player. |
| `wishes` | `WishManager.loadWishes(server)`, then resyncs wishes to every online player. |
| `all` | All of the above, in that order. |

After a reload, connected players are automatically resynced — you don't need them to relog for `config`, `story`, or `wishes` changes to apply.

## What `/dmzreload` covers

- `config/dragonminez/general-server.json`, `general-user.json`, `combat.json`, `skills.json`, `entities.json`
- `config/dragonminez/races/<race>/character.json` and `stats.json`
- `config/dragonminez/races/<race>/forms/*.json` and `config/dragonminez/forms/*.json` (stack forms)
- `<world save>/dragonminez/{sagas,quests,sidequests}` (with `story` or `all`)
- `config/dragonminez/wishes/*.json` (with `wishes` or `all`)

## What `/dmzreload` does NOT cover

- **Resource pack assets** (models, textures, animations under `assets/dragonminez/...`). These need a client resource reload (`F3+T`, or reconnecting) since they're loaded client-side, not from server config.
- **Datapack-only reload listeners** — `dragonminez/dragonballs/*.json` (base dragon ball/wish definitions), `space_pod_destinations/*.json`, `weapon_attributes/*.json`. These follow vanilla datapack rules; use `/reload` (vanilla) or restart the server to pick them up, since they aren't wired into `/dmzreload`'s scopes.
- **External dragon ball addon packs** under `<game dir>/dragonballs/` — these are merged at server start by `DragonBallPackManager`; changing them requires a restart.
- **Storage backend switch** (`NBT` <-> `JSON` <-> `DATABASE`) — this is read at server start and reloaded by `StorageManager.reload()` under the `config` scope, but switching backends mid-session with players online is not something you should do casually; prefer doing it with the server empty.
- **New Java code / addon jars** — obviously requires a restart.

## Practical workflow

1. Edit your JSON file(s).
2. Validate the JSON syntax (see [[How to JSON|How-to-JSON]]).
3. Run the narrowest `/dmzreload <target>` that covers your change — `config` for balance/race/form edits, `story` for quest/saga edits, `wishes` for wish edits.
4. Check server logs for `Regenerating <file>. Reason: ...` warnings — that means DMZ rejected your edit and fell back to defaults (backed up as `old_<filename>`).
5. For resource pack or datapack-only changes, do a client resource reload or a full restart instead.

If in doubt, `/dmzreload all` is safe to run at any time — it just re-runs every reload path and resyncs everyone.
