# Stats & Attributes

Your character is defined by **6 core attributes** you raise by training. Those attributes feed your **derived resources** (Health, Ki, Stamina), your **damage**, and your overall **Battle Power**. This page explains what each one does, plus stamina, poise, power release and alignment.

> The exact numbers depend on your **race** and **class** scalings — see [[Player Classes|Player-Classes]] and [[Races|Races]]. Values below describe *what a stat drives*, not fixed amounts.

## The 6 core attributes

| Stat | Key | What it governs |
| :-- | :-- | :-- |
| **Strength** | STR | **Melee (physical) damage.** |
| **Strike Power** | SKP | **Strike damage** — ki-enhanced melee/technique hits. |
| **Resistance** | RES | **Defense** (damage mitigation), plus your **max Stamina** and **max Poise**. |
| **Vitality** | VIT | **Max Health**, and your **Health & Stamina regen** rate. |
| **Ki Power** | PWR | **Ki blast / energy-wave damage.** |
| **Energy** | ENE | **Max Ki (energy pool).** |

Your three offensive stats stack: total offense = melee (STR) + strike (SKP) + ki (PWR). RES, VIT and ENE build your survivability and resource pools.

Each stat is raised with **training points / attribute points** (see [[Weights|Weights]] for faster training, and [[Commands|Commands]] — `/dmzstats`, `/dmzpoints`). A server-side cap can limit max stat value.

## Derived resources

- **Health (HP)** — scales from **VIT** (× your class VIT scaling and form multipliers).
- **Ki / Energy (EP)** — a pool of `20 + ENE × scaling`; used to cast ki skills and to power transformations.
- **Stamina (SP)** — a pool of `20 + RES × stamina scaling`; spent on sprinting, blocking, combat flight and melee actions (see [[Combat|Combat]]).
- **Poise** — `25 + defense`; poise resists being staggered/stunned in combat.

**Regeneration** (per second) is driven by your class's base "per-5" values plus your attributes:

- **HP regen** = `baseHp5 + VIT × hp5Scaling`, ÷5.
- **Ki regen** = `baseEp5 + ENE × ep5Scaling`, ÷5 — greatly increased while actively **charging ki**.
- **Stamina regen** = `baseSp5 + VIT × sp5Scaling`, ÷5 — reduced while an action is draining stamina.

Regen is further modified by armor recovery enchantments, the **Meditation** skill, potion effects, and active transformation drain.

## Battle Power

**Battle Power (BP)** is your overall "power level". It's computed from your four combat stats (**STR + SKP + RES + PWR**, with their scalings and bonuses) run through a scaling curve, then multiplied by your current **Power Release**. VIT and ENE don't directly raise BP — they build your pools instead.

## Power Release (Ki charge)

**Power Release** is a percentage (0–100%) representing how much of your power you're currently channeling. Charging ki raises it; it acts as a **multiplier** on your melee damage and Battle Power. Releasing more power costs energy upkeep.

## Alignment

**Alignment** is a **0–100** value (**default 100**) representing how good or evil your character is. It falls into three bands:

| Band | Range |
| :-- | :-- |
| **Good** | 61–100 |
| **Neutral** | 41–60 |
| **Evil** | 0–40 |

**What it affects — NPC disposition.** Your alignment decides which masters and NPCs will deal with you:

- **Good-aligned masters** (Goku, Gohan, King Kai, Old Kai, Roshi, Krillin) only teach you if your alignment is **≥ 61**.
- **Evil-aligned masters** (Cell, Frieza) only deal with you if your alignment is **≤ 40**.
- NPCs can also turn **hostile** or refuse interaction based on configurable alignment thresholds.

Alignment is managed with **`/dmzalignment set|add|remove`** (see [[Commands|Commands]] and [[Permissions|Permissions]]). See [[Masters|Structures]] for who teaches what.

---

Related: [[Player Classes|Player-Classes]] · [[Races|Races]] · [[Weights|Weights]] · [[Transformations & Mastery|Transformations-and-Mastery]] · [[Combat|Combat]] · [[Masters|Structures]]
