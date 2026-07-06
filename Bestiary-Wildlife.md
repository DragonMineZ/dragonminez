# Bestiary — Wildlife

Passive/ambient creatures that roam the world. They don't attack, and most exist to be hunted for food.

> Drops come from each creature's loot table (`data/dragonminez/loot_tables/entities/`). "Cooks if on fire" means the meat drops already cooked when the animal dies while burning. "Looting" means the count is boosted by the Looting enchantment (+0–1 per level).

## Dinosaurs (adult)

Three adult dinosaur species share the same drops and spawn conditions, differing only in model.

| | |
| :-- | :-- |
| Entity ids | `dragonminez:dino1`, `dragonminez:dino2`, `dragonminez:dino3` |
| Dimension | Overworld (Earth) |
| Where they spawn | Badlands, mountains, hills, DMZ "rocky" biomes, and near savanna villages |
| Spawn group | 2–4 (weights: dino1 = 60, dino2 = 50, dino3 = 40) |

**Drops:**

- **0–1 Raw Dino Tail** (cooks if on fire, +0–1 Looting)
- **1–2 Raw Dino Meat** (cooks if on fire, +0–1 Looting)

## Baby Dinosaur

| | |
| :-- | :-- |
| Entity id | `dragonminez:dinokid` |
| Dimension | Overworld (Earth) |
| Where it spawns | Same biomes as adult dinosaurs |
| Spawn group | 2–5 (spawn weight 30) |

**Drops:**

- **1–2 Raw Baby Dino Meat** (cooks if on fire, +0–1 Looting)

## Sabertooth

| | |
| :-- | :-- |
| Entity id | `dragonminez:sabertooth` |
| Dimension | Overworld (Earth) |
| Where it spawns | Savanna, forest and jungle biomes, near plains/savanna villages and woodland mansions |
| Spawn group | 1–4 (spawn weight 10) |

**Drops:**

- **1–2 Raw Beef** (cooks to Steak if on fire, +0–1 Looting)

## Namek Frog

The native amphibian of Planet Namek. There is also a rare **Ginyu variant** (`namek_frog_ginyu`) — a cosmetic nod to Captain Ginyu; it behaves and drops exactly like a normal frog.

| | |
| :-- | :-- |
| Entity ids | `dragonminez:namek_frog`, `dragonminez:namek_frog_ginyu` |
| Dimension | Planet Namek |
| Where they spawn | Ajissa Plains, Sacred Land and Namekian Rivers biomes |
| Spawn group | 1–3 (frog, weight 2) · 1 (Ginyu variant, weight 5) |

**Drops (both):**

- **1–2 Raw Frog Legs** (cooks if on fire, +0–1 Looting)

---

**Note:** the **Flying Dinosaur** (`DinoFlyEntity`) is a *hostile* flyer, not passive wildlife, and it does **not** spawn naturally (no spawn table) — it only appears via spawn egg or where specifically placed. Planet Namek's rivers and the Sacred Planet also host vanilla animals (fish, squid, and on the Sacred Planet: pigs, sheep, cows, etc.), which are not listed here.

Related: [[Bestiary|Bestiary]] · [[Bestiary-Hostiles|Bestiary-Hostiles]] · [[Bestiary-NPCs|Bestiary-NPCs]] · [[Dimension-Namek|Dimension-Namek]]
