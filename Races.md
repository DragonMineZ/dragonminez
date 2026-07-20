# Races

DMZ ships **6 playable races**, each with its own **racial skill** (a unique passive/active), appearance options, and transformation lines.

> This is the **reference** page (what each race *is* and its default setup). To *create or fully re-tune* a race, see [[Custom Races|Custom-Races]]; for its transformations see [[Custom Forms|Custom-Forms]] and [[Transformations & Mastery|Transformations-and-Mastery]].

The default races are: **Human, Saiyan, Namekian, Frost Demon, Bio-Android, Majin**.

## What you can edit per race

Each race has a `config/dragonminez/races/<race>/` folder. In `character.json` you can edit:

- **Gender** (`hasGender`) — only Human, Saiyan and Majin use gender by default.
- **Model**: use the vanilla player skin (`useVanillaSkin`), a custom model, layering, model scaling, head bones.
- **Default appearance**: body type, hair type, eyes, nose, mouth, tattoo — plus all colors (3 body colors, hair, both eyes, aura) and aura type.
- **Racial skill** assignment and **form-skill TP costs** (super/god/legendary/android forms).

Players themselves pick from the appearance options above in the character-creation screen; the number of body types, eyes, etc. is driven by how many textures exist for that race. Stat scalings live separately in `stats.json` (see [[Player Classes|Player-Classes]] and [[Custom Races|Custom-Races]]).

> **Tuning the racial skills.** Every racial passive/active below can be toggled and re-balanced (percentages, cooldowns, stack counts, level gates) under the `racialSkills` block of `general-server.json` — including a master `enableRacialSkills` switch. See [[General Server|general-server]]. The exact percentages shown here are the **defaults**; the in-game **X (skills) menu** always displays your server's live values.

---

## Human

- **Racial skill — Blood-Fueled Ki:** always-on passive. Ki attacks cost **25% less Ki**, but the ki you save is drawn from your **health** instead (**12.5%** of the original ki cost, as flat HP loss); that drained HP is then added back as **bonus flat damage** on the attack's first hit. No cooldown — it applies continuously whenever you charge/fire a ki attack.
- **Hair:** yes (selectable hairstyles).
- **Gender:** yes.
- **Default body type:** 0 (uses the player's Minecraft skin + extra body types, per gender).
- **Legendary forms:** yes (locked by default — see below).

### Android sub-race

Humans (and **only** humans) can convert into **Androids** — a **sub-race** of the human race. This is a major, defining choice:

- **It is permanent.** Once you become an Android you **cannot revert** to a normal human.
- **Different transformations.** As an Android you lose access to the human **Super forms** and **Legendary forms**, and instead gain the **Android forms** line (Android Base → Super Android → Fused Android). A normal human has the opposite: Super/Legendary forms but no Android forms.
- **Blood-Fueled Ki upgrades:** the Ki discount rises to **50%**, all **Ki regeneration is doubled**, but ki attacks deal **15% less damage** — and the HP-drain/bonus-damage part of the passive no longer applies. See [[Transformations & Mastery|Transformations-and-Mastery]].

## Saiyan

- **Racial skill — Zenkai:** after staying **8 seconds below 15% HP**, a Zenkai triggers — it heals **20%** of your max HP and grants a **permanent +7.5%** to **STR/SKP/PWR**. Stacks up to **3 times**, **900s** cooldown, and only works from **level 100** onward. Saiyans also have a **tail** (Oozaru).
- **Hair:** yes (selectable hairstyles).
- **Gender:** yes.
- **Default body type:** 0 (vanilla skin + extra body types, per gender).
- **Legendary forms:** yes — the classic Legendary Super Saiyan line (locked by default).

## Namekian

- **Racial skill — Assimilation:** select the skill in the **X (skills) menu**, then hold **G** next to another Namekian — players, and by default Namek NPCs too — to assimilate them: heal **35%** of your max HP and gain a **permanent +7.5%** to **STR/SKP/PWR**. Up to **4 times**. Namekians also **regenerate**.
- **Hair:** no — uses **antennae/ears** (`ears1–3`).
- **Gender:** no.
- **Default body type:** 0 (3 body types available).
- **Legendary forms:** yes (locked by default).

## Frost Demon

- **Racial skill — Prodigious Strength:** always-on passive that raises your **TP (training-point) gain by +25%** (×1.25) — Frost Demons simply train faster. This **stacks** with other TP boosts (e.g. the Hyperbolic Time Chamber).
- **Hair:** no — uses **horns** (`horns1–5`).
- **Gender:** no.
- **Default body type:** 0 (3 base body types; note it also has dedicated third/final/fifth-form bodies). Smaller default model scale (0.7375).
- **Legendary forms:** yes (locked by default).

## Bio-Android

- **Racial skill — Life Drain:** select the skill in the **X menu** and press **G** to drain a target — it deals **25% of the target's max health** as damage and **heals you for the same amount**. **180s** cooldown. (The classic Cell-style absorption.)
- **Hair:** no (no head appendage bones).
- **Gender:** no.
- **Default body type:** 0 (3 body types, `base_0–2`).
- **Legendary forms:** yes (locked by default).

## Majin

- **Racial skill — Absorption:** select the skill in the **X menu** and press **G** to absorb a target: heal **30%** of your max HP and gain a **permanent +4% of the target's stats** added to your **STR/SKP/PWR**. Up to **3 times**; works on mobs too by default.
- **Hair:** no — uses the **head tentacle** (`majin1–3`).
- **Gender:** yes.
- **Default body type:** 0 (3 body types per gender).
- **Legendary forms:** yes (locked by default).

---

## About Legendary Forms

**All 6 races have a 3-tier legendary form line**, but by default its TP cost is `-1` on every tier, meaning it is **not purchasable** the normal way. Legendary forms are meant to be obtained through the **Mutant "legendary form holder" lottery** (see the `mutant` section in [[General Server|general-server]]), not bought with training points — unless a server owner re-enables the costs. See [[Transformations & Mastery|Transformations-and-Mastery]].

---

## Planned races

Two more races are planned for future updates:

- **Cerealian (Granolah's race)** — the "Cereal"/Cerealian race.
- **A community-voted race** — the second new race will be decided by a **player vote**.

---

Related: [[Player Classes|Player-Classes]] · [[Transformations & Mastery|Transformations-and-Mastery]] · [[Custom Races|Custom-Races]] · [[Custom Forms|Custom-Forms]]
