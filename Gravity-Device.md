# Gravity Device

The **Gravity Device** (gravity machine) is a block that raises gravity inside a sealed room, turning it into a personal training chamber. Combined with [[Weights|Weights]], it's the fastest way to grind stats, Training Points and mastery outside the [[Hyperbolic Time Chamber|Dimension-Hyperbolic-Time-Chamber]].

> This page covers **how to use** the machine. For the full gravity mechanics and config values, see the [[Gravity System|Gravity-System]] page.

## How it works

1. **Build a sealed room.** The device needs an enclosed room between **5×5×5 and 25×25×25** blocks by default (server owners can change these limits in [[General Server|General-Server]]). It scans the space and only works if the room is properly closed off (`Room valid` in its menu).
2. **Place the Gravity Device** inside the room.
3. **Power it with Star Energy.** Like the [[Kikono Station|Armors]], the device runs on **Star Energy** — hook it up to a **Fuel Generator** (which burns furnace fuel) through **Energy Cables**. Its buffer holds up to **20,000 energy**.
4. **Set the target gravity** in the device's menu — anywhere from **1× up to 1000×**.
5. While powered and the room is valid, **everyone inside the room feels that gravity.**

**Energy cost:** it burns roughly **1 energy per second for each × of gravity** (so 100× gravity ≈ 100 energy/second). Higher gravity drains the buffer faster, so keep fuel flowing.

## Building the room

The device checks the room by **flood-filling outward** from itself in every direction, treating a spot as "inside the room" only if it's **completely empty air** (no collision at all). This means:

- **Any solid block seals the room** — full blocks (stone, wood, glass, etc.) all work as walls, floor and ceiling.
- **Half-blocks don't seal it.** Slabs, stairs, fences, carpets, and anything else with a partial hitbox still count as "open" to the scan — even a single slab gap will make the room invalid (the gravity "leaks out" and the check fails because it can't stay inside the max size).
- **Doors and trapdoors always seal the room**, whether they're open or closed — you can use them as an entrance without breaking validity.
- The room's outer size (walls included) must be **between 5×5×5 and 25×25×25** blocks by default. If the sealed space is smaller or bigger than that — or if there's any gap letting the scan escape past the max size — the menu shows **"Invalid room (5-25 blocks each side)"** and the device won't apply gravity.
- The device itself doesn't need to be centered or on the floor — just place it anywhere inside the sealed space.

**Quick checklist:**
1. Build a fully enclosed box using only full blocks (or doors/trapdoors for the entrance) — no slabs, stairs, or other partial blocks anywhere in the walls, floor or ceiling.
2. Keep the inside dimensions within the 5–25 block range on all three axes.
3. Place the Gravity Device anywhere inside, power it, and open its menu — it should show `Room valid`.

### Example:


## Using it with Weights

Gravity and [[Weights|Weights]] multiply each other — this is the key combo:

- Your **effective weight = worn weight × gravity**. So training weights that barely register at normal gravity are pushed into the **ideal / heavy training zone** under a gravity machine, giving the big **×2–2.5 TP** multiplier.
- Gravity also adds a **flat bonus**: about **+2.5% TP and +0.25% mastery per × of gravity**.

The trade-off: high gravity also **weakens and slows you** (movement, jumping and flight are penalized, and overloading yourself with too much weight cuts your stats). So find a gravity + weight combo that keeps your load in the **ideal zone** for safe fast training, or push into the heavy zone for max gains while you accept being weaker.

**Recommended loop:** build a sealed room → power the device → equip weights → set a gravity you can handle → train (fight, spar, or grind) inside. See [[Training Points & Mastery|Training-Points-and-Mastery]] for everything that grants TP and mastery.

> There's also a portable **Gravity Device item** for gravity on the go (see [[Items|Items]]).

---

Related: [[Weights|Weights]] · [[Gravity System|Gravity-System]] · [[Training Points & Mastery|Training-Points-and-Mastery]] · [[Hyperbolic Time Chamber|Dimension-Hyperbolic-Time-Chamber]] · [[Armors|Armors]] · [[Items|Items]]
