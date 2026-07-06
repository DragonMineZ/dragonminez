# Special Weapons

DMZ ships its **own melee combat system** — similar in spirit to *Better Combat* — where every weapon has a hand-crafted **moveset** instead of a single generic swing. This page explains that system, then lists the signature weapons that use it.

## The combat system

Each weapon is bound to a **moveset** (defined in `weapon_attributes`). A moveset is a **combo chain**: a sequence of attacks you cycle through as you keep swinging, each with its own **reach, sweep angle, timing (wind-up) and damage**.

Key ideas:

- **Combos** — consecutive attacks step through the moveset (attack 1 → 2 → 3 → …) and loop. Different weapon classes swing, sweep and reach very differently.
- **Dual-wielding** — hold a weapon in each hand and your combo **alternates hands** automatically.
- **Reach** — each moveset has its own range (a staff out-reaches a sword), on top of your entity-reach attribute.
- **Stamina** — swinging, blocking and dashing all cost **Stamina** (see [[Stats & Attributes|Stats-and-Attributes]]).
- **Blocking & parrying** — you can block (up to ~65% damage reduction) and **parry** within a tight window (~150 ms); a broken block leaves you stunned.
- **Dashing** — dash and double-dash (with cooldowns) for mobility in a fight.

DMZ ships **~27 base movesets** — sword, katana, claymore, staff, spear, dagger, rapier, scythe, hammer, glaive, halberd, lance, mace, trident, twin-blade, fist, claw and more. Tuning lives in [[Combat|Combat]] (`combat.json`); creating/assigning movesets is covered in [[Weapon Attributes|Weapon-Attributes]].

## Signature weapons

Each of these plays differently because it inherits a different base moveset (and its own crit stats):

| Weapon | Moveset | Feel | Crit chance / dmg |
| :-- | :-- | :-- | :-- |
| **Yajirobe's Katana** | Katana (reach 2.75, fast 4-hit combo) | Quick, aggressive slashes | 25% / 10% |
| **Dimensional Sword** | Katana (reach 2.75) | Fast combo with high crit chance | 25% / 10% |
| **Brave Sword** | Sword (reach 2.5, balanced) | All-rounder | 10% / 15% |
| **Z Sword** | Claymore (heavy, wide slow swings) | Big, hard-hitting blows | 15% / 25% |
| **Power Pole** (Nyoibo) | Staff (reach 3 — the longest) | Long-range poking, keeps enemies at bay | 15% / 10% |

So the same fight plays out very differently depending on the blade: the **Katana** weapons chain quick combos, the **Z Sword** trades speed for heavy crits, and the **Power Pole** wins the spacing game with reach.

> The **Brave Sword** and **Z Sword** are dragon wishes, and the Power Pole/Nyoibo is a Shenron wish — see [[Dragons & Wishes|Dragons-and-Wishes]].

---

Related: [[Combat|Combat]] · [[Weapon Attributes|Weapon-Attributes]] · [[Items|Items]] · [[Stats & Attributes|Stats-and-Attributes]] · [[Dragons & Wishes|Dragons-and-Wishes]]
