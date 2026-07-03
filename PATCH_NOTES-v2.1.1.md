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
- **Form Skin Tint — `formTint` Config Field** *(Addon/Developer API)*: Forms can now specify a `tintColor` (hex) and `tintIntensity` (0.0–1.0) in their form JSON config to tint the player's skin, hair, and race parts while in that form. Replaces the old hardcoded Kaioken-only red tint. Kaioken (x2–x100), Shiyoken, Shin Shiyoken, and Chou Shiyoken all use this new system. User-defined form files are automatically version-migrated on load.

*(by @yuseix300, @Shokkoh)*

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

### Raditz Sidequest — Kill Objective Fix
- Fixed the "Saiyan Biology Sample" Bulma sidequest where the Raditz kill objective used the wrong tracking method. Kills now register correctly.

*(by @Bruneitor123)*

---

## Combat & Balance

### Defense System Rework
- Overhauled how defense values are stored, computed, and displayed. Defense stats are now stored in **flat mitigation units** — the displayed value directly represents damage reduction per hit, already including your active form's DEF multiplier.
- Existing worlds **auto-migrate on first load** (config version 21.2): all `defenseScaling` values in `stats.json` are multiplied by 0.12 and re-saved. Combat math adjusts to match, so effective damage reduction stays comparable to before.
- The `k_factor` minimum in the post-mitigation formula changed from 100.0 to 12.0, normalizing defense behavior at all stat levels.
- The old "transform DEF divider" (which applied a secondary reduction per form) is removed; form DEF contribution is now handled entirely through the higher `defMultiplier` values in form configs (see below).

*(by @Shokkoh)*

### Form DEF Multipliers Buffed
- All form defense multipliers have been increased by approximately 25% of their bonus-over-1.0. Examples: Kaioken x100 goes 2.0 → 2.25, SSJ4 goes 2.5 → 2.875, Chou Shiyoken goes 3.1 → 3.625. This compensates for the removal of the old transform DEF divider, maintaining and slightly improving protection while in forms across all races.

*(by @Shokkoh)*

### Form TP Costs Rebalanced (~2.6× Increase)
- Transformation unlock TP costs have been significantly increased across all races — approximately 2.6× across all tiers. For example, Human super forms go from 8,000 / 16,000 / 25,000 / 40,000 to 21,000 / 42,000 / 65,000 / 104,000. This makes the progression to unlocking transformations longer and more deliberate.

*(by @Shokkoh)*

### Health Regen Nerfed
- Baseline HP/5 regeneration has been reduced by approximately 75% for all classes. VIT-based HP/5 scaling is also slightly reduced. Health regeneration is now much slower and should no longer trivialize recovery between engagements.

*(by @Shokkoh)*

### Ki/Energy Regen Buffed
- ENE-based energy regeneration (`ep5EneScaling`) has been doubled for all classes. Players investing in the ENE stat will regenerate Ki significantly faster than before.

*(by @Shokkoh)*

### Meditation Skill Buffed
- The Meditation technique now provides a 50% stronger regen bonus per level — each level grants +7.5% (was +5%) to both stamina and energy regeneration.

*(by @Shokkoh)*

### STR Scaling Nerfed
- Strength stat scaling has been slightly reduced across most classes (examples: Warrior 1.6 → 1.4, Berserker 1.9 → 1.7, Martial Artist 1.0 → 0.8, Tank 0.8 → 0.6). This brings melee damage growth more in line with the overall combat system.

*(by @Shokkoh)*

### TP Cost Formula Rebalanced (Mid-to-Late Game)
- The Training Points cost formula for stat purchases has been reworked for mid-to-late game. A curved formula with a "knee" at 5% of max stats replaces the old linear formula — costs scale more steeply in the late game, rewarding earlier investment.

*(by @Shokkoh)*

### Progression TP Gain Multiplier
- A new **Progression TP gain multiplier** scales TP rewards based on how far along your stat progression you are. As your stat cost grows relative to the configured maximum, you receive a bonus on all TP earned — up to a configurable cap (default: +50% at max stat cost). This is visible in the TP multiplier tooltip as a new "Progression" entry. Configurable via `increaseTPGainRelativeToTPCost` in the server config (default `0.5`).

*(by @Shokkoh)*

### Party Mob Scaling Rework
- Enemy scaling in party play has been changed from an exponential formula to a **linear per-player model**. Each additional party member now adds a configurable percentage to enemy HP and damage (defaults: +25% HP, +10% damage per member). New server config fields: `enemyHealthPerPartyPlayer` and `enemyDamagePerPartyPlayer`.

*(by @Shokkoh)*

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

### Ki Fall Damage Negation
- Players now automatically use Ki to negate fall damage. Ki is consumed at a rate of **3 Ki per point of fall damage**. If you have enough Ki, fall damage is fully negated; if not, damage is partially reduced proportional to remaining Ki, and all remaining Ki is drained. Stats are synced to the server immediately.

*(by @Shokkoh)*

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

### Defense Stats Display Overhauled
- The defense stat panel and tooltip in the Character Stats screen have been cleaned up. The "Flat Mitigation" and "Power Divider" tooltip lines are removed — the displayed defense value now directly shows flat damage mitigation including the active form's DEF multiplier.
- A new **"Stamina per hit"** line is now shown in both the stats list and hexagon panel.
- TP multiplier values now display with **two decimal places** throughout the stats screen for greater precision.

*(by @Shokkoh)*

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

### Aura Sound — Permanent Aura State
- Fixed aura loop sounds not playing or stopping incorrectly during **permanent aura** states. Both the aura sound handler and the aura loop sound tick now check for permanent aura in addition to the regular active aura flag. Several sound files were also updated.

*(by @Shokkoh)*

### Forms Not Appearing in Transformation Menu
- Fixed certain forms not showing in the transformation selection menu. The mastery prerequisite check now correctly considers both regular form masteries and stack form masteries when determining if a form's unlock requirements are met.

*(by @Shokkoh)*

### Kikono Station & Fuel Generator — Drops & Required Tool
- Fixed the Kikono Station and Fuel Generator not dropping correctly and requiring the wrong mining tool. Kikono Station now correctly requires a diamond tool. Fuel Generator is now tagged as mineable with a pickaxe. Gravity Device was moved from `needs_iron_tool` to `needs_diamond_tool`.

*(by @Shokkoh)*

### Particle Effects — Null Return Fix
- Fixed a crash and visual issue where all 17 particle types (aura, ki blast, ki explosion, ki lightning, dust, rock, punch, and others) could return null during rendering, causing them to fail silently or crash the client.

*(by @Shokkoh)*

### Aura Layer Renderer
- Fixed a rendering issue in the aura layer renderer that caused incorrect aura display.

*(by @Shokkoh)*

### Weapon Registry — Invalid Entry Handling
- The weapon registry now handles missing or invalid weapon registrations gracefully with fallbacks, preventing potential crashes from unregistered or malformed weapon entries.

*(by @Shokkoh)*

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
