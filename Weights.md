# Weights

Weights are wearable training gear. Wearing them while you train **multiplies the Training Points (TP) you earn** — but overloading yourself makes you weaker. Getting the balance right is the whole point.

> Weights work hand-in-hand with gravity — see [[Gravity System|Gravity-System]]. All numbers here are **defaults** and are tunable in [[General Server|general-server]] (`gravity`).

## Weight items

Weights are **Curios** worn in the dedicated *Weights* slot. There are three, each holding a configurable **weight value** (kg):

| Item | Notes |
| :-- | :-- |
| **Turtle Shell** (`weight_turtle_shell`) | Turtle School weighted shell. |
| **Workout Weights** (`workout_weights`) | Standard training weights. |
| **Piccolo Cape** (`weight_piccolo_cape`) | Weighted cape. |

You obtain/upgrade them from the **weight service** offered by masters **Roshi** and **King Kai** (see [[Masters|Structures]]), or an admin can hand them out with **`/dmzweight <type> <amount>`** (see [[Commands|Commands]]).

## How training with weights works

The bonus isn't just "more weight = more TP". The game compares how much weight you carry to how much your character *should* be able to handle:

- **Effective weight** = your total worn weight **× gravity multiplier**. Training under higher gravity makes the same weight count for much more (see [[Gravity System|Gravity-System]]).
- **Ideal weight** = scales with your power level (roughly your relative level ÷ 2).
- **Load ratio** = **effective weight ÷ ideal weight**. This ratio decides your TP multiplier.

### The load-ratio zones (defaults)

| Load ratio | TP multiplier | Stat penalty |
| :-- | :-- | :-- |
| 0 (no weights) | ×1.0 | none |
| up to 0.5 | 1.0 → 1.5 | none |
| 0.5 – 0.75 | 1.5 → 2.0 | none |
| **0.75 – 1.25 (ideal zone)** | **×2.0** | **none** |
| 1.25 – 2.0 (heavy) | 2.0 → **2.5** | rising |
| 2.0 – 2.5 (overload) | 2.5 → 1.0 | rising to max |
| 2.5 and above | ×1.0 | **×0.6 (−60% stats)** |

**What this means:**

- The **ideal zone (ratio 0.75–1.25)** gives a safe **×2.0 TP** with **no penalty** — the recommended place to sit.
- Pushing into the **heavy zone** can reach up to **×2.5 TP**, but you start taking a **stat penalty** (up to −60% of your stats), so you fight much weaker while overloaded.
- Going **too heavy (ratio ≥ 2.5)** is the worst of both: the TP bonus collapses back to ×1.0 **and** you keep the full penalty.

Because *ideal weight grows with your level*, weights that once overloaded you become the ideal zone as you get stronger — so you keep adding weight over time.

### Gravity synergy

Training under gravity stacks with weights:

- Gravity **amplifies effective weight**, so you can hit the ideal/heavy zones with less physical weight.
- Gravity also grants a **flat TP bonus** (+2.5% per gravity by default) and a small **mastery bonus** (+0.25% per gravity).
- Heavier load also increases your **stamina/energy drain** while worn.

So the strongest training setup is **weights + a Gravity Device / the Hyperbolic Time Chamber**, kept in (or just into) the ideal zone.

---

Related: [[Training Points & Mastery|Training-Points-and-Mastery]] · [[Gravity System|Gravity-System]] · [[Hyperbolic Time Chamber|Dimension-Hyperbolic-Time-Chamber]] · [[Stats & Attributes|Stats-and-Attributes]] · [[Masters|Structures]] · [[Commands|Commands]]
