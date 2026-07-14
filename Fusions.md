# Fusions

DMZ lets **two players merge into a single, much stronger character** for a limited time. There are two ways to fuse — the **Fusion Dance (Metamoru)** and the **Potara Earrings (Pothala)** — and they work very differently. In both cases one player becomes the **leader** who controls the fused body, while the other rides along; the fusion has a duration, ends on death or if the partners get separated, and grants big bonus stats for its lifetime.

> Fusion timings are server-tunable in [[General Server|general-server]] (`gameplay`). Defaults: **15 min** duration, **30 min** cooldown (Metamoru), boosted stats are **STR / SKP / PWR**.

---

## Fusion Dance — Metamoru

The classic dance fusion. It uses the **Fusion** skill, costs nothing but timing, but has strict requirements.

**Requirements**

- Both players must have the **Fusion** [[skill|Abilities]] learned (its level sets how long the fusion lasts).
- **Both players must be the same race.**
- Neither player may be an **upgraded Android** (androids can't fuse).
- The two players' total stats must be **close enough** — if the gap is larger than the `metamoruFusionThreshold` (default **50%**), the dance fails with a "level gap" message.
- Neither player may already be fused, and neither can be on **Fusion cooldown**.

**How to do it**

1. Both players open the **Combat Radial** (**X**) and select the **Fusion** action.
2. Both players **hold the Action key (G)** to charge the dance. While charging you're locked in place and facing is frozen (the pose).
3. Stand **close together** (within ~5 blocks). When one player reaches a **full charge** while the partner is at least **half-charged and still charging**, the fusion triggers.
4. **The player who completes the charge first (pressed and filled `G` first) becomes the leader** and controls the fused body; the partner rides along as a passenger.

**Result**

- Bonus stats are added from the partner's STR/SKP/PWR, scaled **×1.25 → ×2.0** depending on how evenly matched the two were (closer stats = bigger multiplier).
- Duration scales with the **average Fusion skill level** of the two players (up to the server's full duration).
- When it ends — timer runs out, either player dies, disconnects, or the partner is pulled too far away — both split back apart and the **leader goes on Fusion cooldown** (default 30 min).

---

## Potara Earrings — Pothala

The earring fusion. It ignores race and stat-gap rules and is **stronger** than the dance, but it needs the **Potara earrings**, which come from a **Porunga wish** (see [[Dragons & Wishes|Dragons-and-Wishes]]).

**Getting the earrings**

- A Porunga wish grants a **full pair** (yellow or green). You receive them as a single **paired item**.
- **Right-click the pair item to split it** into a **left earring** and a **right earring** (both stamped with the same matching pair ID).
- Give one earring to each player. Each player equips their earring in the **head/tech accessory slot** (the [[Curios|Items]] slot).

**How to do it**

1. Both players wear their **matching earrings** (same pair — a left and a right with the same pair ID). Yellow pairs give a yellow fusion aura, green pairs give a green one.
2. Simply **get near each other** (within ~20 blocks). The earrings automatically pull the two players together into the Potara pose — no key press needed.
3. When they meet, they fuse. **The player wearing the *right* earring is the leader** and controls the fused body.

**Result**

- Bonus stats scale **×1.75 → ×2.5** — noticeably stronger than the dance.
- The fusion lasts the server duration; the **earrings take durability damage** each fusion, and when the fusion ends the earrings **break**.
- Like Metamoru, it ends on death, disconnect, or separation, and the partners split back apart.

---

## Quick comparison

| | **Fusion Dance (Metamoru)** | **Potara (Pothala)** |
| :-- | :-- | :-- |
| Needs | **Fusion skill** on both players | A **pair of Potara earrings** (Porunga wish) |
| Same race required? | **Yes** | No |
| Stat gap limit | **Yes** (default 50%) | No |
| Androids | Upgraded androids **can't** fuse | Upgraded androids **can't** fuse |
| How to start | Both select Fusion in the radial and **hold G** close together | Both **wear matching earrings** and get near each other |
| Who leads (controls body) | Player who **fills the G charge first** | Player wearing the **right earring** |
| Stat multiplier | ×1.25 → ×2.0 (STR/SKP/PWR) | ×1.75 → ×2.5 (STR/SKP/PWR) |
| After it ends | Leader gets a **Fusion cooldown** | Earrings **break** |

> Both fusions end early if either player dies, logs out, or the partner is dragged too far from the leader.

---

Related: [[Skills & Abilities|Abilities]] · [[Dragons & Wishes|Dragons-and-Wishes]] · [[Controls|Controls]] · [[Races|Races]] · [[General Server|general-server]]
