# Dimensions

DMZ adds five custom dimensions on top of the Overworld. This page is the overview: what each one is for and how to reach it. Each dimension has its own page covering gravity, environment, unique blocks & ores, mobs and structures.

## Dimensions

| Dimension | Id | Gravity | Purpose | How to get there |
| :-- | :-- | :-- | :-- | :-- |
| [[Planet Namek|Dimension-Namek]] | `dragonminez:namek` | ×1.0 | Namekian home world — mining, Namek Dragon Balls, Frieza's Invasion raid, the Big Gete Star | Space Pod (always available) |
| [[Sacred Planet|Dimension-Sacred-Planet]] | `dragonminez:sacredkaiplanet` | ×1.0 | Home of the Kais — Old Kai, lore world | Space Pod, after Buu Saga quest 23 |
| [[Hyperbolic Time Chamber|Dimension-Hyperbolic-Time-Chamber]] | `dragonminez:time_chamber` | **×10** | Extreme-gravity training void | Portal at Kami's Lookout, or Space Pod after Bulma's *Temporal Comm-Link* sidequest |
| [[Otherworld|Dimension-Otherworld]] | `dragonminez:otherworld` | ×1.0 | The afterlife — death & revival, King Kai, Hell, the Otherworld Tournament | Dying (when the Otherworld is enabled), or Space Pod after Bulma's *Otherworld Drive* sidequest |
| [[Demon Realm|Dimension-Demon-Realm]] | `dragonminez:demon_realm` | ×1.0 | Dragon Ball Daima — three stacked Demon Worlds, demon villages, Gomah's army, the Tamagamis | Space Pod, after Daima Saga quest 3 (*To the Demon Realm*) |

Gravity values are the defaults of `gravityPerWorld` in [[General Server|general-server]] (see [[Gravity System|Gravity-System]]). Dimensions that aren't listed in `gravityPerWorld` (the Sacred Planet and the Demon Realm) use `defaultWorldGravity` (`1.0`).

## Getting around

- **Space Pod** — the main way to travel between worlds. Each destination has its own unlock rule (a quest, a saga step, a server toggle…). See [[Space Pod Destinations|Space-Pod-Destinations]] for the shipped list and how to add your own.
- **Death** — with the Otherworld enabled (`otherworldActive`), dying sends your spirit to the [[Otherworld|Dimension-Otherworld]].
- **`/dmzlocate <structure>`** — finds DMZ structures in the dimension you are in (see [[Structures|Structures]]).

## What each world holds

| World | Structures | World bosses | Raids |
| :-- | :-- | :-- | :-- |
| Overworld | Masters' homes, Kami's Lookout, Baba's Palace, Cell Arena, Tree of Might, Saiyan Craters | Turles | Saiyan Assault |
| Planet Namek | Frieza's Ship, Grand Elder's House, Namekian villages, Namekian Ruins, Big Gete Star | Metal Cooler Core | Frieza's Invasion |
| Sacred Planet | Old Kai's Pillar | — | — |
| Otherworld | King Yemma's check-in area, Snake Way & King Kai's Planet, Otherworld Tournament grounds | Janemba (in Hell) | — |
| Demon Realm | Demon villages, Gomah camps | Tamagami Number 3, 2 and 1 | Gomah's Army |

See [[Structures|Structures]], [[World Bosses|World-Bosses]], [[Raids|Raids]] and [[Tournaments|Tournaments]].

---

Related: [[Space Pod Destinations|Space-Pod-Destinations]] · [[Structures|Structures]] · [[Gravity System|Gravity-System]]
