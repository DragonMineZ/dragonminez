# DragonMineZ v2.1 — "End of Z Intro" Patch Notes

> **Version:** `2.0.4` → `2.1` (current build tag: `2.1`)  
> **Minecraft / Loader:** 1.20.1, Forge 47.4.10  
> **Scope:** 489 commits · ~1,469 files changed (~136k insertions / ~82k deletions)

This is a **major content + systems update**. Headlines: a brand-new **combo melee combat system**, a full **story/quest mode (DBSagas)** across the Z timeline, an **alignment system**, **RPG classes with passives**, a **custom Ki technique creator**, datapack-driven **Dragon Balls / Wishes / Space Pods**, a **party system**, and a huge **UI/visual overhaul**.

> ⚠️ **Required dependency change:** **Curios API** is now a **required** dependency. Players must install Curios to run 2.1.

---

### ✨ New Features

- **Dynamic Growing Stats** — Stats now scale dynamically as players progress; a new `DynamicGrowth` system tracks growth rate per-stat and applies configurable multipliers over time. New server-side config options have been added to tune growth curves.
- **Class Passives** — All class passives are now live and functional. Each passive is backed by a dedicated handler: Warrior combo-stacking (stamina regen + armor penetration at max stacks), Berserker low-HP bonus regen and crit chance, Tank stamina-to-regen conversion (doubled at low HP), Paladin party lifesteal share and incoming damage redirect (within 50 blocks), Martial Artist bonus damage vs. wounded targets, Spiritualist ki cooldown reduction for damage techniques and longer debuff durations, Cleric ki cooldown reduction for heal techniques and longer buff durations.
- **Ki Boost Skill** — New **Ki Boost** skill added to the skills menu. The action charge handler has been refactored to use form masteries for charging thresholds rather than raw stats.
- **Strike Attacks Level System** — Strike (melee) techniques now have a dedicated level that scales their damage. The higher the strike level, the more damage each strike technique deals, rewarding investment in melee-focused combat styles.
- **Regen Scales with Saturation** — Ki, health, and stamina regeneration now scale with the player's food saturation level. Keeping hunger full provides a passive regen multiplier bonus. Power Release charge has also been fixed.
- **New Transformation Animations** — Frost Demons, Saiyans, Majins, and Absorption forms each now have new, race-specific transformation animations.
- **Human & Android-Human Racial Skills Reworked** — Racial skills for the Human and Android-Human lineages have been redesigned and rebalanced, with updated stat scaling, ki projectile interactions, and corrected lang entries.
- **Full Damaged-Armor Support** — All damaged armor variants (every tier and type) are now properly supported and render correctly. The cape armor model has also been fixed.
- **Gete-tech Items & Quests** — New Gete material, with a full tool set post-netherite. Bulma's mega R&D sidequest storyline gains additional Gete-tech quests. Gete-tech armor enchantments **Ki Conductivity** (ki/energy regen boost) and **Gravity Forged** (knockback reduction) are fully implemented; the **Gete Plating** enchant has been removed.
- **Gete Tool Tiers** — Gete-tech materials now have a proper tool tier, enabling Gete tools to harvest blocks at the correct tier level. Item models, Kikono recipes (cleaned up and rebalanced), and the item registry have all been updated to integrate the new tiers.
- **Beam Clash** — Added a Beam Clashing system whenever two Ki Attacks collide, starting a fast minigame where players can tap at the right moment to gain momentum and push the clash toward their opponent. The clash outcome blends timing with stats, and includes a cinematic camera effect.
- **Ki Projectile Screen Shake** — Ki projectiles trigger a camera shake effect scaled to the projectile's blast radius. Larger ki attacks produce a noticeably stronger shake for players caught near the impact zone.
- **Quest & Party Entity Logic Improvements** — Improved quest entity tracking and party interaction rules in the server entity events handler — better handling of quest enemy spawns and party-member targeting/exclusion.
- **Config Menu Screen Cleaned Up** — Removed the player model rendering widget from the Config Menu screen; layout simplified and streamlined.
- **Flying Nimbus: 2-Player Capacity** — The Flying Nimbus can now carry two players simultaneously.
- **Flying Nimbus & Black Nimbus: Speed Boost** — Both cloud mounts now support a speed burst activated by pressing the Dash key while riding. The boost fires a sharp initial impulse that decays smoothly to a sustained extra push over ~45 ticks before gradually bleeding back to normal speed. Activating the boost spawns a radial particle burst from behind the cloud; a particle trail streams from the rear throughout the boost duration (golden particles on the Flying Nimbus, dark blue on the Black Nimbus).
- **Mastery System Reworked & Expanded** — Form mastery has been significantly overhauled. Each form now carries its own dedicated mastery track with more granular config options per form (`FormConfig`); Server owners can fine-tune mastery thresholds and rates per form.
- **Shared Mastery Across Related Forms** — Forms can now share mastery XP with related forms in the same group (e.g., time spent in SSJ1 contributes toward SSJ2 mastery progress). Opt-in and configurable per form via `FormConfig`.
- **Transformations Use Revised Defense Scaling** — Defense multipliers during transformations are now configured independently per form in `FormConfig`, decoupled from general stat multipliers. This allows forms to have precise, individual defense profiles instead of inheriting a blanket multiplier.
- **Advanced Stat Tooltips on Character Screen** — The Character Stats screen now displays detailed tooltip breakdowns showing per-source stat contributions (base value, bonus stats, form multipliers).
- **Bonus Stats Split: RES → DEF + STM** — The bonus stat system (`BonusStats`) now tracks Defense (DEF) and Stamina (STM) as distinct bonus categories, replacing the previous single Resistance (RES) bucket.
- **Ki Barrier: Shield Other Targets** — The Ki Shield/Barrier technique can be directed to protect other players or entities rather than only the caster, enabling support-style plays where a player deploys a barrier on an ally.
- **Chasing (Homing) Ki Attacks** — Ki techniques can be fired as homing attacks that track their target mid-flight.
- **Rocky Biome: New Terrain Formations** — The Rocky biome gains three new world-generation features. The Rocky biome no longer replaces vanilla biomes (Desert, Savanna, Wooded Badlands); it now generates as an additive region in hot, dry, inland, high-erosion climate bands. Rocky Peak generation has also been reworked with a more natural tapering profile.
- **Battle Power Display on Character Stats Screen** — Battle Power (BP) is now shown on the Character Stats screen (V menu). The BP calculation formula has also been reworked to better reflect overall player strength.
- **Instant Transmission** — Instant Transmission on tap teleports you to your latest damaged entity on your line of sight, using refined target selection for more precise picks. On hold it opens an interactive menu listing remembered Masters, nearby players and party members. The system handles Battle Power thresholds, God Form restrictions, and Android race rules.
- **Ki Sense Rework: Combat Indicators & Search Mode** — Ki Sense has been substantially overhauled. It now displays Ki, Stamina, Battle Power, estimated damage output, and health values via an in-world combat indicator overlay. A new **Search Mode** applies a grayscale shader to the entire screen, enabling focused long-range scanning of the surrounding area while rendering an aura on detected entities, which scales on size and color based on their strength compared to yours.
- **Healing Reduction Skill & Enchantment** — A new skill and weapon enchantment that reduce an enemy's incoming healing when applied.
- **10 New Master NPCs** — Ten new trainable Masters have been added: **Babidi, Cell, Frieza, Gohan, Krillin, Old Kai (Elder Kai), Piccolo, Future Trunks, Vegeta, and Yamcha**.
- **Sacred Kai Planet** — Added a new generated planet destination, the Sacred Kai Planet.
- **12 New Armor Sets** — Added 12 new wearable armor sets: Android 17 (Super), Android 18 (Kame House, Tournament, and Cell Saga variants), Beerus, Goku's Whis-symbol Gi, Kefla, Android 21 (Majin), Vegeta's Whis-symbol Gi, Vegeta (GT), Videl, and Whis — plus missing crafting recipes for 9 previously uncraftable armor sets.
- **Training Animations** — Using a training minigame now plays a visible flex or meditation animation on your character for as long as the minigame runs, instead of standing idle.
- **Gravity Device** — A new craftable machine block that creates an artificial high-gravity training zone inside a fully enclosed room. Right-click to open the GUI, set a target gravity multiplier (1–1000×, server-configurable), and toggle the device on. The room must be fully sealed and within 5–25 blocks on each axis (configurable); the GUI reports "No Room" if the enclosure is invalid. The device consumes **Star Energy** from connected energy cables (default: 1 SE per gravity unit per second; 20,000 SE internal buffer). While running, all players inside the gravity zone see a full-screen red distortion shader whose intensity scales with gravity level. Shift+right-clicking the block toggles a wireframe room visualizer (green = valid enclosure, red = invalid). Crafted from Iron Blocks, a T1 Radar CPU, Redstone Repeaters, a Weighted Item (Turtle Shell, Workout Weights, or Weight Piccolo Cape), and an Anvil.
- **Oozaru (Great Ape) Overhaul** — Saiyans can no longer manually trigger the Oozaru transformation; it now only triggers naturally at night during a full moon while looking up at the moon, and charges automatically once those conditions are met. This also works with the Fake Moon skill.
- **Vanilla `/locate` Now Finds DragonMineZ Structures** — Using Minecraft's built-in `/locate structure` command on a DragonMineZ structure now redirects to the mod's own locator logic, so you don't need to remember the separate `/dmzlocate` command.
- **Quest NPC Team Protection** — NPCs spawned together for the same quest objective are now flagged as a team and can no longer accidentally damage, target, or kill each other (including with projectiles), preventing friendly-fire chaos during multi-enemy quest fights.
- **Faster, Smarter Quest Structure Lookup** — Quest-related structure locations (Goku's House, Kami's Lookout, Babidi's location, the Time Chamber, etc.) are now located on a background thread and cached server-side instead of being resolved synchronously every time quest data syncs, reducing potential hitches when quest data loads.
- **Master Technique Assignments** — **Yamcha** now teaches the **Sokidan** (Spirit Ball), **Old Kai (Elder Kai)** teaches **Soul Punisher**, **Vegeta** teaches the **Fake Moon** technique, **Gohan** and **Future Trunks** each teach **Ki Protection**, and **Roshi** and **Krillin** both teach **Taiyoken (Solar Flare)** as learnable master skills.
- **Weighted Training Items: Roshi, Piccolo & King Kai** — New WeightedItem drops have been assigned to Master Roshi, Piccolo, and King Kai, extending the weighted training gear system to more masters.
- **Capsule Corp Villager** — A new Capsule Corp Villager NPC type spawns at Capsule Corporation locations and offers unique map trades, opening a merchant progression path tied to the Capsule Corp faction.
- **Skill Data Repair** — The skills system now automatically detects and repairs corrupted or outdated skill entries on load, preventing broken skill states after version updates.
- **Mutant Effect** — A new **Mutant** status effect is now available, obtained through a random roll mechanic. Players who roll the Mutant trait receive a passive **TP Multiplier** bonus and gain access to the **next available Legendary Form** in their race's sequence (sequential unlock — one form at a time — rather than granting all Legendary Forms at once); the effect and its multiplier are visible as a tooltip on the V (character stats) menu.
- **Stat Relocation** — Players can now relocate (redistribute) their allocated stat points via a new command or as a **Shenron wish**, allowing full reallocation of invested TP across all stats without starting over.
- **~35 New Advancements** — Approximately 35 new Minecraft advancements have been added covering DragonMineZ-specific milestones, progression goals, and hidden secrets.
- **Action Block Feedback** — Players now receive an informative on-screen message whenever an action is blocked, explaining exactly what requirement is missing — such as needing a minimum mastery level to transform or the Ki Control skill to fly.
- **Quest Start Button Cooldown** — After attempting a quest (starting, completing, or failing), the Start Quest button automatically reappears after a **60-second cooldown**, removing the need for a menu relog to retry.
- **In-Game Mod Configuration Screen** — DragonMineZ now ships a dedicated GUI config screen (`DMZModConfigScreen` + `DMZConfigEditScreen`) accessible directly from the Forge Mods menu. Players and server operators can browse and edit all config options in-game without external editors or restart requirements; options are grouped, labeled, and editable through the screen with proper field validation.
- **Mob Loot Tables** — Loot tables have been added for several world mobs: **Bandit**, **Sabertooth**, **Red Ribbon Soldier**, **Robot** (three variants), and **Frieza Soldier** (three story variants). Each table drops thematically appropriate items with weighted rarity. The **DinoKid** loot table has also been revised and streamlined.

### 🎨 Visual & Texture Updates

- **Improved Ki Attack Visuals** — The previous 2D sprite-based ki attack visuals have been replaced with new 3D models and shader effects for all ki techniques, giving them a more dynamic and impactful appearance. Enhanced particle effects and lighting have also been added to ki attacks for greater visual flair.
- **Reworked Item & Block Textures** 
- **Ki & Sparks Shader** — Auras and Lightning effects now feature a completely overhauled visual renderer, bringing sharper outlines, improved spark effects, and full compatibility with **Oculus shader packs**.
- **Overhauled Flying Nimbus Model** — The Flying Nimbus and Black Nimbus receive refreshed geometry, and updated textures for both cloud variants.
- **Skin Layer Face Rendering Rework** — `DMZSkinLayer` face rendering pipeline substantially refactored to improve correctness and performance of per-race facial feature rendering.
- **Improved Hair Physics** — `HairRenderer` simulation has been refined for more natural hair movement and layer behaviour during flight, transformation, and idle states.
- **Aura & Outline Rendering Reworked for Shaderpack Compat** — The aura and transformation outline pipeline has been overhauled for better shader pack compatibility. `AuraRenderer` has been significantly simplified and now is more reliable with both vanilla and Oculus shader packs installed.
- **Transform-Cancel Fast Hair Animation** — When a transformation is canceled mid-sequence, the player's hair now plays a fast return animation back to the base state, rather than snapping instantly.
- **Combat Animations Reworked** — The full race combat animation set has been substantially overhauled, with large portions rewritten to improve attack timing, combo flow, and overall visual polish.
- **Movement Animations Reworked** — Player locomotion animations (walking, running, strafing, jumping, idle, and related transitions) have been completely rewritten from scratch. The previous animation file has been replaced with a substantially leaner and more accurate set of keyframes, improving the visual feel of all movement states during exploration and combat.
- **Movies Saga Boss Models — Part 2** — 3D models and textures have been added for the second batch of Movies Saga bosses: **Broly** (Base, SSJ Restrained, SSJ, and Legendary SSJ), **Paragus**, **Bojack** (base and Full Power), **Zangya**, **Bido**, **Bujin**, **Gokua**, **Bio-Broly** (standard and Giant-form textures), **Fat Janemba**, **Super Janemba**, and **Paikuhan (Pikkon)**. Entity registrations in `SagaMoviesEntity` have been updated to wire these models into the Movies Saga encounter system.
- **Combat Wounds Visual Layer** — Players now display visible wounds on their character model as their health drops. A wounds texture overlay fades in below 75% max HP; a more severe "grievous wounds" layer additionally fades in below 40% max HP, providing a clear visual cue of a player's damage state during combat.
- **Armor Item Texture Overhaul** — A large batch of armor item icon textures has been reworked and refreshed, covering sets including Android 16/17/18, Bardock (DBZ and Super), Black Goku, Broly Super, Caulifla, and many more. Item icons for the **Kibito** and **Lord Slug** armor sets have also been added.
- **Reduced Aura/Form/Kaioken Tint Intensity on Darker Colors** — The color tint applied to the player model during aura, transformation, and Kaioken effects is now attenuated for darker palette hues, preventing oversaturation and color distortion on dark-colored characters.
- **Majin Absorption Goo Effect** — When a Majin player channels their racial absorption ability, an animated goo-blob overlay now renders over the targeted entity, tinted to match the Majin's body color. The blob pulses, wobbles, and grows organic spikes that intensify as absorption progresses; a looping absorption sound plays at intervals during the process.
- **Additional Armor Sets** — New armor sets added: **Android 13, Android 14, Dr. Gero (Android 20), Capsule Corp, Chiaotzu, Cooler Soldier, Evil Buu, Great Saiyaman 2, Kibito, Raditz, Lord Slug, Super Buu**, plus two bonus crossover outfits (**Subaru Natsuki** & **Daniel Natsuki**). Six new **Dragon Ball Online (DBO)** sets added (art by Darkeryr): **Dragon Clan, Fighter, Mighty Majin, Mystic, Warrior Clan**, and **Wonder Majin**.
- **MetalCooler Texture Fix** — MetalCooler and MetalCooler Core texture and geo model file paths corrected; both the individual unit and Core boss now render correctly.
- **Ki Attack Animation Rework** — The ki entity animation file has been substantially streamlined, removing redundant and conflicting keyframes and improving the accuracy and consistency of ki blast and ki wave animations.
- **New Armor Textures** — Additional item icon textures have been added for several more armor sets.
- **Hildegarn Model Revision (v2)** — The Hildegarn (base and Super form) Movies Saga boss model and animations have received a second-pass revision with improved geometry and smoother animations.
- **DBSagas Entity Animations** — Animations have been added and updated for various DBSagas story-mode entities.

### 🐛 Bug Fixes

- Fixed first-person hand swing animation not playing correctly.
- Fixed NaN values corrupting player health on certain damage or heal events.
- Rebalanced initial and per-level scaling stats for all classes (corrected per-class base values and growth rates).
- Fixed multiplayer shared player animations desyncing between clients.
- Fixed the mining animation playing incorrectly when the player is holding nothing.
- Fixed player hitbox not updating correctly when transforming between forms.
- Fixed aura color tint not smoothly tracking transformation progress — now correctly interpolates tint during power-up sequences.
- Fixed hair transformation speed (animating too fast/slow during form change), aura color desync during transformation, incorrect aura size scaling, and a bug causing hair to appear bald on certain transformation layer transitions.
- Fixed wrong player model rotation on other clients in multiplayer.
- Fixed Dragon Balls rendering with incorrect facing direction.
- Fixed Ki Sense combat indicator overlay not rendering above the player's hair layer; it now correctly draws on top.
- Fixed certain structures being placed on or overlapping Guru and Gero NPC spawn positions.
- Fixed Kikono Station dropping its loot table twice per trigger.
- Fixed Porunga's wish options being incorrect or non-functional.
- Removed the landing impact feature, which was causing unintended issues and has been cut.
- Fixed camera YZ-axis offset causing misalignment in first/third-person views.
- Fixed Majin and Namekian body colors rendering incorrectly in `DMZRacePartsLayer`.
- Fixed armor Protection enchantment incorrectly calculating damage reduction values.
- Fixed sleep animation not playing correctly.
- Fixed hair and skin color desyncing from aura tint during transformation and power-up sequences.
- Fixed a config crash caused by stale cache data; cache is now properly cleared and sync is restored on startup.
- Fixed Ultimate form aura color rendering incorrectly.
- Fixed attack speed becoming unintentionally extreme at very low stat values.
- Fixed aura not rendering correctly when a Ki attack projectile was simultaneously visible on screen.
- Fixed shadow dummy training partner massively inflating player stats during a fight.
- Fixed Majin mark rendering in the wrong draw order.
- Fixed Alt and Ctrl modifier keys overriding skill keybindings; skill key normalization improved.
- Fixed stunned entities still processing incoming damage events when they should be immune.
- Fixed Saiyan Zenkai passive state being shared between all players — each Saiyan now tracks their own independent Zenkai timer in multiplayer.
- Fixed stack form mastery XP being credited to the base form instead of the active stack form during combat.
- Fixed form preview in the Skills screen not rendering correctly for custom addon/datapack transformation forms.
- Fixed Flying Nimbus and Black Nimbus entity alignment — both cloud entities now correctly position and orient riders, resolving a misalignment that caused players to appear offset or clipped into the mount.
- Quest NPCs spawned via the `/questnpc` command are now anchored to their spawn position and made invulnerable to environmental damage, preventing permanent displacement from knockback, water flow, pistons, or other forces.
- Fixed armor not rendering correctly on custom race models.
- Fixed head rotation not playing correctly.
- Fixed Fat Majin cape not rendering while wearing armor.
- Fixed custom race models and textures failing to load correctly.
- Fixed stale `old_[config]` form config files appearing in the Skills Menu and Form selection screen.
- Fixed model size scaling not applying correctly when using stack forms alongside base transformations.
- Fixed fused character aura and body colors not rendering correctly.
- Fixed melee strike attacks being usable on Master NPCs during training sessions.
- Continued iterative combat animation improvements: refined attack timing, fixed Majin geometry, and tuned sword animation parameters.
- Fixed SparksFX effect not triggering correctly in some hit scenarios.
- Fixed KiMediumBall hitbox not registering hits reliably; collision detection for Ki blast and Ki wave entities has been corrected.
- Fixed MetalCooler and MetalCooler Core textures failing to load due to mismatched file paths.
- Further refined sword attack animation parameters across all sword tiers (wooden, iron, diamond, netherite).

---

## ⚔️ Combat System (Major Rework)

The old "1.8-combat" timing gimmick is **gone**, replaced by a fully animation-driven melee system.

- **Combo attacks** — every weapon performs a combo chain, each hit with its own animation, hitbox shape, swing sound, and damage.
  - Swords: two horizontal slashes into a forward stab.
  - Fists/empty-hand: punch → punch → punch → low kick → uppercut → gut kick. With **Ki Infusion** active (lvl 5+) an extra high-damage Black-Flash unlocks.
- **Strike Technique Level** — Strike (melee) techniques now have a dedicated level that scales their damage proportionally as the level increases, incentivizing repeated use and investment in strike-based combat.
- **Data-driven weapon attributes** — 31 weapon types (sword, katana, claymore, spear, scythe, power pole, z-sword, etc.), each defining range, per-attack hitbox shape, damage, wind-up, two-handed flag, and optional crit stats. Weapons can inherit from a parent profile.
- **Dual wielding** — wield two one-handed weapons of the same category; attacks alternate hands (off-hand hits ~0.9× damage). Two-handers and shields can't be dual-wielded.
- **Critical hits** — new **Critical Hit Chance** and **Critical Hit Damage** attributes (from weapons and class passives).
- **Attack speed** — combat now respects an attack-speed stat with proper cooldowns.
- **Block / Parry / Perfect Evasion** — new defensive mechanics tracking poise damage; all three are individually toggleable in config. *Exact parry timing window & on-screen feedback*
- **Guard break** mechanics and **ki projectile diversion** while guarding.
- **Knockback** is now a configurable multiplier per attack.
- **Impact frames** — optional hit-pause/flash effect (off by default).
- **Better Combat addons compatibility** — modded weapons auto-map onto DMZ weapon profiles via item-ID matching (with a blacklist); a `fallbackCompatibilityEnabled` path handles foreign weapons.
- **New damage types:** `strike_attack` (physical melee) and `kiblast` (ki), each with custom death messages.
- **Shadow Dummy** trainer — spawns a clone using a share of your stats (unlock: defeat a Shadow Dummy, learn Ki Control + Ki Manipulation lvl 5+).

### Beam Clashing
- Two ki beams fired at each other trigger a **clash / tug-of-war**, between players and/or NPCs ("BEAM CLASH! Press Right Click to tap!").
- It's a **QTE minigame**: a meter sweeps; hitting the sweet-spot window builds momentum to push the clash toward your opponent.
- **Reworked win condition** — the clash resolution formula has been rebalanced for a more skill-weighted and fair outcome.
- **Lower sweep rate** — the clash meter no longer sweeps as aggressively, making the timing window more readable and less punishing.
- **Spam right-clicking now counts** — rapid right-click inputs are now properly registered during a clash; previously only a fraction of rapid inputs were tracked, making aggressive tapping feel unreliable.
- **Fancier green line indicator** — the clash overlay now features an improved animated green line showing current clash momentum.
- **NPC clash AI updated** — Saga enemy NPCs (DBSagas entities) now properly participate in and react to beam clashes.
- Outcome blends **timing** with **stat power** — stronger fighters start ahead, but timing can decide close clashes; includes a cinematic clash camera.

---

## 🌀 Ki Attacks & Techniques

- **Implemented Ki Attacks** with charge/overcharge, casting, launching, damage reduction while charging, and **8 technique slots across two hotbars**.
- **XP & leveling for Ki attacks.**
- **Predefined Ki techniques** include: **Kamehameha, Galick Gun, Masenko, Spirit Bomb, Supernova (+ Cooler variant), Big Bang, Burning Attack, Final Flash, Kienzan (+ Double Kienzan), Death Beam / Emperor Death Beam, Makankosappo, Ki Barrage, Final Explosion, Sokidan, Fake Moon, Soul Punisher and Taiyoken.** Attack archetypes: wave, beam, laser, disk, giant ball, barrage, explosion, area.
- **Taiyoken (Solar Flare) — How it works** — When cast bare-handed (requires Ki Control skill and Power Release ≥ 5), fires a dazzling flash that blinds all entities within 20 blocks that have line of sight to the caster and are facing toward them. Blinded players see a full-screen white flash shader; blinded mobs lose and cannot re-acquire their target for the blind duration. Duration scales with angle and distance: direct look at close range = up to 12 seconds, partial look at far range ≈ 6 seconds. Friendly-flagged entities are blinded for half duration. The flash palette can be inverted to a dark-out effect via the in-game config screen. 45-second cooldown; 2,000 TP to unlock; learnable from **Roshi** and **Krillin**.
- **Strike (melee) techniques** include: **Meteor Combination, Dragon Fist, Deadly Dance (+ Vegetto variant), Kaioken Attack, Wolf Fang, Oozaru Fist, Super God Fist.**
- **Technique Creator** — build/customize your own Ki techniques: set Type (Heal/Damage), Size, Speed, Armor Pen, Cooldown, plus base/secondary **Effects** (buff/debuff on stats). Techniques can be **created, upgraded, deleted, imported/exported via share codes, and bound to slots**. New **`/dmztech experience`** sub-command grants technique XP directly to players. Ki costs have also been reduced, and the technique creator values display has been corrected.
- **Ki Weapons reworked** — configurable, combo support, custom ki-weapon support, forced/inherited blade color, damage scaling by ki-manipulation level, with custom attributes like criticals and attack speed too.
- **Master skills** — learn techniques from masters; the game remembers masters you've met and their locations. Reworked masters skills menu. Notable assignments: **Yamcha** teaches **Sokidan**, **Old Kai** teaches **Soul Punisher**, **Vegeta** teaches **Fake Moon**, **Roshi** and **Krillin** teach **Taiyoken (Solar Flare)**.

---

## 🧬 Transformations & Forms

A new **Legendary Forms** line was added for every race, plus an **Ultimate** stack form:

- **Saiyan** — SSJ4 [D], SSJ4 [GT]; Mutant group: **Ikari, SSJ Hybrid, SSJ Full Power**
- **Namekian** — **Evil Namek, Evil Giant Namek, Buffed Namek**
- **Bio-Android** (Bio-Corruption) — **Xeno, Xeno Full Power, Xeno Max**
- **Frost Demon** (Artificial) — **Mecha, Metal, Metal-Core** (this is the "5th form" line)
- **Majin** (Demon) — **Innocence Demon, Giant Innocence Demon, Super Demon**
- **Human** — **Shiyoken, Shin Shiyoken, Chou Shiyoken**

- **Custom transformation animations per form**.
- **Shader outlines & bloom** on transformed players; **form preview** on the customization screen.
- **Form mechanics:** forms can grant mob effects (while active or on trigger), support trigger-item and duration-item costs, and **drains can now be negative** (a form can restore resources instead of draining). Separate "form" and "stack form" tracks with **form masteries**.
- **Forms are now displayer in a tree structure** on the Skills Screen, showing their corresponding order and grouping.
- **Extra Form Layer (`extraFormLayer`)** — `FormConfig` now supports an `extraFormLayer` field, allowing any transformation to define an additional skin layer that renders on top of the base model while the form is active. Backed by a generalized `BodyLayerFadeTracker` (replacing the old SSJ4-specific fade tracker), this enables custom per-form skin overlays for both built-in and addon forms.

---

## 📖 Story Mode & Quests (DBSagas)

A full story/quest mode with canon DBZ characters as NPCs, organized into a **Quest Tree** menu by saga.

- **Sagas (in order):** Saiyan → Frieza → Android → **Future (new)** → **Buu (new)** → **Movies (new)**.
- **Large NPC/boss roster** including: Goku (multiple stages incl. early Goku & SSJ3), Vegeta, Gohan, Kid Gohan, Krillin, Bulma, Chiaotzu/Chaoz, Videl, Goten, Trunks, Gotenks, Piccolo, Nail, Shin, Kibito, the full **Ginyu Force** (Guldo, Recoome, Burter, Jeice, Ginyu + body-swapped Ginyu-Goku), **Frieza's forms** (1st/2nd/3rd/Final/Full-Power), Zarbon, Dr. Gero, Babidi, Dabura, Spopovitch, Pui Pui, Yakon, Kid/Super/Evil/Fat Buu, Janemba (Fat & Super), **Great Apes (Oozaru)**, and the full **Movies villain roster**.
- **Movies Saga Boss Roster (Part 1)** — The Movies saga now features a large, fully-modeled villain cast, each with custom 3D models, textures, and AI combat movesets: **Garlic Jr.** (base + transformed), **Dr. Wheelo**, **Turles**, **Lord Slug** (base + Giant form + Slug Soldiers), **Cooler** (base + 5th form) with the full **Armored Squadron** (Dore, Neiz, Salza), **Meta-Cooler** (individual units + giant Core), **Android 13** (base + Super Android 13) alongside **Android 14** and **Android 15**, **Broly** (Base → SSJ Restrained → Legendary Super Saiyan) with **Paragus**, **Bojack** (base + Full Power) with **Zangya**, **Bido**, and **Bujin**, **Bio-Broly** (base + Giant form), **Pikkon** (Paikuhan), **Hirudegarn** (base + Super form), and **Janemba** (Fat Janemba + Super Janemba).
- **Saga Difficulty Tiers: Easy, Normal & Hard (with Independent Progress)** — The story difficulty system has been expanded from a single Hard Mode toggle to three full tiers — **Easy**, **Normal**, and **Hard** — each tracking its own independent quest progress. Players can replay sagas at any difficulty without losing completion records from other tiers. HP and damage multipliers per tier are server-configurable. The difficulty selector is accessible directly from the Quest Tree screen; party leaders control the active difficulty for their group.
- **Saga enemies** have their own combat system, movesets, and effects (e.g. Yakon removes your transformation and takes damage from it).
- **Smarter Saga NPC Combat AI** — Story-mode (Saga) enemies now use one of three AI tiers — Simple, Intermediate, Advanced — governing how intelligently they fight. Higher tiers react in real time to blocking, stuns, knockdowns, transformations, and incoming ki blasts, choosing between melee combos, dashing in, teleporting, or ranged ki attacks for noticeably more dynamic boss fights.
- **New quest objective types:** Dimension (visit a dimension), Dragon Summon (summon a specific dragon from a specific ball set), and Skill (reach a skill level) — alongside Kill, Item, Biome, Coords, Interact, Structure, Talk-To.
- **New reward types: Alignment** (quests can shift you good/evil), **Ki Technique** (unlock a specific technique), and **Transformation** (unlock a specific form with configurable mastery) — alongside Item, Skill, TP, and Command rewards.
- **New quest conditions: Race and Class** — quests can now require the player to be a specific race or class before becoming available.
- **Parallel objectives** and **party-scaling** quests; **enemy preview** for KILL objectives; texture variants in objectives.
- **Quest Tree** with node statuses (Active/Available/Locked) and saga gating; tracked-quest HUD; toasts (started/failed/complete, objective complete).
- **Secret Sidequests** section that appears after you encounter them via NPCs. Sidequest categories: **Story, Collection, Capsule Corp**.
- **Bulma's mega R&D sidequest storyline** — ~40 chained side quests covering scouters, radars, Ki batteries, capsules, gravity chamber, anti-Ki cloak, Gete-tech, Frieza/Android/Namekian tech analysis, the Otherworld drive, and the Hyperbolic Time Chamber link, with its own **Gete-tech armor** (custom enchant effects).
- **Movies Saga Side Quests** — 10 new side quests added for the Movies saga, gated on story progress and covering the full arc of movie villains: **Vigil at the Lookout** (guard Kami's Lookout with Dende as Garlic Jr. breaks free), **The Spice Boys** (hunt Garlic Jr.'s followers across rocky wastelands with Popo), **The Frozen Lab** (salvage Dr. Wheelo's tech and circuitry from the snowy ruins with Bulma), **Roots of Might** (defeat Turles and gather Tree of Might saplings for Piccolo), **Slug's Shadow** (clear Lord Slug's Namekian soldiers from the plains with the Namek Elder), **Armored Squadron Mop-Up** (hunt Salza, Dore, and Neiz with Krillin), **Salvage from the Big Gete Star** (clear Gete guardian robots and recover Gete ingots for Bulma), **Cold Steel** (venture into frozen biomes, defeat Androids 14 & 15, and salvage their components for Bulma), **Galaxy Soldiers' Last Stand** (defeat Zangya, Bido, Bujin, and Gokua with Gohan at the tournament arena), and **Otherworld Rematch** (face Pikkon in the Otherworld arranged by King Kai, rewarding Senzu Beans).
- **Legendary Forms as Movies Saga Rewards** — Completing certain Movies Saga story objectives now grants **Legendary Form** unlocks for the player's race, tying access to these powerful race-specific transformations directly to saga progression.
- **Quest Menu: Reward Descriptions & Enemy Target Cycling** — The Quest Tree screen now displays a description of each quest's rewards in the preview panel. The enemy preview for Kill-type objectives also supports cycling through multiple target entries, making multi-enemy quest requirements easier to review before starting.
- **NPC Dialogue Lore** — Several NPC dialogue lines have been enriched with lore content, adding canon flavor and world-building depth to conversations with story characters.
- **Quest/Saga Command Permissions** — New granular permission nodes for quest and saga commands: `info`, `start`, `fail`, `track`, and `reset` actions each now have dedicated permission nodes, giving server operators fine-grained control over which players can perform each action.

---

## ⚖️ Alignment System

A **good / neutral / evil** alignment value (0–100) governs how story NPCs treat you.

- Bands: **Evil (≤40), Neutral (41–60), Good (>60)**.
- Each NPC has rules: default relation (friendly/neutral/hostile), min/max alignment to interact, and hostility thresholds.
- **Effect:** good-aligned heroes can interact with hero NPCs (Goku, Guru, Dende, King Kai…), while evil/low-alignment players get turned away or attacked. Some NPCs (e.g. Frieza, Cell…) instead require staying *below* a goodness threshold.
- **Masters Enma, Baba & Toribot ignore alignment rules** (so core progression is never blocked).
- Rules live in a **server-editable** `<world>/dragonminez/npcs/alignment_rules.json`; NPCs can also be overridden via NBT, and attacking an NPC can permanently mark it hostile to you.
- **Nimbus Alignment Requirements** — Riding the **Flying Nimbus** now requires an alignment score above 66 (Good-aligned). Riding the **Black Nimbus** requires alignment 66 or below (Evil or Neutral). Attempting to board the wrong cloud for your alignment displays a rejection message and denies mounting.
- **`/dmzalignment` Command** — New operator command to `set`, `add`, or `remove` alignment points (range: 0–100) for one or multiple players. Full permission-node support for self (`dmzalignment.*.self`) and others (`dmzalignment.*.others`) scopes.

---

## 🎮 RPG Classes & Stats

### Classes & Passives
New classes — **Berserker, Cleric, Tank, Paladin** — join **Warrior, Spiritualist, Martial Artist**. Each class has a unique, config-tunable passive:

- **Warrior** — landing combos builds stacks (up to ~5): +stamina regen per stack, armor penetration at max; decays over time, blocked hits reset it.
- **Martial Artist** — bonus strike damage to wounded targets (up to +25% as their HP drops).
- **Berserker** — stronger as you're hurt: big health-regen & crit-chance boosts below 66% / 33% HP.
- **Tank** — converts stamina regen into bonus HP regen and +25% healing received; both double at low HP.
- **Paladin** — party support: lifesteal share when allies deal damage, and redirects a share of allies' incoming damage to itself. Only activates for allies within **50 blocks**.
- **Spiritualist** — offensive ki support: reduced cooldowns on damage techniques, longer debuff durations.
- **Cleric** (formerly Enchanter) — healing/buff ki support: reduced cooldowns on heal techniques, longer buff durations.
- **Per-class TP-gain multipliers** — classes train stats at different rates.
- **Class Initial Stats & Minimum Stat Floor Updated** — Starting stats for every class have been rebalanced. The per-stat minimum floor has also been reworked to better reflect each class's intended role.

### Stats & progression
- **Six trainable stats:** **Strength (STR), Strike Power (SKP), Resistance (RES), Vitality (VIT), Ki Power (PWR), Energy (ENE)** — minimum floor of 5 and a configurable maximum.
- **Stats rewritten onto Forge attributes** for players *and* entities — better mod compatibility; stats now apply to NPCs/mobs.
- **Revamped leveling formula** with **max level & max stat caps**, a **TP-multiplier display**, and a server option to **cap by level instead of by stat value**.
- **New ways to gain TP** and a rewritten TP-gain calculation.
- **Defense/Resistance reworked** — damage reduction based on a resistance multiplier; **defense penetration** skill/enchant added.
- **Bonus stats** are now both flat *and* multiplicative.
- New stats info shown on the **character creation** screen.

---

## 🏃 Skills, Movement & Utility

- **New skills:** Instant Transmission (teleport), Sprint, Ki Infusion, Ki Protection, Defense Penetration, Friendly Fist; reworked Jump skill.
- **Flight rework** — new flight impulse/burst replaces dash, with momentum, aura boost, double-tap burst, and reduced camera roll. Fly speed tuned down (normal 0.85→0.7, sprint 1.5→1.25). Two flight modes: **Combat Fly** and **Search Fly** (toggle with Alt + F).
- **Utility menu** with right-click tooltips.

---

## 🩹 Stamina, Regen & Food

- **Stamina rework** — lower costs, higher regen, new regen multipliers; stamina costs for dash & blocking rebalanced.
- **Regeneration rework** — enchants can boost regen; regen scales with **saturation** (eating food matters).
- **Food regen for all foods** — every food now heals over time and stacks (with min/max hunger thresholds and whitelist/blacklist), not just hardcoded items and plain instant values.
- New mob effects: **Ki Regeneration, Stamina Regeneration, TP Multiplier, Mastery Multiplier**

---

### Dragon Balls, Radar & Wishes (datapack-driven rework)
- The **Dragon Ball, Dragon Radar, and dragon (Shenron-type) entity** systems were merged into one **extensible, datapackable** format.
- **Two ball sets:** Earth (7 stars, Overworld) and Namek (7 stars, Namek dimension), with configurable set count & spawn range.
- **Dragon Radars:** Earth Radar and Namek Radar (craftable from a radar chip + radar CPU), plus a new **Bi-Dimensional Radar** — a fused Earth+Namek radar obtained as a **quest reward (not craftable)** that works in both dimensions with extended range.
- **Dragons:** **Shenron** (Earth, 1 wish) and **Porunga** (Namek, 3 wishes), both summonable entities.
- **Wishes** are fully datapack-driven (loaded from world config), reworked and synced; packs/addons can add/override wishes per dragon. Retains types like Skill wish and Passive Reset wish.
- Fixes: dragon balls no longer fail to spawn on chunk load; wishes load correctly from world config.

### Space Pods (planet travel — reworked destinations)
- A datapack-driven **destination system** lets travel destinations be added/overridden with **unlock rules** (combine with AND/OR/NOT; check quest completed, level, stat minimum, race, player tag, or visited dimension).
- **Default destinations:** Overworld & Namek (always), **Otherworld** (via the *bulma_otherworld_drive* sidequest), **Hyperbolic Time Chamber** (via *bulma_time_chamber_link* sidequest). Supreme/Kai Planet, Cereal Planet, and **Beerus' Planet** (dmzsuper add-on) are currently locked — likely placeholders for future content **(?)** 

---

## 👥 Party System (Rework)

- **Create a Party / Invite / Accept-Reject / Kick / Leave / Disband**, with **Leader** and **Member** roles, invite toasts, and a multiplayer-readiness check.
- **Shared quest progress**; quests can require **all party members** to meet conditions before starting; party-scaling difficulty.
- **New party menu**; parties persist across world sessions and handle disconnections.
- Party members are friendly (no friendly-fire) unless **party PvP is toggled** via `/dmzparty`.
- **Fusion Party Management** — When two players fuse, their party memberships are now automatically managed: each player's current party state is snapshotted before fusion begins, and both are united into a single shared party for the duration of the fusion. Upon defusion, each player is cleanly restored to their original party (or removed if they had none before), preserving party leadership and syncing quest progress correctly throughout the process.
- **Fusion Name Differentiation** — Fused characters now get different display names depending on the fusion method. Potara fusion and Metamoran (Fusion Dance) each use a distinct name-blending formula, resulting in different fusion names for the same pair of players depending on which method they used.

---

## 🕹️ Minigames (Training)

A **Minigames** menu (master training) with five games — **Rhythm** (4-arrow note-hitting), **Control**, **Shadow Boxing (Memory)**, **Precision**, **Gravity** — awarding TP with level/score/timer systems. The rhythm minigame was reworked; known minigames are tracked per character.

---

## 🎨 Character Customization & Cosmetics

- **New character creation flow** with **per-race animated panorama backgrounds** (Human, Saiyan, Namekian, Bio-Android, Frost Demon, Majin + a Roshi background).
- **Remade race selection screen**; class stat info shown on creation.
- **New Hair Editor** — full styling tool (Base/SSJ/SSJ2/SSJ3 styles, length/width, X/Z axis & bend, physics, mirror), **zoom**, improved rendering performance, and **import/export hair codes** (Full Code / Style Code). **16 new hairstyles** with reworked textures & colors.
- **Body customization:** layers & face customization for custom races, eyes/nose/mouth/tattoo types, body colors, aura color, **chest size slider**, hair-base toggle, new buffed female body model.
- **Aura overhaul** — new aura type, multi-color gradient auras, auras on entities/NPCs, aura preview in customization, dark-color aura support.
- **Cosmetic/armor rendering** — full damaged-armor support, fixed cape armor models, hair rendering with cosmetic/helmet armor (whitelistable helmets), and compatibility with third-party player renderers/layers.
- **`/dmztail` Command** — New command to `cut` or `grow` a Saiyan tail on eligible race characters. Saiyan players can use it on themselves by default (`dmztail.self` permission); targeting other players requires `dmztail.others`. A katana slash sound plays when cutting a tail.
- **`/dmzhalo` Command** — New admin command to force-enable (`on`) or disable (`off`) the halo cosmetic on any player, independently of their alive/dead status. Controlled by `dmzhalo.self` and `dmzhalo.others` permission nodes.
- **New Hairstyle Added** — A new hair option has been added to the Hair Editor and is available for all races.

---

## 🖥️ UI / HUD / Quality of Life

- **New DMZ font** with smooth rendering across all interfaces and broader localization coverage.
- **Revamped tooltip system** — custom bordered tooltips, attribute/enchant coloring, weapon-range tooltips, merged attribute display, scrollable tooltips, and new attribute/effect icons.
- **HUD:** implemented new Technique HUD + technique hotbar, charge/overcharge overlay, beam-clash overlay, tracked-quest HUD, animated stat numbers, "always-visible values" option.
- **Dynamic menu music** on title/pause screens; custom branding.
- **Discord & Patreon buttons** on title/pause screens.
- **In-game version checker** with an update-available toast/popup linking to CurseForge.
- **Ideal Weight on V Menu** — Hovering over the Gravity stat on the V (character stats) menu now displays a tooltip showing the player's **Ideal Weight** value.
- **Form Drain & Regen on V Menu** — The V (character stats) menu now displays drain and regeneration values for each transformation form. Hovering over a form's stats shows a tooltip with the exact drain and regen amounts, making it easier to plan resource management while transformed.
- **`/dmz` commands:** `tech` (add/remove techniques), `config` (live config edit), `debug` (export STATS/CHARACTER/TECHNIQUES), `reload`; `/dmzparty` fixed. **New standalone commands:** `/dmzalignment` (set/add/remove alignment), `/dmztail` (cut/grow Saiyan tails), `/dmzhalo` (toggle halo cosmetic), `/dmztech experience` (grant technique XP).

---

## 🎵 Audio & Visual Effects

- **~64 new sound files**: menu/series music discs, dynamic menu music, weapon swing/slash/stab sounds, sword sheathe/unsheathe, afterimage, transformation and special-move SFX.
- **~33 shader programs**: aura rendering, transformation outline/bloom post-processing chain, impact frames, ki-attack & lightning shaders, beam-clash visuals.
- **~90 models** and **~50 animations** for new forms, ~60 saga NPCs, and the new combo combat.
- **~250+ new textures** and new mod app icons.

---

## 🐛 Notable Bug Fixes

- **Multiplayer:** health-value mixin fix; synced-animation fix; combat-logout handling; fixed Saiyan Zenkai passive timer being shared across all players (now tracked per-player UUID).
- **Combat:** attack-while-blocking, PvP/team handling; fixed attack speed producing unintended near-instant cooldowns at very low stat values; fixed stunned entities incorrectly receiving damage events; fixed melee strike attacks being usable on Master NPCs; iterative combat animation improvements (timing, Majin geometry, sword parameters); fixed SparksFX effects not triggering correctly; fixed KiMediumBall hitbox not registering hits reliably (collision detection corrected for both Ki blast and Ki wave entities); further refined sword attack animation parameters across wooden, iron, diamond, and netherite sword tiers.
- **Stun:** fixed players and entities becoming permanently stunned — the infinite stun state is now correctly cleared after the stun duration expires or on KO.
- **Ki Attacks:** fixed ki attacks incorrectly healing enemies — ki healing now correctly applies only to allied targets, for both players and Saga entities; fixed Oozaru Beam not functioning correctly.
- **Input:** fixed Alt and Ctrl modifier keys overriding skill keybindings; improved skill key normalization to prevent unintended conflicts.
- **Transformations:** saga entities no longer transform on `/kill` or void damage; fixed form-slot & kaioken-slot; fixed sleep animation not playing; fixed Ultimate form aura color; fixed hair and skin color desyncing from aura tint during transformation sequences; fixed stack form mastery XP being credited to the base form data instead of the active stack form; fixed form preview not rendering for custom/addon forms in the Skills screen; fixed stale `old_[config]` form config files appearing in the Skills Menu and Form selection; fixed K.O. not removing the player's active transformation — being knocked out now correctly reverts the player to base form.
- **Rendering:** Oozaru armor/scouter/Potara/FOV, hair with cosmetic armor, sword/accessory models, left-hand item offset; aura not rendering when a Ki attack was simultaneously on screen; Majin mark render order; fixed render buffer in `DMZRacePartsLayer` not being reset in some cases; fixed armor rendering on custom race models; fixed head rotation not animating; fixed Fat Majin cape not rendering while wearing armor; fixed custom race models and textures failing to load; fixed model scaling when using stack forms alongside base transformations; restored fused character aura and body colors (FusionLogic rewrite); fixed MetalCooler and MetalCooler Core textures not rendering; fixed Super Janemba armor not rendering correctly; fixed SSJ4 skin layer not rendering on custom player models; fixed Saiyan body types missing from character options; fixed custom race model layers and face features failing to render correctly; fixed fused character colors not restoring after defusion; fixed custom SSJ4D/GT model support not loading correctly; fixed Fat Janemba armor rendering incorrectly.
- **Hair & Physics:** fixed hair physics not simulating correctly during jumping and falling; ki charge animation now correctly progresses through its stages.
- **Landing:** improved landing trigger detection distance for more reliable landing event detection; fixed player head rotation during the landing animation.
- **UI/Screens:** fixed button overlap on the Wishes, Space Pod, and Instant Transmission screens.
- **Stats/regen:** fixed NaN on damage/heal events, mastery/TP gain on 0-damage hits, max-mastery bug, regen on level-up.
- **Quests:** loading from world config, start cooldowns, kill/unlock logic, biome-start validation; fixed quest progress not sharing correctly between party members; fixed potential null pointer exceptions in quest start and turn-in logic; fixed incorrect entity IDs used in Movies saga boss encounter objectives (Garlic Jr., Lord Slug Giant form, Cooler 5th form, Gete robots, Meta-Cooler, Super Android 13, Broly variants, Bojack's individual allies now tracked separately, Full-Power Bojack, Bio-Broly Giant form, Hirudegarn forms, Fat & Super Janemba); fixed TP rewards for Movies Saga quests being incorrectly balanced.
- **NPCs/Masters:** fixed master relations not properly recognizing player–master associations; fixed Masters entity stat scaling referencing the wrong entity data accessor; quest NPCs spawned via `/questnpc` are now anchored to their spawn position, invulnerable to environmental damage, and persist correctly across server restarts; fixed Movies Saga boss entity sizes being incorrect (entity hitboxes and model scales corrected).
- **Misc:** fixed the immortal training (shadow) dummy; fixed shadow dummy massively inflating player stats; fixed config crash caused by stale cache data (cache now clears and sync restores on startup); fixed Red Ribbon Army skin fetching via the Mojang API now using a bounded thread pool to prevent resource exhaustion.

---

## ⚖️ Balance Changes (Summary)

- Defense/resistance reworked; damage reduction by resistance multiplier; defense penetration added. Transformations now matter a lot more on defense. You're no longer either immortal or one-shoted; instead, your survivability scales more smoothly with your stats and forms.
- Critical hits introduced (chance + damage).
- Bonus stats now flat + multiplicative; bonus stat system now tracks DEF and STM independently (replaces unified RES bucket).
- Max level/stat caps + revamped leveling formula; multiple new TP sources & per-class TP multipliers.
- Ki attack damage delivery reworked by attack type: contact types deal full damage on impact; area/explosion types deal per-hit damage.
- Removed the **Gete Plating** enchantment (previously reduced flat incoming damage by 4% per level, up to 20%); it no longer exists as a craftable or obtainable enchant. The Bulma R&D sidequest book reward that previously included it has been updated accordingly.
- **Merus Laser & Blaster Cannon** now have limited durability (250 and 200 uses respectively); each shot consumes one durability point and the item breaks at zero.
- **Mini Buu** entity attribute values have been corrected.
- TP costs for all learnable skills have been rebalanced across the board.
- Skill offerings have been redistributed and updated among all Master NPCs.
- **Ki Weapons significantly nerfed** — Ki weapon base damage and scaling values have been substantially reduced; ki weapons were considerably overperforming relative to other combat options.
- **All race transformation forms rebalanced** — Transformation stat multipliers for every non-Saiyan race have been recalibrated to align with Saiyan form power scaling, harmonizing progression feel across all races.
- **Legendary & Android Forms Rebalanced** — Stat multipliers for Legendary forms and Android forms have been individually recalibrated across all applicable races.
- **Gravity Device Hard Stop Threshold** — The Gravity Device now enforces a hard stop threshold; the effective gravity multiplier is capped at a configurable maximum even when higher values are entered, preventing unintended runaway training conditions.
- **Paladin Passive Range Capped at 50 Blocks** — The Paladin class passive (lifesteal share and incoming damage redirect) now only activates for allies within a **50-block** radius. Allies farther away are not affected.
- **Movies Saga TP Rewards Rebalanced** — Training Point rewards for Movies Saga quests have been adjusted to be more consistent with the overall progression curve.
- **Battle Power Precision** — Player Battle Power (BP) is now tracked as a floating-point value, enabling more precise BP calculations and display (entities retain integer BP).

---

## 🔧 Technical / Modding & Compatibility

- **New required dependency:** **Curios API** (`[5.14.1+1.20.1,)`) — powers Head Tech, Weights, and accessory slots.
- **Bumped:** GeckoLib `4.8.2 → 4.8.3`, TerraBlender `3.0.1.7 → 3.0.1.10`. (Forge/MC unchanged.)
- **Energy Cable & Fuel Generator Improvements** — Energy cable internal buffer capacity increased from 50 to 150 SE; transfer rate tripled from 5 to 15 SE per tick. The Fuel Generator's generation tick rate was also corrected — it was only generating energy on 1 of every 4 ticks and now correctly generates on 3 of 4 ticks, effectively tripling output.
- **Incompatibilities:** Better Combat, Epic Fight, Legendary Tooltips — not compatible. A clear **`IllegalStateException` splash screen** now displays at game load when any of these mods are detected alongside DragonMineZ, replacing the previous silent or hard-to-diagnose crash.
- **Structure Spawn Planner Refactored** — The structure placement system has been substantially rewritten. A terrain and biome **sample cache** now avoids redundant world queries during structure placement checks, significantly reducing world generation overhead. The structure async resolver was also reworked alongside this change for improved reliability.
- **New config files:** CombatConfig, TechniqueConfig, TrainingConfig (config grew 10→13), with a refactored config manager + live `/dmz config` editing. Notable toggles: Impact Frames, right-side technique hotbar, always-visible HUD values, Blocking/Parrying/Perfect Evasion, kill-on-combat-logout, live Crowdin translations.
- **Config Version: Double Precision** — `configVersion` is now a `double` (floating-point) instead of an `int`, enabling finer-grained version tracking for config migration and future sub-version increments.
- **Addon/modding API** — lifecycle hooks & quest events for addons; datapackable Dragon Balls, Space Pods, Namek biome source, wishes, and weapon attributes; custom techniques; **addon armor mod namespace support**
- **~30 new network packets** backing the new interactive systems (combat, ki attacks, quests, party, config, space pods, beam clash).
- **~31 mixins** focused on compatibility & multiplayer stability (player list, disconnect screen, health sync, custom menus/branding, camera/flight, tooltip compat, enchantment handling, ItemStack/LivingEntity crash fixes).
- **PackSquash** integration compresses bundled textures/sounds → **smaller download**.

---

*Generated from analysis of the `v2.1` vs `main` branch diff (489 commits) — code, assets, lang strings, config, and commit history.*  
*Recent build changes (June 3–27, 2026) added from commits on the `v2.1` branch.*
