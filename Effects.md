# Effects

DMZ adds a number of **status effects** (`MobEffect`s). Some are gameplay effects you'll actually notice — combat control, buffs and the Majin mark — while the rest are **internal state/cooldown markers** the mod uses to track transformations, charging, dashes, etc.

You can apply/clear them with `/dmzeffect` (see [[Commands|Commands]]).

## Combat control

| Effect | Id | What it does |
| :-- | :-- | :-- |
| **Stun** | `stun` | Locks you out of acting for a moment (~3s by default). Applied when your **guard is broken** — the classic block-break punish. |
| **Stagger** | `stagger` | A brief destabilize/interrupt in melee (e.g. from being parried). |
| **Ki Slow** | `ki_slow` | Slows the target's ki — hampering their ki regen and abilities. |
| **Candy** | `candy` | Majin Buu's candy beam — the target is turned into candy and left helpless. |

## Buffs

| Effect | Id | What it does |
| :-- | :-- | :-- |
| **Might Fruit** | `mightfruit` | The buff granted by eating a **Might Tree Fruit** (see [[Items|Items]]). |
| **TP Gain** | `tp_gain` | Boosts your **Training Point** gain (see [[Training Points & Mastery|Training-Points-and-Mastery]]). |
| **Mastery Gain** | `mastery_gain` | Boosts your **form mastery** gain. |
| **Ki Regen** | `ki_regen` | Boosts **Ki / Energy** regeneration. |
| **Stamina Regen** | `stamina_regen` | Boosts **Stamina** regeneration. |

## The Majin Mark

| Effect | Id | What it does |
| :-- | :-- | :-- |
| **Majin** | `majin` | Babidi's mark — a **permanent power boost** (×1.3 by default) that also puts the Majin "M" on your character. |

The Majin mark is obtained from **Master Babidi** (in Babidi's Ship — see [[Structures & Masters|Structures]]). You must be **evil-aligned** to be marked: if your [[Stats & Attributes|alignment]] is too high (good), Babidi refuses; if it's low enough (below ~39), he brands you. Once marked it stays with you permanently.

## The Mutant

| Effect | Id | What it does |
| :-- | :-- | :-- |
| **Mutant** | `mutant` | Marks you as the server's **"legendary form holder"** — the rare player who can wield their race's Legendary transformation line. |

**Benefits (defaults):**

- **Unlocks Legendary Forms** — Legendary forms are locked for everyone else (see [[Transformations & Mastery|Transformations-and-Mastery]]); only a Mutant can enter them.
- **+25% Training Point gain** and **+50% form-mastery gain** (see [[Training Points & Mastery|Training-Points-and-Mastery]]).
- **Scaled legendary power** — your Legendary form's stat bonus depends on whether you've learned the `legendaryforms` skill: **+33% stronger with it**, but **−67% (much weaker) without it** — so a Mutant should still train the skill.

**How you get it — the Mutant lottery:**

- The server runs a **lottery every 30 minutes** by default. Each roll it picks a player with a **20% chance**, up to **1 Mutant on the server at a time** (`maxHolders`). So it's a rare, random event — one lucky online player becomes the Mutant.
- Admins can grant/revoke it directly with `/dmzeffect give mutant <player>` (see [[Commands|Commands]]).
- **Lost on death** by default — dying frees the slot so the lottery can pick a new holder. (Tunable with `keepMutantOnDeath`.)

All of the above is configurable in the `mutant` section of [[General Server|general-server]].

## Internal / state markers

These are used by the mod to track state and cooldowns; you normally won't interact with them directly:

`transform`, `transformed`, `stack_transform`, `stack_transformed`, `kicharge`, `fly`, `fused`, `fusion_cd`, `majin_revive`, `saiyan_passive`, `bioandroid_passive`, `dash_cd`, `doubledash_cd`, `teleport_cd`, `ki_blast_cd`, `poise_cd`.

---

Related: [[Commands|Commands]] · [[Skills & Abilities|Abilities]] · [[Structures & Masters|Structures]] · [[Transformations & Mastery|Transformations-and-Mastery]] · [[Items|Items]]
