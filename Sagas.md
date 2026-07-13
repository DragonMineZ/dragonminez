# Sagas

The Saga system recreates the Dragon Ball story as a chain of quest battles. Their enemies are special combat NPCs whose behavior is driven by an **AI tier** — how smart and dangerous they are in a fight.

> AI tier is set **per encounter** (in the saga/quest data), so the *same* character can appear at different tiers in different fights. If a fight's `AITier` isn't set explicitly, it now defaults to the player's chosen difficulty (Easy → 1, Normal → 2, Hard → 3) instead of a flat Tier 1. See [[Custom Quests, Sagas & Sidequests|Custom-Quests-Sagas-and-Sidequests]] to edit encounters, and [[Raids (W.I.P)|Raids]] for the raid feature.

## AI tiers

| Tier | Name | Behavior |
| :-- | :-- | :-- |
| **1** | **Simple** | Basic melee fighter — chases, uses wild-sense reads and pre-set melee combos. **No ki attacks, no dashing.** Predictable fodder. |
| **2** | **Tactical** | A real fighter: **casts ki attacks** (chosen by situation), **dashes** to close distance, fires ranged/hitscan skills at range and uses guard-breakers when you block. |
| **3** | **Advanced** | Everything Tactical does, plus smart reads: **punishes you while you're casting or transforming**, bursts helpless targets, and uses **Zanzoken** (after-image dodges). Boss-level AI. |

Stats (HP/attack) are separate from AI tier and are scaled by quest difficulty and party size.

---

## Enemy list by saga

Counts show how many spawn in the fight. Where a character appears in more than one fight at different tiers, both are listed.

### Saiyan Saga
| Enemy | Count | AI |
| :-- | :-- | :-- |
| Raditz | 1 | 1 |
| Saibaman | 6 | 1 |
| Nappa | 1 | 2 |
| Vegeta | 1 | 2 |
| Great Ape Vegeta | 1 | 3 |

### Frieza Saga
| Enemy | Count | AI |
| :-- | :-- | :-- |
| Frieza Soldier (type 1) | 8 | 1 |
| Cui | 1 | 1 |
| Frieza Soldier (type 2) | 10 | 1 |
| Frieza Soldier (type 3) | 6 | 1 |
| Dodoria | 1 | 1 |
| Zarbon | 1 | 1 |
| Vegeta (Namek) | 1 | 2 |
| Guldo | 1 | 2 |
| Recoome | 1 | 2 |
| Burter | 1 | 2 |
| Jeice | 1 | 2 |
| Captain Ginyu | 1 | 2 |
| Ginyu (Goku's body) | 1 | 2 |
| Frieza 1st Form | 1 | 3 |
| Frieza 2nd Form | 1 | 3 |
| Frieza Final Form | 1 | 3 |
| Frieza Full Power | 1 | 3 |

### Android & Cell Saga
| Enemy | Count | AI |
| :-- | :-- | :-- |
| Mecha Frieza | 1 | 2 |
| King Cold | 1 | 1 |
| Android 19 | 1 | 2 |
| Dr. Gero | 1 | 2 |
| Android 18 | 1 | 2 |
| Android 17 | 1 | 2 |
| Imperfect Cell | 1 | 2 |
| Semi-Perfect Cell | 1 | 2 |
| Perfect Cell | 1 | 3 |
| Cell Jr. | 7 | 2 |
| Super Perfect Cell | 1 | 3 |

### Future Saga
| Enemy | Count | AI |
| :-- | :-- | :-- |
| Future Trunks (base) | 1 | 2 |
| Future Gohan (base) | 1 | 2 |
| Android 17 | 1 | 2 / 3 |
| Android 18 | 1 | 2 / 3 |
| Future Gohan (SSJ) | 1 | 3 |
| Imperfect Cell | 1 | 3 |

### Buu Saga
| Enemy | Count | AI |
| :-- | :-- | :-- |
| Goten | 1 | 1 / 2 |
| Gohan (base) | 1 | 2 |
| Kid Trunks | 1 | 1 / 2 |
| Vegeta (base) | 1 | 2 |
| Krillin | 1 | 1 |
| Supreme Kai (Shin) | 1 | 2 |
| Spopovich | 1 | 2 |
| Pui Pui | 1 | 1 |
| Yakon | 1 | 2 |
| Dabura | 1 | 2 |
| Fat Buu | 1 | 2 / 3 |
| Goku SSJ2 | 1 | 3 |
| Majin Vegeta | 1 | 3 |
| Babidi | 1 | 1 |
| Goku SSJ3 | 1 | 3 |
| Gotenks | 1 | 2 |
| Evil Buu | 1 | 3 |
| Android 18 | 1 | 2 |
| Super Buu | 1 | 3 |
| Gotenks SSJ3 | 1 | 3 |
| Super Buu (Gotenks absorbed) | 1 | 3 |
| Super Buu (Gohan absorbed) | 1 | 3 |
| Vegeta SSJ2 | 1 | 3 |
| Kid Buu | 1 | 3 |

### Movie Sagas
| Enemy | Count | AI |
| :-- | :-- | :-- |
| Garlic Jr. | 1 | 2 |
| Garlic Jr. (transformed) | 1 | 3 |
| Kid Gohan | 1 | 1 |
| Krillin | 1 | 1 |
| Dr. Wheelo | 1 | 3 |
| Goku (base) | 1 | 2 |
| Great Ape | 1 | 3 |
| Turles | 1 | 3 |
| Slug Soldier | 8 | 1 |
| Lord Slug | 1 | 2 |
| Giant Slug | 1 | 3 |
| Neiz | 1 | 1 |
| Salza | 1 | 1 |
| Dore | 1 | 1 |
| Cooler | 1 | 2 |
| Cooler 5th Form | 1 | 3 |
| Gete Robot | 10 | 1 |
| Metal Cooler | 1 | 3 |
| Metal Cooler Core | 1 | 3 |
| Android 14 | 1 | 2 |
| Android 15 | 1 | 2 |
| Android 13 | 1 | 2 |
| Super Android 13 | 1 | 3 |
| Broly (base) | 1 | 2 |
| Paragus | 1 | 1 |
| Broly (Legendary SSJ) | 1 | 3 |
| Bujin | 1 | 1 |
| Bido | 1 | 1 |
| Zangya | 1 | 1 |
| Gokua | 1 | 2 |
| Bojack | 1 | 2 |
| Bojack (Full Power) | 1 | 3 |
| Broly (SSJ) | 1 | 2 |
| Bio-Broly | 1 | 2 |
| Bio-Broly (Giant) | 1 | 3 |
| Pikkon | 1 | 2 |
| Janemba (fat) | 1 | 2 |
| Super Janemba | 1 | 3 |
| Hirudegarn (incomplete) | 1 | 2 |
| Hirudegarn | 1 | 3 |
| Super Hirudegarn | 1 | 3 |

---

Related: [[Custom Quests, Sagas & Sidequests|Custom-Quests-Sagas-and-Sidequests]] · [[Raids (W.I.P)|Raids]] · [[Bestiary|Bestiary]] · [[Entities|Entities]]
