# Gamerules

DMZ adds a few vanilla-style **gamerules** (set with `/gamerule <name> <value>`). They all control **ki-griefing** — whether ki attacks are allowed to damage/destroy the world — so server owners can protect terrain and builds.

| Gamerule | Default | What it does |
| :-- | :-- | :-- |
| **`allowKiGriefingPlayers`** | `true` | Whether ki attacks **fired by players** can break/damage blocks. Set to `false` to stop players blowing up terrain. |
| **`allowKiGriefingMobs`** | `true` | Whether ki attacks **fired by mobs/NPCs** (e.g. saga enemies) can break/damage blocks. |
| **`allowKiGriefingMasterStructures`** | `false` | Whether ki-griefing is allowed **inside master structures**. Off by default, so master homes (Goku's House, etc.) are protected even when the other two are on. |

**How they combine:** a ki blast can break a block only if the matching player/mob rule is `true` **and** — if the spot is inside a master structure — `allowKiGriefingMasterStructures` is also `true`.

> For finer, region-based control DMZ also integrates with **WorldGuard** if it's installed; otherwise these gamerules are the control.

---

Related: [[General Server|general-server]] · [[Structures & Masters|Structures]] · [[Commands|Commands]] · [[Permissions|Permissions]]
