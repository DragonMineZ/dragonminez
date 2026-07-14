# Masters

**Masters** are the trainer NPCs who teach you [[Skills & Abilities|Abilities]], [[Techniques|Techniques]] and — for a few of them — run a **training minigame**. You interact with a master by right-clicking them: this opens a dialogue with a **Skills/Services** option (buy skills & techniques with [[Training Points|Training-Points-and-Mastery]]) and, for some, a **Train** option (a minigame that rewards TP).

> Some masters only deal with you if your [[alignment|Stats-and-Attributes]] fits (bands: **Good 61–100**, **Neutral 41–60**, **Evil 0–40**). Those requirements are noted per master below. Hostility toward you is a **separate** system (`alignment_rules.json`).

Everything a master offers is **data-driven** in `config/dragonminez/skills.json` (`skillOfferings`) and `training.json` — these are the **defaults**.

---

## Where to find each master

| Master | Where | Alignment to talk | Also offers training |
| :-- | :-- | :-- | :-- |
| **Master Roshi** | Roshi's House (Overworld coast) | **Good (≥ 61)** | — |
| **Goku** | Goku's House (Overworld plains) | **Good (≥ 61)** | — |
| **Piccolo** | Piccolo's House (Overworld land biomes) | None | — |
| **Yamcha** | Yamcha's House (Overworld desert) | None | — |
| **Trunks** | Trunks' Ship (Overworld land biomes) | None | **Precision** |
| **Vegeta** | Vegeta's Pod (Overworld rocky) | None | **Gravity** |
| **Cell** | Cell Arena (Overworld plains) | **Evil (≤ 40)** | — |
| **Gohan** | Story NPC — met through his saga on Earth | **Good (≥ 61)** | **Shadow Boxing** |
| **Krillin** | Story NPC — met through the Saiyan/Vegeta saga on Earth | **Good (≥ 61)** | **Control** |
| **Mr. Popo** | Kami's Lookout (Overworld, near spawn) | None | **Rhythm** |
| **Frieza** | Frieza's Ship (Planet Namek) | **Evil (≤ 40)** | — |
| **Old Kai** | Old Kai's Pillar (Sacred Planet) | **Good (≥ 61)** | — |
| **King Kai** | The **Otherworld**, on King Kai's Planet (reached via [[Otherworld\|Dimension-Otherworld]]) | **Good (≥ 61)** | — |

> **Gohan and Krillin** don't have their own landmark building — they're **story/quest NPCs** placed along their [[sagas|Sagas]], so you meet them by progressing the story rather than by locating a structure. They still teach and (Gohan/Krillin) run a training minigame like any other master.

Other NPCs at these spots (Karin, Dende, Enma, Baba, Grand Kai, Grand Elder, Dr. Gero, Babidi…) give **story dialogue, quests or services** rather than the skill list below. See [[Structures|Structures]] and [[Bestiary-NPCs|Bestiary-NPCs]].

---

## What each master teaches

Techniques are split into **Ki** (ranged energy) and **Strike** (melee) — see [[Techniques|Techniques]]. Plain **Skills/Abilities** are passive or utility unlocks — see [[Skills & Abilities|Abilities]]. Costs are paid in **Training Points**; a technique/skill still needs your **race** to allow it.

### Master Roshi — *the basics*
- **Skills:** Jump · Meditation · Ki Control
- **Ki:** Kamehameha

### Goku
- **Skills:** Flight · Instant Transmission · **Fusion** (the Fusion Dance — see [[Fusions|Fusions]])
- **Ki:** Kamehameha · Spirit Bomb
- **Strike:** Dragon Fist · Super God Fist · Oozaru Fist

### King Kai
- **Skills:** Potential Unlock · Ki Manipulation
- **Stack form:** **Kaioken** (see [[Transformations & Mastery|Transformations-and-Mastery]])
- **Ki:** Spirit Bomb
- **Strike:** Kaioken Attack

### Piccolo
- **Skills:** Potential Unlock · Ki Control
- **Ki:** Masenko · Special Beam Cannon (Makankosappo) · Ki Barrage · Destructo Disc (Kienzan)

### Gohan  *(training: Shadow Boxing)*
- **Skills:** Ki Boost · Ki Protection · Ki Sense
- **Ki:** Masenko · Special Beam Cannon · Kamehameha · Ki Barrage

### Krillin  *(training: Control)*
- **Skills:** Sprint · Ki Sense
- **Ki:** Destructo Disc · Double Destructo Disc · Ki Barrage · Solar Flare (Taiyoken)
- **Strike:** Deadly Dance

### Vegeta  *(training: Gravity)*
- **Skills:** Defense Penetration · Potential Unlock
- **Ki:** Galick Gun · Big Bang Attack · Final Flash · Final Explosion · Fake Moon
- **Strike:** Deadly Dance (Vegetto)

### Trunks  *(training: Precision)*
- **Skills:** Ki Boost · Ki Protection
- **Ki:** Burning Attack · Galick Gun · Ki Barrage
- **Strike:** Meteor

### Yamcha
- **Skills:** Ki Control · Ki Infusion
- **Ki:** Spirit Ball (Sokidan) · Kamehameha · Ki Barrage
- **Strike:** Wolf Fang Fist

### Old Kai
- **Skills:** Ki Infusion · Healing Reduction
- **Ki:** Soul Punisher

### Frieza  *(Evil)*
- **Skills:** Flight · Jump · Sprint · Ki Sense · Meditation · Potential Unlock · Instant Transmission
- **Ki:** Death Beam · Emperor Death Beam · Supernova · Double Destructo Disc
- **Strike:** Deadly Dance · Meteor

### Cell  *(Evil)*
- **Skills:** Ki Control · Ki Manipulation · Ki Infusion · Ki Protection · Defense Penetration · Healing Reduction · Ki Boost
- **Ki:** Kamehameha · Destructo Disc · Galick Gun · Masenko · Death Beam · Ki Barrage
- **Strike:** Deadly Dance · Meteor

> Cell and Frieza cover a huge chunk of the technique roster on their own — but you need **Evil alignment (≤ 40)** to train with them, so a "good" character can't learn everything from a single master.

---

## Training minigames

Five masters run a **training minigame** — a skill challenge that awards **Training Points** scaled to your current power (the stronger you are, the more TP per level cleared), capped per game. You first **learn** the minigame from its master (the **Train** option), after which you can replay it anytime from the **Minigames** tab of the Character Menu (**V**). Each targets a different skill:

| Minigame | Master | What you do |
| :-- | :-- | :-- |
| **Rhythm** | Mr. Popo | Hit the matching arrow (←↓↑→) as each note reaches the center; hold notes stay pressed. |
| **Control** | Krillin | Keep a moving marker inside the green zone (←/→) for 5 seconds straight; the zone shrinks and speeds up each level. |
| **Shadow Boxing** | Gohan | Watch a flashing arrow sequence, then repeat it exactly; each level adds an arrow and shortens the reveal. |
| **Precision** | Trunks | Click each circle the moment its shrinking ring meets it; Perfect/ Good timing scores, misses drain points. |
| **Gravity** | Vegeta | Tap the highlighted side (←/→) to push an indicator up past gravity; wrong side shoves it down. |

### Shadow Dummy (self-serve)

Not a master minigame, but it lives in the same **Minigames** tab. Once you've learned **Ki Control**, reached **Ki Manipulation level 5**, and killed a Shadow Dummy at least once, you can manifest a **Shadow Dummy** made of your own ki (25–75% of your stats). It fights on its own, but **you lose that same percentage of your stats while it lives** — a sparring partner that doubles as a mastery/combat-training target. See [[Training Points & Mastery|Training-Points-and-Mastery]].

---

Related: [[Structures|Structures]] · [[Skills & Abilities|Abilities]] · [[Techniques|Techniques]] · [[Training Points & Mastery|Training-Points-and-Mastery]] · [[Fusions|Fusions]] · [[Stats & Attributes|Stats-and-Attributes]]
