# Racial Skills

Every race has a **racial skill**: a set of signature passives and, for most races, an **active ability**. This page explains what each race gets, how to use the active parts, their limits, how to reset them, and how server owners can tune them.

> The numbers on this page are the **defaults**. Everything is configurable in the `racialSkills` block of `general-server.json` (see [Configuration](#configuration) below). The in-game **Skills menu** (and the race-selection screen) always shows your server's live values.

## Using an active racial skill

1. Open the **action menu** (default **X**) and select **Racial Skill**.
2. Hold or press the **action key** (default **G**) — what it does depends on your race (see below).

While Namekian Assimilation or Majin Absorption is the selected action, your **finishing blows knock enemies down instead of killing them**, so you can absorb them.

---

## Human

| Part | What it does |
| :-- | :-- |
| **Blood-Fueled Ki** | Ki attacks cost **25% less Ki**. Part of the saved Ki is paid with your health instead (**12.5%** of the original cost), and that health is added as **bonus damage** to the attack's next hit. It never brings you below 2 health. |
| **Fast Learner** | Your techniques gain **35%** more experience. |
| **Adrenaline** | Dropping to **25%** health or below fills you with adrenaline for **15 s**: you take **30%** less damage and gain **+25%** attack speed and **+25%** movement speed. It can trigger again after **180 s**, once your health has risen back above the threshold. |

### Androids (Humans upgraded by Dr. Gero)

An upgraded Android loses Blood-Fueled Ki and gets these instead (Fast Learner and Adrenaline stay):

| Part | What it does |
| :-- | :-- |
| **Android Core** | Ki attacks cost **50%** less Ki with no health cost, and your Ki regeneration is multiplied by **2**, but your Ki attacks deal **15%** less damage. |
| **Absorption Barrier** | Select the racial skill in the X menu and **hold G** to raise a barrier for up to **10 s**. Ki blasts that hit it deal no damage and restore **50%** of their power as Ki; other projectiles are blocked. If the absorbed Ki overflows your max Ki by **50%**, you enter a **Ki Surge** (see [[Combat|Combat]]); past **75%** the barrier shatters and the excess backfires on you. Cooldown: **30 s** (**180 s** if shattered). |

## Saiyan

| Part | What it does |
| :-- | :-- |
| **Zenkai** | From **level 100**, a lethal blow from an enemy leaves you at 1 health instead — unless it dealt more than **100%** of your max health. You stay down for **10 s**, invulnerable, recovering **40%** health, **80%** Ki and **80%** stamina. Your **STR, SKP and PWR** rise by **1% to 10%**, depending on the hardest hit you took in the last **10 s**. Hits of at least **25%** of your max health grant the full boost, which is **permanent** (up to **3** times); any other boost is temporary and lasts **120–240 s**. Cooldown: **900 s**. |
| **Limitless Power** | Each full-strength (permanent) Zenkai raises your **Power Release** limit by **5%**, up to **+15%** in total. |

## Namekian

| Part | What it does |
| :-- | :-- |
| **Assimilation** | Select the racial skill in the X menu and **hold G for 4 s** while aiming at another Namekian (or a Namekian warrior, trader or Piccolo) within **3 blocks**. Players must accept the request (clickable **[Accept] / [Reject]** in chat) unless they are knocked down. The target is absorbed: you heal **35%** of your max health and permanently gain **7.5%** of your own **STR, SKP and PWR**. Up to **4** assimilations, **600 s** cooldown. When you die you lose the **latest** one. |
| **Regeneration** | Use **Regeneration** from the X menu to recover **25%** of your max health over **3 s**, spending **30%** of your Ki and **20%** of your stamina. Taking damage interrupts it. Cooldown: **45 s** (half if interrupted). |
| **Water Affinity** | Being underwater or drinking any potion boosts your health, Ki and stamina regeneration by **25%** for **5 s**. |

## Frost Demon

| Part | What it does |
| :-- | :-- |
| **Prodigious Strength** | You gain **25%** more TP from every source, stacking with other boosts such as the [[Hyperbolic Time Chamber|Dimension-Hyperbolic-Time-Chamber]]. |
| **Evolutionary Mastery** | Your forms gain **25%** more mastery. |
| **Hardened Body** | You take **12.5%** less damage from Ki techniques and **5%** less from strike techniques. |
| **Power Reserve** | In base form or in your Second, Third or Final Form, with Power Release at **25%** or less, you store Ki in a reserve of up to **50%** of your max Ki (full in **30 s**). When you are transformed at **100%** Power Release, the reserve feeds your form for up to **10 s**: no Ki upkeep and **15%** stronger form bonuses. |

## Bio-Android

Your active racial skill **changes with your form** (Bio-Evolution):

| Form | Active racial skill |
| :-- | :-- |
| Base (Imperfect) | Vital Drain |
| Semi-Perfect | Vital Drain or Self-Destruct (pick one in the X menu) |
| Perfect (or any other transformation) | Summon Cell Jr. |

| Part | What it does |
| :-- | :-- |
| **Vital Drain** | Select the racial skill and **press G** next to a target within **3 blocks**: you appear behind it and both of you are stunned for **6 s** while you drain **5%** of its max health per second, healing that much and gaining five times as much Ki. Draining it down to 1 health kills it, granting TP in the Imperfect form. Cooldown: **180 s**. |
| **Self-Destruct** | Semi-Perfect form only. Select **Self-Destruct** in the X menu, **hold G for 5 s** and release it to explode: everything within **36 blocks** takes up to **500%** of your max health as damage (less the farther it is), and the terrain is torn apart. You survive at **1%** health, knocked down for **30 s** and unable to heal. Cooldown: **600 s**. |
| **Cell Jr.** | Perfect form only. **Hold G for 5 s** to summon a Cell Jr. with **20%** of your health and damage, up to **3** at a time. Each living Cell Jr. lowers your stats by **10%**, and the slot of a destroyed one recharges in **240 s**. Press **Alt + G** to dismiss them all. |

## Majin

| Part | What it does |
| :-- | :-- |
| **Absorption** | Select the racial skill in the X menu and **hold G for 4 s** while aiming at a weaker player or mob within **8 blocks**, or at a knocked-down player. The target is absorbed: you heal **30%** of your max health and permanently gain **4%** of its **STR, SKP and PWR** (mobs use their max health). Up to **3** absorptions, **600 s** cooldown. You can eject an absorption from the X menu (click the slot twice), which locks that slot for **300 s**. You lose **all** absorptions when you die. |
| **Sweet Tooth** | Food heals you **25%** more. |
| **Healing Magic** | Your healing Ki techniques heal **25%** more, and you receive **25%** more healing from them. Healing techniques also cost **10%** less Ki to fire and recharge **10%** faster. |

## Glind

| Part | What it does |
| :-- | :-- |
| **Divine Ki** | Ki Sense, Scouters and Instant Transmission cannot find you unless the seeker has unlocked a God Form or is under **Kami's Blessing** (see [[Effects|Effects]]). Lock-on still works. |
| **Celestial Body** | Your health, Ki and stamina regeneration are **25%** higher, and your Meditation dodge chance against melee attacks is **50%** higher. |
| **Divine Punishment** | Your attacks reduce the healing of players you hit by **20%** for 6 s. Your Healing Reduction skill and enchantment stack on top at **50%** effectiveness, up to **40%**. |
| **Light and Darkness** | Deal **10%** more damage to players of the opposite alignment (**15%** against Frost Demons, **5%** against Majins): with **25** alignment or less you punish players at **60** or more, and with **75** or more you punish players at **40** or less. Against any non-player foe you always deal **7.5%** more damage. |

---

## Resetting racial progress

The permanent racial gains — Zenkai boosts and the Power Release bonus, assimilations, absorptions and Cell Jrs. — can be reset:

- **Wish** — the **"Reset Passive"** wish, available from **both Shenron and Porunga** (see [[Dragons & Wishes|Dragons-and-Wishes]]).
- **Command** — `/dmzracial reset [targets]` (permission nodes `dmzracial.reset.self` / `dmzracial.reset.others`; see [[Commands|Commands]] and [[Permissions|Permissions]]).

After a reset you can earn them again from zero.

---

## Configuration

All racial tuning lives in `config/dragonminez/general-server.json` under `racialSkills`. Reload with `/dmzreload config` (see [[Reloading and updating changes|Reloading-and-updating-changes]]).

| Key | Default | What it does |
| :-- | :-- | :-- |
| `enableRacialSkills` | `true` | Master switch for every racial skill. |

Each race has its own section, and every section has an `enabled` switch (default `true`). Which section a race uses is set by `racialSkill` in its `races/<race>/character.json` (see [[Custom Races|Custom-Races]]).

### `human`

| Key | Default | What it does |
| :-- | :-- | :-- |
| `techniqueXpBonus` | `0.35` | Fast Learner technique XP bonus. |
| `adrenalineThreshold` | `0.25` | Health ratio that triggers Adrenaline. |
| `adrenalineSeconds` | `15` | Adrenaline duration. |
| `adrenalineDamageReduction` | `0.30` | Damage reduction during Adrenaline. |
| `adrenalineAttackSpeed` / `adrenalineMoveSpeed` | `0.25` / `0.25` | Attack / movement speed bonus during Adrenaline. |
| `adrenalineCooldownSeconds` | `180` | Adrenaline cooldown. |
| `androidBarrierEnabled` | `true` | Enables the Android Absorption Barrier. |
| `androidBarrierMaxSeconds` | `10` | Maximum barrier duration. |
| `androidBarrierCooldownSeconds` | `30` | Cooldown after a normal release. |
| `androidBarrierBrokenCooldownSeconds` | `180` | Cooldown after the barrier shatters. |
| `androidBarrierKiConversion` | `0.50` | Fraction of absorbed damage turned into Ki. |
| `androidBarrierSurgeOverflow` | `0.50` | Ki overflow (fraction of max Ki) that triggers a Ki Surge. |
| `androidBarrierBreakOverflow` | `0.75` | Ki overflow that shatters the barrier. |
| `androidBarrierSize` | `2.6` | Barrier size. |
| `androidKiRegenMultiplier` | `2.0` | Android Core Ki regeneration multiplier. |

### `saiyan`

| Key | Default | What it does |
| :-- | :-- | :-- |
| `minLevel` | `100` | Minimum character level for Zenkai. |
| `cooldownSeconds` | `900` | Zenkai cooldown. |
| `knockoutSeconds` | `10` | Time spent down after a Zenkai. |
| `knockoutHealthRegen` / `knockoutStaminaRegen` / `knockoutEnergyRegen` | `0.40` / `0.80` / `0.80` | Health / stamina / Ki recovered while down. |
| `overkillThreshold` | `1.0` | A blow above this fraction of max health cannot be saved. |
| `peakWindowSeconds` | `10` | Window used to find the hardest hit taken. |
| `maxBuffHitRatio` | `0.25` | Hit size (fraction of max health) that grants the full, permanent boost. |
| `minBuffPct` / `maxBuffPct` | `0.01` / `0.10` | Smallest / largest stat boost. |
| `permanentMaxBuffs` | `3` | Number of permanent Zenkai boosts. |
| `tempBuffMinSeconds` / `tempBuffMaxSeconds` | `120` / `240` | Duration range of temporary boosts. |
| `releaseBonusPerZenkai` / `releaseBonusCap` | `0.05` / `0.15` | Limitless Power bonus per Zenkai and its cap. |
| `buffStats` | `["STR","SKP","PWR"]` | Stats boosted by Zenkai. |

### `namekian`

| Key | Default | What it does |
| :-- | :-- | :-- |
| `assimilationAmount` | `4` | Maximum assimilations. |
| `assimilationCooldownSeconds` | `600` | Assimilation cooldown. |
| `assimilationHealthRegen` | `0.35` | Health restored on assimilation. |
| `assimilationStatBoost` | `0.075` | Permanent stat gain per assimilation. |
| `assimilationBoosts` | `["STR","SKP","PWR"]` | Stats boosted by assimilation. |
| `assimilationOnNamekNpcs` | `true` | Allows assimilating Namekian NPCs. |
| `assimilationRequestTimeoutSeconds` | `30` | Time a player has to accept a request. |
| `regenChannelSeconds` | `3` | Regeneration duration. |
| `regenHealthRatio` | `0.25` | Health restored by Regeneration. |
| `regenEnergyCost` / `regenStaminaCost` | `0.30` / `0.20` | Ki / stamina spent by Regeneration. |
| `regenCooldownSeconds` | `45` | Regeneration cooldown. |
| `waterRegenBonus` / `waterRegenSeconds` | `0.25` / `5` | Water Affinity bonus and duration. |

### `frostdemon`

| Key | Default | What it does |
| :-- | :-- | :-- |
| `tpBoost` | `1.25` | TP gain multiplier (minimum `1`). |
| `masteryGainBonus` | `0.25` | Extra form mastery gain. |
| `kiTechniqueResistance` / `strikeTechniqueResistance` | `0.125` / `0.05` | Damage reduction against Ki / strike techniques. |
| `reserveChargeReleaseThreshold` | `25` | Maximum Power Release (%) to store Ki. |
| `reserveMaxRatio` | `0.50` | Reserve size as a fraction of max Ki. |
| `reserveFullChargeSeconds` | `30` | Time to fill the reserve. |
| `reserveDurationSeconds` | `10` | How long the reserve feeds your form. |
| `reserveFormBonus` | `0.15` | Form bonus increase while the reserve is used. |
| `reserveChargeForms` | `["second","third","final"]` | Forms (besides base) in which the reserve charges. |

### `bioandroid`

| Key | Default | What it does |
| :-- | :-- | :-- |
| `cooldownSeconds` | `180` | Vital Drain cooldown. |
| `drainRatio` | `0.25` | Total fraction of the target's max health drained. |
| `drainTpRatio` | `0.50` | TP ratio granted for a Vital Drain kill in the Imperfect form. |
| `explodeChargeSeconds` | `5` | Self-Destruct charge time. |
| `explodeRevertSeconds` | `4` | Time your swollen body takes to settle back if you cancel the charge (you can't charge again until it does). |
| `explodeDamageRatio` | `5.0` | Explosion damage as a multiple of your max health. |
| `explodeRadius` | `36` | Explosion radius in blocks. |
| `explodeKillsUser` | `false` | If `true`, the explosion kills you. |
| `explodeKnockdownSeconds` | `30` | Knockdown after surviving the explosion. |
| `explodeCooldownSeconds` | `600` | Self-Destruct cooldown. |
| `cellJrMax` | `3` | Cell Jrs. per player. |
| `cellJrStatRatio` | `0.20` | Cell Jr. health and damage relative to you. |
| `cellJrOwnerPenalty` | `0.10` | Stat penalty per living Cell Jr. |
| `cellJrChargeCooldownSeconds` | `240` | Recharge time of a destroyed Cell Jr.'s slot. |
| `cellJrLeashRange` | `64` | A Cell Jr. farther than this from you disappears. |
| `cellJrGlobalCap` | `30` | Maximum Cell Jrs. alive on the server. |

### `majin`

| Key | Default | What it does |
| :-- | :-- | :-- |
| `absorptionAmount` | `3` | Maximum absorptions. |
| `absorptionCooldownSeconds` | `600` | Absorption cooldown. |
| `absorptionHealthRegen` | `0.30` | Health restored on absorption. |
| `absorptionStatCopy` | `0.04` | Fraction of the target's stats copied. |
| `absorptionBoosts` | `["STR","SKP","PWR"]` | Stats boosted by absorption. |
| `absorptionOnMobs` | `true` | Allows absorbing mobs. |
| `slotEjectCooldownSeconds` | `300` | Lock time of an ejected slot. |
| `foodHealBonus` | `0.25` | Sweet Tooth food healing bonus. |
| `kiHealReceivedBonus` / `kiHealDealtBonus` | `0.25` / `0.25` | Healing Magic bonuses. |
| `healTechniqueCooldownReduction` / `healTechniqueCostReduction` | `0.10` / `0.10` | Healing technique cooldown / Ki cost reduction. |

### `glind`

| Key | Default | What it does |
| :-- | :-- | :-- |
| `divineKi` | `true` | Enables Divine Ki. |
| `meditationDodgeMultiplier` | `1.5` | Meditation dodge chance multiplier. |
| `regenMultiplier` | `1.25` | Health, Ki and stamina regeneration multiplier. |
| `healingReductionBase` | `0.20` | Divine Punishment healing reduction. |
| `healingReductionStackEfficiency` | `0.5` | Effectiveness of the Healing Reduction skill/enchantment on top. |
| `evilAttackerMaxAlignment` / `goodTargetMinAlignment` | `25` / `60` | Evil Glind punishes players at or above this alignment. |
| `goodAttackerMinAlignment` / `evilTargetMaxAlignment` | `75` / `40` | Good Glind punishes players at or below this alignment. |
| `playerDamageBonus` | `0.10` | Damage bonus against opposite-alignment players. |
| `frostDemonDamageBonus` / `majinDamageBonus` | `0.15` / `0.05` | Bonus against opposite-alignment Frost Demons / Majins. |
| `npcDamageBonus` | `0.075` | Bonus against any non-player foe. |

> **Upgrading from an older config:** the old flat keys (`saiyanZenkaiAmount`, `namekianAssimilationAmount`, `majinAbsorptionAmount`, `bioAndroidDrainRatio`, …) are moved into the per-race sections automatically the first time v2.2 loads the file. The old Majin Revive and Human Ki-regen keys are removed.

---

Related: [[Races|Races]] · [[Skills & Abilities|Abilities]] · [[General Server|General-Server]] · [[Transformations & Mastery|Transformations-and-Mastery]] · [[Dragons & Wishes|Dragons-and-Wishes]] · [[Commands|Commands]]
