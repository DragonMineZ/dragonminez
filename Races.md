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

---

## Human

- **Racial skill — Ki Regeneration Boost:** passive **×1.40 ki regen**.
- **Hair:** yes (selectable hairstyles).
- **Gender:** yes.
- **Default body type:** 0 (uses the player's Minecraft skin + extra body types, per gender).
- **Legendary forms:** yes (locked by default — see below).

### Android sub-race

Humans (and **only** humans) can convert into **Androids** — a **sub-race** of the human race. This is a major, defining choice:

- **It is permanent.** Once you become an Android you **cannot revert** to a normal human.
- **Different transformations.** As an Android you lose access to the human **Super forms** and **Legendary forms**, and instead gain the **Android forms** line (Android Base → Super Android → Fused Android). A normal human has the opposite: Super/Legendary forms but no Android forms.
- Being an Android also changes how your ki works (e.g. boosted energy regen and reduced ki costs). See [[Transformations & Mastery|Transformations-and-Mastery]].

## Saiyan

- **Racial skill — Zenkai:** on recovering from near-death, gain a permanent **+10%** to **STR/SKP/PWR**, heal **20%** HP; stacks up to **3 times**, 900s cooldown. Saiyans also have a **tail** (Oozaru).
- **Hair:** yes (selectable hairstyles).
- **Gender:** yes.
- **Default body type:** 0 (vanilla skin + extra body types, per gender).
- **Legendary forms:** yes — the classic Legendary Super Saiyan line (locked by default).

## Namekian

- **Racial skill — Assimilation:** absorb other Namekians (players and, optionally, Namek NPCs) to gain a permanent **+15%** to **STR/SKP/PWR** and heal **35%** HP; up to **4 times**. Namekians also **regenerate**.
- **Hair:** no — uses **antennae/ears** (`ears1–3`).
- **Gender:** no.
- **Default body type:** 0 (3 body types available).
- **Legendary forms:** yes (locked by default).

## Frost Demon

- **Racial skill — Training Boost:** passive **×1.25 TP gain** (trains faster).
- **Hair:** no — uses **horns** (`horns1–5`).
- **Gender:** no.
- **Default body type:** 0 (3 base body types; note it also has dedicated third/final/fifth-form bodies). Smaller default model scale (0.7375).
- **Legendary forms:** yes (locked by default).

## Bio-Android

- **Racial skill — Drain:** actively drain living targets (default **25%** ratio) to empower yourself; 180s cooldown, applies a passive effect. (The classic Cell-style absorption.)
- **Hair:** no (no head appendage bones).
- **Gender:** no.
- **Default body type:** 0 (3 body types, `base_0–2`).
- **Legendary forms:** yes (locked by default).

## Majin

- **Racial skill — Absorption + Revive:** absorb targets to copy **4%** of their stats (up to **3 times**, heal **30%**, also works on mobs); and a separate **Revive** skill to come back from a blob (3600s cooldown, restores 25% HP per blob).
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
