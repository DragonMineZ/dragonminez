# Skills & Abilities

This page lists the **skills** you can learn in DMZ and what they do, plus the **racial passives** (race abilities), how to use them, their limits, and how to reset them.

> ⭐ **Learn Ki Control first.** **Ki Control** is the single most important skill — it's the foundation for **channeling ki**, so most other abilities (charging, flight, and all [[Techniques|Techniques]]) won't work until you have it. Get it early.

> **Skills** are learned from **[[Masters|Structures]]** and leveled up with training points. Each master only teaches a specific set (see [[Skills|Skills]] for the full `skillOfferings` and costs). "Level" below refers to how many times a skill can be upgraded.

---

## Movement & utility

| Skill | What it does |
| :-- | :-- |
| **Jump** | Higher jumping (scales with level). |
| **Sprint** | Faster sprinting. |
| **Fly** | Ki flight (toggle with **F**). It has **two modes you can switch with `Alt + F`**: **Exploration flight** for fast, free travel, and **Combat flight** — a momentum-based hover tuned for fighting. See [[Controls|Controls]]. |
| **Ki Sense** | Sense nearby entities and read their power level; enables lock-on and the ki-sense scan. |
| **Meditation** | Passive **+7.5% Ki & Stamina regen per level** (10 levels). |
| **Ki Control** | The foundation for channeling ki — required to charge and use techniques. |
| **Potential Unlock** | Unlocks your hidden potential for a permanent power boost; maxing it (with a master, requires good alignment) gates further advanced unlocks. |
| **Instant Transmission** | Teleport. Opens a menu to warp to **any master you've already met** and to **players**; player range scales **+200 blocks per level**. (Default key: **H**.) |
| **Fusion** | Fuse with another player into a single stronger character. |

## Passive combat abilities

These are the "abilities" that quietly modify your combat (bought and leveled like skills):

| Ability | Effect |
| :-- | :-- |
| **Ki Boost** | **+25% ki regen per level** while actively charging ki (4 levels). |
| **Ki Manipulation** | Increases ki-blast damage (**+5% per level**); at level 5+ enables ki weapons and shadow-dummy sparring. |
| **Ki Infusion** | Infuses your melee strikes with extra ki damage. |
| **Ki Protection** | Reduces the ki damage you take. |
| **Defense Penetration** | Ignores **2.5% of enemy defense per level**. |
| **Healing Reduction** | Your hits reduce the target's healing by **2% per level**. |

## Offensive techniques

Ki attacks (Kamehameha, Galick Gun…) and strike attacks (Dragon Fist, Meteor…) have their own page, with the full list and which master teaches each: **[[Techniques|Techniques]]**.

## Stack forms & transformations

**Kaioken** and **Ultimate** (stack forms), and the Super/God/Legendary/Android transformations, are covered in **[[Transformations & Mastery|Transformations-and-Mastery]]**.

---

# Racial passives (race abilities)

Every race has a **racial skill** — its signature ability. Some are **always-on passives**; others are **active** and are triggered from the racial-skill radial and by defeating valid targets. Full default values live in [[General Server|general-server]] (`racialSkills`).

## How each works, and its limit

| Race | Racial ability | How to use | Limit |
| :-- | :-- | :-- | :-- |
| **Human** | **Ki Regen Boost** (×1.40 ki regen) | Always-on passive. | — |
| **Frost Demon** | **Training Boost** (×1.25 TP gain) | Always-on passive. | — |
| **Saiyan** | **Zenkai** — permanent **+10% STR/SKP/PWR** and 20% heal | Triggers automatically when you **recover from near death**. 900s cooldown between gains. | **3 times** |
| **Namekian** | **Assimilation** — permanent **+15% STR/SKP/PWR** and 35% heal | Activate the racial skill, then **absorb another Namekian** (player or, optionally, Namek NPC). Namekians also regenerate. | **4 times** |
| **Majin** | **Absorption** — copies **4% of a target's stats** (+ **Revive** from a blob) | Activate, then **absorb** a target (works on mobs too). Revive is a separate 1-hour-cooldown self-revive. | **3 times** |
| **Bio-Android** | **Drain** — drain a living target (25%) to empower yourself | Activate and hit/finish a valid target. | Cooldown-based (180s) |

The **Zenkai/Assimilation/Absorption** permanent boosts share a **racial-skill counter** — once you hit the limit (3 or 4), you can't gain more.

## Resetting racial passives

Reached your cap, or want to re-earn your boosts? You can **reset the racial-skill counter** two ways:

- **Wish** — the **"Reset Racial Skill"** wish, available from **both Shenron and Porunga**, clears your accumulated racial boosts back to zero (see [[Dragons & Wishes|Dragons-and-Wishes]]).
- **Command** — `/dmzracialskill reset` (server/admin; see [[Commands|Commands]] and [[Permissions|Permissions]]).

After a reset your counter returns to 0 and you can build the boosts up again.

---

Related: [[Masters|Structures]] · [[Skills|Skills]] · [[Races|Races]] · [[Stats & Attributes|Stats-and-Attributes]] · [[Transformations & Mastery|Transformations-and-Mastery]] · [[Dragons & Wishes|Dragons-and-Wishes]]
