# Notice to Server Owners/Developers

Read this before you start editing anything in `config/dragonminez` or a world's `dragonminez/` save folder on a live server.

## Back up before you touch anything

DMZ will back up its **own** config files automatically when it detects an outdated/invalid one (renamed to `old_<filename>`, see [[How to JSON|How-to-JSON]]), but that safety net does **not** cover:

- Quest/saga/sidequest JSON in the world save — DMZ never overwrites these, which also means it never backs them up for you either.
- Player data (`playerdata_json/<uuid>.json` when using `JSON` storage, or your database when using `DATABASE` storage).
- Any custom race/form/entity file you authored yourself (these are treated as user-defined and loaded as-is, not versioned).

Take your own filesystem or database backup before bulk-editing any of these, especially on a server with real player data.

## This wiki assumes JSON/datapack-level editing, not gameplay support

This wiki is written for people who are comfortable editing JSON, running a dev environment, or building a Forge addon. It intentionally does not cover:

- General player walkthroughs or "how do I get Super Saiyan" questions — see the [Trello](https://trello.com/b/xewaqYee/dragonmine-z-trello) board instead.
- Lore or story explanations.
- Server hosting/administration outside of DMZ itself (ports, proxies, etc).

See [[Developer Scope and Prerequisites|Developer-Scope-and-Prerequisites]] for the full audience/scope breakdown.

## Never trust client input for progression

If you're writing an addon or a mixin against DMZ: stats, unlocks, permissions, quests, and wishes are validated server-side by design. Don't build addon features that trust a client-reported value for anything progression-related — read state from the server-side `StatsCapability`/`StatsProvider` instead of a client cache.

## Config edits can regenerate on you

If you hand-edit a config file into something DMZ can't parse, or that has a stale `configVersion`, DMZ silently replaces it with regenerated defaults (best-effort merging your old values) the next time it loads. This is expected behavior, not a bug — see [[How to JSON|How-to-JSON]] for exactly when it triggers, and always check for an `old_<filename>` backup after a config regenerates unexpectedly.

## Custom/community content survives updates, DMZ's own defaults don't (necessarily)

- Custom races, forms, and quests you author are preserved across DMZ updates.
- DMZ's own default/bundled config and story content can be regenerated when its internal version changes. Don't rely on hand-edited copies of DMZ's default files surviving an update untouched — fork them into your own custom race/form/saga instead if you want changes to be update-proof.

## Multiplayer/server-scale considerations

- Config changes made via `/dmzreload config` resync every currently connected player — expect a brief hitch for larger servers.
- Quest/saga edits only take effect for a world after `/dmzreload story` (or a restart); players already in a quest chain won't retroactively see structural changes to quests they've already progressed through.
- Don't switch storage backends (`NBT`/`JSON`/`DATABASE`) with players online — do it with the server empty and take a backup first.

## Getting help

- Discord: see the badges on the wiki [[Home]] page.
- File engine-level bugs or feature requests on the GitHub [Progress Board](https://github.com/orgs/DragonMineZ/projects/4).
