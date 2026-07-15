# FAQ

Quick fixes for the most common issues players and server owners run into. If your problem isn't here, check the related reference pages linked at the bottom.

## "Priceless" forms / broken form values

If your transformations show up as **"Priceless"** (or otherwise have missing/broken values), your form config files are out of date or corrupted.

**Fix:** close the game/server, delete the `dragonminez` folder inside your `config` folder, then start back up (or run `/dmzreload`). DMZ regenerates the config from its current defaults.

> This only wipes **config** (`config/dragonminez`), not your world save or player progress. Any custom edits you made to those config files will be lost, so back them up first if you want to keep them. See [[Reloading and updating changes|Reloading-and-updating-changes]].

## Merchant01 and Shin — bugged quests from an older version

If **Merchant01** or **Shin** (or their quests) are stuck/bugged after updating from an earlier version, it's leftover quest data from the previous version.

**Fix:** run `/dmzrestoreupdate` (it prompts for confirmation first; run `/dmzrestoreupdate confirm` to skip the prompt). This restores the shipped quest/saga defaults for the new version. See [[Commands|Commands]].

## Mod structures aren't spawning

If DMZ's structures (houses, ships, arenas, etc.) don't generate in your world, you can place them manually with the vanilla `/place` command:

```
/place structure dragonminez:<structure>
```

For example, `/place structure dragonminez:goku_house`. Type `/place structure dragonminez:` and use tab-completion to see every available structure. See [[Structures|Structures]].

> Tip: `/dmzlocate <structureId>` finds where an already-generated structure is, e.g. `/dmzlocate goku_house` (see [[Commands|Commands]]).

## Changing the flight mode

To switch flight mode between **Combat** and **Exploration** flight, press **Alt + F**. See [[Controls|Controls]] and [[Skills & Abilities|Abilities]].

---

Related: [[Commands|Commands]] · [[Controls|Controls]] · [[Reloading and updating changes|Reloading-and-updating-changes]]
