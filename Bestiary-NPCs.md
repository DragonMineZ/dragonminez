# Bestiary — NPCs

Neutral/friendly characters you can interact with. Besides trading, several of them have a **special role** — the "Capsule Corp" NPCs sell **treasure maps** that reveal the location of DMZ structures, and Namek Warriors defend their villages.

> Most NPCs are villagers or peaceful mobs and have **no custom drops**. Any exceptions are noted below.

## Capsule Corp Assistant (Earth)

The "CC-Villager". A custom villager profession (`cc_villager`) that generates inside **plains villages on Earth (Overworld)** only.

**Function beyond trading — structure locator.** For a price it sells filled **treasure maps** (marked with a red X) that lead to Earth structures, unlocked across its five trade tiers:

| Tier | Maps to | Cost |
| :-- | :-- | :-- |
| 1 | Goku's House | 10 Cooked Dino Meat |
| 1 | Roshi's House | 8 Cod |
| 2 | Piccolo's House | 1 Water Bucket |
| 2 | Yamcha's House | 1 Bone |
| 3 | Kami's Lookout | 1 Spruce Sapling |
| 3 | Cell Arena | 1 Tier-1 Radar Chip |
| 4 | Gero's Lab | 16 Iron Ingot |
| 4 | Trunks' Ship | 1 Iron Sword |
| 5 | Vegeta's Pod | 1 Red Scouter |
| 5 | Babidi's Ship | 4 Ender Pearl |

Each map trade can be used up to 8 times. **Drops:** none (standard villager).

## CC-Namekian (Namek)

The Namek counterpart of the Capsule Corp Assistant (`cc_namekian`), found **only on Planet Namek**.

**Function beyond trading — structure locator.** Sells treasure maps to Namek structures:

| Maps to | Cost |
| :-- | :-- |
| Frieza's Ship | 1 Tier-1 Radar Chip |
| Grand Elder's House (Elder Guru) | 1 Healing Bucket |

Each map trade can be used up to 8 times. **Drops:** none (trader villager).

## Namek Villager / Trader (Namek)

The general-goods Namekians (`namek_trader`) found on Namek. **Function:** regular trading — e.g. Raw Frog Legs, Healing Buckets, Namek Blocks, Tier-2 Radar Chips, and Netherite Scrap in exchange for emeralds and materials. **Drops:** none.

## Namek Warrior (Namek)

Namekian defenders (`namek_warrior`) found on Namek. **Function:** they do **not** trade — they guard Namekian villages, attacking hostile mobs (village alert/defend AI, ~15 melee damage) and can fly. **Drops:** none.

## Quest NPCs

A single data-driven entity (`questnpc`) that represents **all** quest givers; model, texture and dialogue are resolved per NPC from data. **Function:** open quest dialogue and hand out quests/objectives. **Drops:** none.

See [[Custom Quests, Sagas & Sidequests|Custom-Quests-Sagas-and-Sidequests]] for authoring quest NPCs.

---

Related: [[Bestiary|Bestiary]] · [[Structures|Structures]] · [[Dimension-Namek|Dimension-Namek]] · [[Items|Items]]
