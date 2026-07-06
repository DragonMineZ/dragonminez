# Structures

DMZ generates landmark structures across its worlds. Most of them are the **homes of masters** — the trainer NPCs who teach you [[Skills & Abilities|Abilities]] and [[Techniques|Techniques]]. This page lists each structure, the world it spawns in, the master inside, and the **alignment** you need to talk to them.

## Alignment to talk to a master

Some masters only deal with you if your [[Stats & Attributes|alignment]] fits their morality (bands: Good 61–100, Neutral 41–60, Evil 0–40):

- **Good-aligned masters** — **Goku, Gohan, King Kai, Old Kai, Roshi, Krillin** — require **Good alignment (≥ 61)**.
- **Evil-aligned masters** — **Cell, Frieza** — require **Evil alignment (≤ 40)**.
- **Every other master** (Piccolo, Vegeta, Trunks, Yamcha, Grand Elder, Dr. Gero, Babidi, Mr. Popo/Dende…) has **no alignment requirement** by default.

(Hostility toward you is a separate, configurable system — `alignment_rules.json`.)

## Structures

| Structure | World | Biome(s) | Master inside | Alignment to talk |
| :-- | :-- | :-- | :-- | :-- |
| **Goku's House** | Overworld | Plains | Goku | **Good (≥ 61)** |
| **Roshi's House** | Overworld | Oceans (coastal) | Master Roshi | **Good (≥ 61)** |
| **Kami's Lookout** | Overworld | Near world spawn (any biome) — also holds the **Time Chamber portal** | Mr. Popo / Dende | None |
| **Piccolo's House** | Overworld | Land biomes (forest, jungle, taiga, badlands, hills, mountains, rocky) | Piccolo | None |
| **Yamcha's House** | Overworld | Deserts | Yamcha | None |
| **Gero's Lab** | Overworld | Rocky biomes | Dr. Gero | None |
| **Cell Arena** | Overworld | Plains | Cell | **Evil (≤ 40)** |
| **Babidi's Ship** | Overworld | Mountains | Babidi | None |
| **Trunks' Ship** | Overworld | Land biomes | Trunks | None |
| **Vegeta's Pod** | Overworld | Rocky biomes | Vegeta | None |
| **Frieza's Ship** | **Planet Namek** | Ajissa Plains | Frieza | **Evil (≤ 40)** |
| **Grand Elder's House** (Elder Guru) | **Planet Namek** | Sacred Land | Grand Elder (Guru) | None |
| **Old Kai's Pillar** | **Sacred Planet** | Sacred Plains | Old Kai | **Good (≥ 61)** |
| **Time Chamber** | Overworld | Fixed (at Kami's Lookout) | — (portal to the [[Hyperbolic Time Chamber|Dimension-Hyperbolic-Time-Chamber]]) | — |

Each structure is placed **uniquely** per world (one instance, biome-gated), so they don't spawn on top of each other. The `generateCustomStructures` toggle enables/disables them (see [[General Server|general-server]]).

## Finding structures — locator NPCs

Two NPCs sell **treasure maps** (red X marker) that point straight to these structures:

- **Capsule Corp Assistant** (Earth villages) → maps to the Earth structures above.
- **CC-Namekian** (Planet Namek) → maps to Frieza's Ship and the Grand Elder's House.

See [[Bestiary-NPCs|Bestiary-NPCs]] for their trade costs.

---

Related: [[Dimensions|Dimensions]] · [[Bestiary-NPCs|Bestiary-NPCs]] · [[Skills & Abilities|Abilities]] · [[Stats & Attributes|Stats-and-Attributes]]
