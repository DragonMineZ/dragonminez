# Patch Notes — v2.1.1

---

## New Content

### World Generation

- **Capsule Corp Villager Structure:** A Capsule Corp-themed structure now spawns inside vanilla Minecraft villages, blending the Dragon Ball world into regular Minecraft exploration.
- **Gamerule — `allowKiGriefingMasterStructures`:** Added a new gamerule that controls whether ki attacks can destroy or damage master training structures. Servers can now protect these key structures from ki attack collateral damage.

*(by @yuseix300)*

### Character Customization

- **Namekian — New Body Types:** Added two new body types for the Namekian race, each with 4 layered textures, expanding character creation options.
- **Namekian — New Eye Styles:** Added 2 new eye styles (eyes 3 & 4) with 4 color variants each for Namekian characters.
- **Majin Antenna & Frost Demon Horns:** Two new race part accessories are now available for character customization.
- **Race Parts Textures:** Updated and expanded the race parts texture system.
- **Broly — Texture Variants:** Broly NPC now has multiple texture variants for both his Super Saiyan and Legendary Super Saiyan forms.

*(by @yuseix300)*

### Music Discs

- **Music Discs in World Loot:** DragonMineZ music discs can now be found in the world via the global loot modifier system — they're discoverable in regular Minecraft loot sources.

*(by @Shokkoh)*

### Armor

- Added item inventory textures for the following armor sets: **Capsule Corp**, **Cooler Soldier**, **Gero**, **Gilgamesh** (including worn armor layers), **Great Saiyaman 2**, **Raditz**, **Subaru Natsuki** (base and Arc 6 variant).

*(by @yuseix300)*

---

## Story & Quests

### Permanent Difficulty
- Story difficulty can now be set permanently. A new **dragon wish** (`Change Difficulty`) allows players to permanently switch their story difficulty via the Dragon Balls at any time.

*(by @Shokkoh)*

### New Dragon Wishes
- **Change Difficulty** — use the Dragon Balls to permanently change your story difficulty.
- **Reset Story** — use the Dragon Balls to reset your story progress and start the saga from the beginning.

*(by @Shokkoh)*

### Quest Progress Tracking
- Overhauled the quest tree screen with improved visual progress tracking for sagas and individual quest objectives.

*(by @Shokkoh)*

### Per-Player Reward Claims
- Quest and wish item rewards are now tracked individually per player. In a party, each member independently receives and claims their own reward — no more shared one-time claims that only one player could get.

*(by @Shokkoh)*

### Entity Tags for Kill Objectives
- Kill objectives now support entity tags, allowing quests to group multiple entity types under a shared target. Frieza soldiers have been grouped under a dedicated tag so quests correctly track kills across all their variants. The quest enemy preview UI reflects this grouping as well.

*(by @Bruneitor123)*

### `canTransform` on Kill Objectives *(Addon/Developer API)*
- Kill objectives in quest JSON files now support an optional `canTransform` field. This allows addon and quest developers to require a player to be in a specific transformation state when the kill objective is completed.

*(by @Bruneitor123)*

### NPC & Mission Cleanup
- Removed references to unused NPC entries. Added missing translation keys for mission objective text.

*(by @Bruneitor123)*

---

## Combat & Balance

### Defense Formula Update
- Adjusted the defense formula to perform more consistently across damage types — particularly against **strike attacks**, in **PvP**, and against **story bosses**. The previous formula undervalued defense against these scenarios.

*(by @Shokkoh)*

### Mastery Gain Scaling
- Technique mastery earned in combat now scales with the amount of damage dealt. Landing stronger hits rewards proportionally more mastery progress.

*(by @Shokkoh)*

### Ultimate Techniques Now Scale with Mastery
- Fixed ultimate techniques not factoring in mastery level when calculating their power. Ultimate skills now scale with mastery the same way regular techniques do.

*(by @Shokkoh)*

### Story Boss AI — Skill Cooldown & Cast Time
- DBSagas boss entities now have a proper cooldown between skill uses and a cast time before executing each skill. Bosses no longer chain abilities back to back without pause, making encounters more readable and fair.

*(by @Shokkoh)*

### Ki Attacks No Longer Destroy Dragon Balls
- Fixed ki projectiles accidentally destroying Dragon Ball entities on contact.

*(by @Shokkoh)*

### Ozaru Fist & Dragon Fist Damage
- Corrected the damage values for Ozaru Fist and Dragon Fist attacks.

*(by @yuseix300)*

### Training Minigames Rebalanced
- Rebalanced the Control, Memory, and Rhythm training minigames. Timing windows and scoring thresholds have been adjusted for a more consistent training experience.

*(by @Shokkoh)*

### Stamina Costs
- Fixed stamina not being correctly consumed when entering or holding a transformation.
- Fixed stamina drain being incorrectly multiplied when a single attack struck multiple targets at once.

*(by @Shokkoh)*

### Ki Attack Self-Damage
- Players can no longer deal damage to themselves with their own Ki attacks or blasts.

*(by @Shokkoh)*

### HTC Double TP Bonus
- Fixed the Hyperbolic Time Chamber granting Training Points from two independent sources simultaneously, which resulted in doubled TP gain per session.

*(by @Shokkoh)*

---

## Physics & Movement

### Gravity & Weight System Rework
- Fully reworked the gravity calculation and weight handling logic. Introduced a dedicated `GravityStateSync` to properly synchronize gravity zone state from server to clients.
- Gravity device blocks now correctly notify players entering and exiting their zone.
- Fixed the gravity device room recomputation logic — rooms are now recalculated correctly when blocks are placed or removed inside the zone.
- New configurable gravity parameters are available in the server config.
- Multiple edge-case bugs tied to the old gravity system have been resolved in this rework.

*(by @Shokkoh)*

### Flight with Ki Attacks
- Fixed players being unable to maintain flight properly while using ki attacks.

*(by @yuseix300)*

### Combat Flight Speed Under Gravity
- Fixed combat flight speed not correctly accounting for the active gravity multiplier when inside a gravity zone.

*(by @Shokkoh)*

---

## Skills & Techniques

### Instant Transmission
- Reworked Ki cost handling for Instant Transmission to be accurate and consistent.
- Added a fallback feedback message displayed to the player when no valid teleport destination can be found.

*(by @Shokkoh)*

---

## UI & Controls

### Utility Menu — Radial Rework (X Menu)
- The utility menu (opened with X) has been completely reworked into a **radial wheel interface**. Actions and options are now organized into interactive nodes — forms, ki actions, ki weapons, movement options, racial skills — with a cleaner and more intuitive layout.
- Addon developers can register custom buttons directly into the radial menu.
- Added a **Limit Release** dedicated button to the radial menu, letting players release their power limit directly from the wheel without opening other menus.
- Added icons to radial menu nodes.
- Fixed the display position of the "more" options panel; added frozen (non-interactive) placeholder nodes for layout stability.

*(by @Shokkoh)*

### `/dmzmastery all` Command
- The `/dmzmastery` admin command now accepts `all` as a target, instantly mastering every technique at once instead of requiring individual entries.

*(by @Shokkoh)*

### Menu Tab Shortcuts
- Keyboard shortcuts can now be used to quickly switch between tabs in the character menu.

*(by @Shokkoh)*

### Key Modifier Support
- Improved detection and handling of key modifier combinations (Shift, Ctrl, Alt) for mod keybinds.

*(by @Shokkoh)*

### Non-Latin Alphabet Support
- The mod now correctly renders and processes text containing non-Latin characters (Cyrillic, Japanese, Arabic, Korean, etc.) in player names and UI elements.

*(by @Shokkoh)*

### Overlay — Hover Item Fix
- Fixed an issue where the DMZ HUD overlay would render item hover tooltips through other UI layers during overlay superposition.

*(by @Bruneitor123)*

---

## Bug Fixes

### Majin — Alignment Lock
- Fixed a bug where Majin characters could drift away from 0 (neutral) alignment. Majins are now correctly locked at 0 alignment as intended.

*(by @Shokkoh)*

### Structure Spawning & Foundation
- Fixed an issue where certain structures failed to spawn correctly in the world.
- Fixed the foundation layout on newly added structures.

*(by @Bruneitor123, @Shokkoh)*

### Server Config Sync
- Enforced server-side config synchronization to all clients on login and on `/dmzreload`. Stats sync packets are now batched for better performance.

*(by @Shokkoh)*

### Missing Network Packets
- Fixed missing network packets that caused certain client-server actions to silently fail.

*(by @Shokkoh)*

### Armor — A14 Texture
- Fixed an incorrect texture on the A14 armor set's body layer.

*(by @yuseix300)*

### Body Type — Male2 Texture
- Fixed a texture issue with the male2 body type.

*(by @yuseix300)*

### Head Position
- Corrected an incorrect head position on a character model.

*(by @yuseix300)*

### Texture Variants Across Transformations
- Fixed skin texture variants (markings, color overlays, etc.) not being applied consistently when entering transformations or when previewing them in the character menu.

*(by @Bruneitor123)*

### DBSagas — Item In Hand Layer
- Fixed the `ItemInHandLayer` renderer for DragonBall Saga NPC entities. Items held by story NPCs now display correctly.

*(by @yuseix300)*

### Tail Slot on Other Races
- Fixed the tail display slot incorrectly appearing or interfering with non-Saiyan races that do not have a tail.

*(by @Shokkoh)*

### Custom Human Models
- Fixed the renderer rejecting custom human model definitions supplied by resource packs or addons.

*(by @Shokkoh)*

### Infinite Stat Growth
- Fixed a bug in the Dynamic Growth system where player stats could grow infinitely under certain conditions due to missing or incorrect growth cap validation.

*(by @Shokkoh)*

### Config Updating
- Reworked the config version migration logic. Outdated configs are now upgraded more reliably without risking data loss or silent failures.

*(by @Shokkoh)*

### Curios API — Essential Mod Crash
- Refactored Curios API usage into a centralized utility class (`CuriosUtil`), resolving crashes that occurred when the **Essential** mod was installed alongside DragonMineZ.

*(by @Shokkoh)*

### Arclight — LivingEntity Crash
- Fixed a server crash on **Arclight** software caused by incompatible `LivingEntity` handling in combat event processing.

*(by @Shokkoh)*

---

## Contributors

| Contributor | Area |
|---|---|
| **@Shokkoh** | Combat, balance, boss AI, physics, story systems, UI, networking, bug fixes |
| **@yuseix300** (Yuse) | Art, models, armor, race customization, world generation |
| **@Bruneitor123** (Bruno V.) | Quest systems, developer API, structures, client bug fixes |

---

*Patch notes compiled by Claude — [DragonMineZ/dragonminez](https://github.com/DragonMineZ/dragonminez/tree/v2.1.x)*
