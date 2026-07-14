# Training Points & Mastery

The two things you grind in DMZ: **Training Points (TP)** — the currency for skills, techniques and transformations — and **Mastery** — how well you control each transformation. This page lists **every way** to earn both.

---

## Training Points (TP)

TP is stored on your character and spent on [[Skills & Abilities|Abilities]], [[Techniques|Techniques]] and [[Transformations & Mastery|Transformations-and-Mastery]]. Every source below is run through your **TP boosts** (see [Boosts](#tp-boosts)) before being added.

### Ways to earn TP

| Source | How |
| :-- | :-- |
| **Defeating enemies** | Killing a mob or player grants TP based on the target's **Battle Power** (higher BP = more TP). |
| **Hitting enemies** | You gain a small amount of TP **per hit** landed in combat (`tpPerHit`). |
| **Passive training** | A trickle of TP just for playing (per-tick passive gain). |
| **Traveling** | Moving/travelling awards TP over distance. |
| **Mining** | Breaking blocks grants TP (varies by block). |
| **Crafting** | Crafting items grants TP (scales with amount). |
| **Story & side quests** | Quests reward TP (`STORY` source) — see [[Custom Quests, Sagas & Sidequests|Custom-Quests-Sagas-and-Sidequests]]. |
| **Punch machine / training gear** | Training rewards (e.g. the punch machine) grant TP. |
| **Wishes** | A dragon wish can grant a lump sum of TP — see [[Dragons & Wishes|Dragons-and-Wishes]]. |
| **Party sharing** | TP can be **shared among party members** (see [[Commands|Commands]] — `/dmzparty`). |

### Training multipliers (train faster)

Passive/travel training is multiplied when you train in special conditions:

- **Gravity Device** — higher gravity = more TP (see [[Gravity System|Gravity-System]]).
- **Hyperbolic Time Chamber** — Bulma's gravity-room upgrades boost training inside it (see [[Hyperbolic Time Chamber|Dimension-Hyperbolic-Time-Chamber]]).
- **Weights** — training with weights improves gains (see [[Weights|Weights]]).

### TP boosts

The amount from every source is scaled by your active boosts (they stack):

- **Class** modifier (e.g. Tank/Cleric train faster — see [[Player Classes|Player-Classes]]).
- **Racial skill** — **Frost Demon** gets ×1.25 TP by default (see [[Races|Races]]).
- **`tp_gain` effect** and consumables (see [[Effects|Effects]]).

TP gain rates and per-hit/kill values are all tunable in [[General Server|general-server]] (`gameplay`).

---

## Mastery

**Mastery** is earned **per transformation**, only while that form is active. You raise a form's mastery by:

| Source | Default gain |
| :-- | :-- |
| **Dealing hits while transformed** | +0.04% per hit (scaled by damage). |
| **Taking hits while transformed** | +0.04% per hit (scaled by damage). |
| **Just staying transformed** | +0.006% every 5 seconds (passive). |
| **Quest rewards** | Some quests grant transformation mastery directly. |

So the fastest way to master a form is to **fight in it** — trade blows while transformed rather than sitting idle.

### Best way to farm mastery

Passive gain (+0.006% every 5s) is tiny — you'd wait hours just standing around. Combat is **~7× per hit** and both *dealing* and *taking* hits count, so the goal is to be in a transformation and exchanging as many hits as possible:

1. **Transform first, then fight.** Mastery only ticks up while that specific form is active, so enter the form *before* the fight, not after.
2. **Pick a punching bag that hits back.** Because taking hits also grants mastery, the ideal target is something that survives a while and keeps swinging at you — a tanky mob you don't one-shot, or a sparring partner. Trading blows fills the bar from both directions at once.
3. **Don't out-level your target.** If you delete enemies in one hit you get one hit's worth of mastery and then have to find another. A fight that lasts many exchanges is far better than many one-shots.
4. **Stack the multipliers below** — a `mastery_gain` effect on top of the ×1.50 global multiplier roughly doubles everything above.
5. **Exploit shared mastery.** If a form is configured to share mastery with related forms, grinding the one you *can* keep active also raises the others for free.
6. **Top it up with quests.** A few quests grant transformation mastery directly (see the table above) — cheap chunks that don't require a fight.

> In short: put on the form, find something that trades hits with you for a long time, keep a `mastery_gain` buff running, and avoid one-shotting.

### Mastery multipliers

- **Global mastery multiplier** — ×1.50 by default (server-tunable in [[General Server|general-server]]).
- **`mastery_gain` effect** — a buff that further multiplies mastery gain (see [[Effects|Effects]]).
- **Shared mastery** — some forms are configured to share mastery with related forms, so training one also raises the others.

### Resetting

Mastery is tied to your forms; racial passive boosts (Zenkai, etc.) are separate and can be reset with a wish or `/dmzracialskill` — see [[Skills & Abilities|Abilities]].

---

What mastery **unlocks** for each form (25% → next form, 40% → instant transform, 50% → free transform, 100% → max bonus) is covered in **[[Transformations & Mastery|Transformations-and-Mastery]]**.

---

Related: [[Transformations & Mastery|Transformations-and-Mastery]] · [[Weights|Weights]] · [[Gravity System|Gravity-System]] · [[Hyperbolic Time Chamber|Dimension-Hyperbolic-Time-Chamber]] · [[Stats & Attributes|Stats-and-Attributes]] · [[Commands|Commands]]
