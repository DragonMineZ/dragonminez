# Player Classes

When you create a character you pick a **class**. A class does three things:

1. **Starting attributes** — how your first stat points are distributed.
2. **Stat scalings** — how efficiently each attribute contributes as you train (this is what defines the class's playstyle).
3. **A class passive** — a unique combat/utility effect.

DMZ ships **7 classes**: Warrior, Martial Artist, Spiritualist, Berserker, Paladin, Tank and Cleric.

> **These are the default values.** Everything below (starting stats, scalings, regen, TP multipliers and the passive tuning values) lives per-race in the race stats config (`config/dragonminez/races/<race>/stats.json`) and can be changed by server owners/addons. Class ids used in config: `warrior`, `martialartist`, `spiritualist`, `berserker`, `paladin`, `tank`, `cleric`.

**Stat keys:** STR = Strength · SKP = Strike Power · RES = Resistance · VIT = Vitality · PWR = Ki Power · ENE = Energy · (scalings also include DEF = Defense and STM = Stamina). Regen values are per 5-tick "ticks": e.g. `baseHp5 + VIT × hp5Scaling`.

---

## Warrior (`warrior`)

The melee bruiser — balanced physical stats and the best sustained stamina in the game.

**Passive — Combo Stacks.** Landing **3 consecutive unblocked melee hits** grants **1 stack** (max **5**). Each stack gives **+10% stamina regen**, and at **max stacks** you gain **+10% armor penetration**. A blocked hit resets the combo; combo resets after 3s of no hits (60 ticks) and stacks fade after 5s (100 ticks).

- Starting stats: STR 10 · RES 5 · VIT 5
- Regen: HP 1.75 (+0.06/VIT) · EP 4.0 (+0.08/ENE) · SP 12.0 (+0.12/STM)

## Martial Artist (`martialartist`)

The technical striker — hits harder the more hurt the enemy is.

**Passive — Executioner Strikes.** Melee/strike damage scales up as the **target's** HP drops: from full effect boundaries **+0% at 75% target HP up to +25% at 25% target HP** (`maxBonus 0.25`, `hpHigh 0.75`, `hpLow 0.25`). Great for finishing weakened foes.

- Starting stats: SKP 10 · VIT 10
- Regen: HP 1.5 (+0.0525/VIT) · EP 4.0 (+0.08/ENE) · SP 9.0 (+0.09/STM)

## Spiritualist (`spiritualist`)

The ki caster — enormous energy scaling, weak body.

**Passive — Ki Mastery.** Reduces the cooldown of ki **damage** skills: **−20%** on pure damage skills (`cdPrimary`) and **−15%** on damage+debuff skills (`cdSecondary`), which also get **+25% secondary-effect duration** (`durationBonus`).

- Starting stats: PWR 10 · ENE 10
- Regen: HP 0.5 (+0.015/VIT) · EP 8.0 (+0.20/ENE) · SP 5.0 (+0.05/STM)

## Berserker (`berserker`)

The glass cannon — the closer to death, the stronger.

**Passive — Bloodlust.** Scales with **your** missing HP:
- Below **66%** HP → **+25% HP regen** and **+10% crit chance**.
- Below **33%** HP → **+75% HP regen** and **+25% crit chance**.

- Starting stats: STR 10 · VIT 10
- Regen: HP 1.0 (+0.0375/VIT) · EP 2.0 (+0.04/ENE) · SP 14.0 (+0.13/STM)

## Paladin (`paladin`)

The protector — high defense scaling, built to guard allies.

**Passive — Guardian.** Redirects **15%** of nearby allies' incoming damage to itself (`redirectPct`) and gains **15% lifesteal** (`lifestealPct`).

- Starting stats: SKP 5 · RES 10 · VIT 5
- Regen: HP 2.0 (+0.0675/VIT) · EP 4.0 (+0.08/ENE) · SP 8.0 (+0.08/STM)

## Tank (`tank`)

The wall — best defense/vitality scaling, sustains through stamina.

**Passive — Fortress.** Converts **50%** of stamina regen into bonus **HP regen** (`stmToHpRegenRatio`), and grants **+25% healing received** (`healingBonus`). Below **30%** HP, both effects are **doubled** (`lowHpMultiplier 2.0`).

- Starting stats: RES 10 · VIT 10
- Regen: HP 2.25 (+0.075/VIT) · EP 5.0 (+0.10/ENE) · SP 9.0 (+0.09/STM)
- **TP gain ×1.25** (trains faster than average).

## Cleric (`cleric`)

The support caster — huge energy pool and utility uptime.

**Passive — Blessing.** Same cooldown/duration bonuses as the Spiritualist: **−20%/−15%** ki damage-skill cooldowns and **+25%** debuff duration.

- Starting stats: RES 5 · ENE 15
- Regen: HP 0.5 (+0.015/VIT) · EP 12.0 (+0.24/ENE) · SP 16.0 (+0.12/STM)
- **TP gain ×1.25** and **TP cost ×0.9** (cheapest, fastest to train).

---

## Default stat scalings (comparison)

Higher = that attribute contributes more for this class. Bold marks each class's standout stat(s).

| Class | STR | SKP | DEF | STM | VIT | PWR | ENE |
| :-- | :--: | :--: | :--: | :--: | :--: | :--: | :--: |
| Warrior | 1.4 | 1.0 | 0.24 | **1.6** | 1.8 | 0.5 | 1.5 |
| Martial Artist | 0.8 | **1.8** | 0.18 | 1.3 | **2.2** | 0.6 | 1.6 |
| Spiritualist | 0.3 | 0.5 | 0.156 | 0.7 | 1.4 | 1.9 | **3.7** |
| Berserker | **1.7** | 0.8 | 0.18 | 1.1 | **3.0** | 0.4 | 1.3 |
| Paladin | 0.8 | 1.2 | **0.336** | 1.2 | 2.0 | 0.6 | 1.2 |
| Tank | 0.6 | 0.7 | **0.384** | 1.5 | **2.5** | 0.5 | 0.8 |
| Cleric | 0.5 | 0.5 | 0.168 | **2.6** | 1.2 | 0.8 | **3.0** |

---

Related: [[Skills & Abilities|Abilities]] · [[Races|Races]] · [[Weights|Weights]] · [[Transformations & Mastery|Transformations-and-Mastery]]
