# Transformations & Mastery

Every race in DMZ has **transformations**, organized into **form groups**. Each transformation multiplies your stats but usually drains a resource (ki, stamina or health) while active, and you build **mastery** in a form by using it.

## How forms work

- Every form belongs to a **group** (e.g. a race's Super forms, its Legendary forms, Saiyan's Super-Saiyan grades…).
- To unlock the **next** form in a group you need **both**:
  1. Its **Training Point (TP)** cost.
  2. **25% mastery** on the **previous** form in that group.
- While transformed you pay an upkeep drain (shown below). **Mastery lowers this cost** and eventually makes transforming free — see [Mastery](#mastery).

> To **earn** TP and mastery, see the dedicated page: **[[Training Points & Mastery|Training-Points-and-Mastery]]**.
>
> All values are **defaults** — TP costs live in `character.json` and per-form drain/mastery in each form's config. See [[Custom Forms|Custom-Forms]].

The **Ki drain** column below is energy drained per tick while the form is active. "—" = no ki upkeep. A **negative** value means the form *restores* energy.

---

## Human
Super-form TP ladder: **21,000 · 42,000 · 65,000 · 104,000**

| Group | Forms (in order) | Ki drain |
| :-- | :-- | :-- |
| **Super forms** | Buffed | 0.08 |
| | Full Power | 0.16 |
| | Overdrive | 0.34 |
| | Solaris | 0.22 |
| **Android forms** | Android Base → Super Android → Fused Android | — *(Fused Android: ×2.5 stamina drain)* |
| **Legendary forms** | Shiyoken · Shin Shiyoken · Chou Shiyoken | 0.22 · 0.28 · 0.34 *(locked)* |

> **Androids are a permanent sub-race of Humans**, which is why they get their own **Android forms** group. Once a human becomes an Android the change **can't be undone**: they can **only** use the Android forms and lose access to the Super and Legendary lines above. See [[Races|Races]].

## Saiyan
Super-form TP ladder: **13,000 · 21,000 · 31,000 · 42,000 · 52,000 · 65,000 · 78,000 · 104,000**

| Group | Forms (in order) | Ki drain |
| :-- | :-- | :-- |
| **Oozaru** | Oozaru · Golden Oozaru · Super Saiyan 4 | 0.05 · 0.24 · 0.24 |
| **SS Grades** | Super Saiyan · Grade 2 · Grade 3 | 0.08 · 0.12 · 0.48 |
| **Super Saiyan** | Super Saiyan (Mastered) · SSJ2 · SSJ3 · SSJ4 | 0.03 · 0.16 · 0.34 · 0.24 |
| **Legendary forms** | Ikari · SSJ Hybrid · SSJ Full Power | 0.10 · 0.16 · 0.26 *(locked)* |

## Namekian
Super-form TP ladder: **23,000 · 47,000 · 78,000 · 117,000**

| Group | Forms (in order) | Ki drain |
| :-- | :-- | :-- |
| **Super forms** | Giant · Full Power · Super Namekian | 0.09 · 0.18 · 0.27 |
| **Legendary forms** | Evil Namek · Evil Giant Namek · Buffed Namek | 0.18 · 0.25 · 0.24 *(locked)* |

## Frost Demon
Evolution TP ladder: **18,000 · 31,000 · 52,000 · 83,000 · 117,000**

| Group | Forms (in order) | Ki drain |
| :-- | :-- | :-- |
| **Evolution forms** | Second · Third · Final | — · — · — |
| | Full Power | 0.22 |
| | Fifth Form | 0.28 |
| **Legendary forms** | Mecha · Metal · Metal Core | — · −0.05 · −0.10 *(Metal forms restore energy; locked)* |

## Majin
Pure-form TP ladder: **23,000 · 47,000 · 78,000 · 114,000**

| Group | Forms (in order) | Ki drain |
| :-- | :-- | :-- |
| **Pure forms** | Kid · Evil · Super | — · — · — |
| | Ultra | 0.22 |
| **Legendary forms** | Innocence Demon · Giant Innocence Demon · Super Demon | — · 0.25 · — *(locked)* |

## Bio-Android
Bio-Evolution TP ladder: **26,000 · 57,000 · 88,000 · 125,000**

| Group | Forms (in order) | Ki drain |
| :-- | :-- | :-- |
| **Bio-Evolution** | Semi-Perfect · Perfect | — · — |
| | Super Perfect | 0.16 |
| | Ultra Perfect | 0.28 |
| **Legendary forms** | Xeno · Xeno Full Power · Xeno Max | 0.06 · 0.16 · 0.22 *(locked)* |

---

## Energy consumption while transformed

The "Ki drain" numbers in the tables above are **base** values. What you actually pay each tick is scaled by several things, so the real cost is usually **higher** than the table figure — and it grows with your own power:

- **Which resource** — most forms drain **Ki (energy)**; a few drain **stamina** or **health** instead (e.g. Kaioken drains health). A **negative** value *restores* the resource (Metal Frost-Demon forms).
- **Power Release** — the drain scales directly with your current **power release** (the % you charge with **C**). Fighting at 100% release costs the most; powering down lowers upkeep.
- **Your offensive power** — the stronger your STR/SKP/PWR relative to your max Ki, the more the form costs to hold (a square-root scaling). A percentage of your **max Ki** is also added on top, so bigger pools pay bigger upkeep.
- **Mastery** — this is the discount. As a form's mastery climbs, its cost multiplier drops toward **×0.75** at 100% mastery (see [Mastery](#mastery)). A well-mastered form is much cheaper to sustain.
- **Stacking** — layering a stack form (Kaioken, etc.) multiplies the drain of **both** the base form and the stack (each form's `stackDrainMultiplier`, ×2.0 by default), which is why stacked transformations burn resources fast.
- **Server baseline** — a global `baselineFormDrain` (combat config) and equipped-[[Weights|Weights]] load further scale everything.

**Running out drops you back to base.** Each tick the game checks you can afford the drain; if you can't pay the Ki (or stamina/health), the form — and any stack form — is **forcibly deactivated** and you revert to base with a *"drained"* message. So a form you can't sustain will keep collapsing until you either raise its mastery, power down, or grow your Ki pool.

> Practical takeaway: **mastery + a large Ki pool (ENE) are what let you stay transformed.** Grinding a form's mastery both raises its stat bonus *and* cuts its upkeep, and investing in Energy raises the pool the drain is measured against.

---

## Stackable forms

Stack forms layer **on top of** your normal transformation for an extra multiplier at a heavier upkeep. They're skills learned from a master, not race forms:

| Stack form | Tiers | Drain |
| :-- | :-- | :-- |
| **Kaioken** (from King Kai) | x2 · x3 · x4 · x10 · x20 | **Health** drain: 0.03 · 0.06 · 0.095 · 0.11 · 0.15 |
| **Ultimate** | 1 tier | none *(locked by default)* |

TP per Kaioken tier: 1,000 · 1,500 · 2,500 · 4,000 · 7,500.

---

## About Legendary Forms

Every race has a 3-tier **Legendary form** line, but their default TP cost is `-1` (**not purchasable**). They're obtained through the **Mutant "legendary form holder" lottery** (see the `mutant` section in [[General Server|general-server]]) — unless a server owner sets real costs. See [[Races|Races]].

---

## Mastery

**Mastery** (0–100% per form) measures how well you control a transformation. As it climbs it unlocks perks (defaults):

| Mastery | Unlocks |
| :-- | :-- |
| **25%** | Lets you unlock the **next form** in the group. |
| **40%** | **Instant transformation** (no charge-up). |
| **50%** | **Free transformation** (no activation cost). |
| **100%** | Full mastery: up to **×1.5 stats** and drain reduced to **×0.75**. |

Some forms **share mastery** with related forms. Check/adjust it with **`/dmzmastery`** (see [[Commands|Commands]]).

**How you actually earn mastery (and TP) is on its own page → [[Training Points & Mastery|Training-Points-and-Mastery]].**

---

Related: [[Training Points & Mastery|Training-Points-and-Mastery]] · [[Races|Races]] · [[Skills & Abilities|Abilities]] · [[Custom Forms|Custom-Forms]] · [[Commands|Commands]]
