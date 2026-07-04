# Patch Notes — v2.1.1

---

## New Content

### World Generation

- **Capsule Corp Villager Structure:** A Capsule Corp-themed structure now spawns inside vanilla Minecraft villages, blending the Dragon Ball world into regular Minecraft exploration.
- **Gamerule — `allowKiGriefingMasterStructures`:** Added a new gamerule that controls whether ki attacks can destroy or damage master training structures. Servers can now protect these key structures from ki attack collateral damage.
- **Structure Spacing Server Config:** Two new server config fields let operators tune how far apart repeating structure copies spawn: `structureSpacing` (default 6000 blocks; region size — lower means you encounter structures sooner while exploring) and `structureSeparation` (default 2000 blocks; minimum buffer between placements). Both are configurable at runtime without a datapack override.

*(by @yuseix300, @Shokkoh)*

### Character Customization

- **Namekian — New Body Types:** Added two new body types for the Namekian race, each with 4 layered textures, expanding character creation options.
- **Namekian — New Eye Styles:** Added 2 new eye styles (eyes 3 & 4) with 4 color variants each for Namekian characters.
- **Majin Antenna & Frost Demon Horns:** Two new race part accessories are now available for character customization.
- **Race Parts Textures:** Updated and expanded the race parts texture system.
- **BioAndroid — New Body Types:** Added 2 base body types, 2 Semi-Perfect body types, and 2 Perfect body types for the BioAndroid race, each with 5 layered textures, expanding character creation options for BioAndroid players.
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

### Ki Blast — Movement Slow Effect
- Entities struck by a ki blast now receive the **Ki Slow** status effect for 1.5 seconds, reducing their movement speed by 90%. This creates a brief window of pressure after landing a ki projectile and makes ki combat feel more impactful.

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

### Aura Layer Renderer
- Reworked the aura layer rendering system to support per-layer alpha. Each `AuraLayer` now carries an `alpha` value, enabling smooth fade-in transitions when charging toward a form whose aura uses a different layer slot than the current one. Previously, cross-layer aura transitions could cause an abrupt swap; the incoming layer now fades in proportionally to charge progress while the current layer remains visible.
- Fixed the charge-state logic for stack forms: when the target form's stack layer matches the base layer, color is interpolated in-place; when it differs, the new layer blends in by alpha. Both the GUI aura and the in-world aura pulse draws now respect per-layer alpha.

*(by @Shokkoh)*

### Forms Not Appearing in Transformation Menu
- Fixed certain forms not showing in the transformation selection menu. The mastery prerequisite check now correctly considers both regular form masteries and stack form masteries when determining if a form's unlock requirements are met.

*(by @Shokkoh)*

### Form Preview — Head Bones & State Restoration
- Fixed the form preview in the Character Customization and Skills screens where the player model's state (rotations, active form, stack form, pose stack) was not guaranteed to be restored if a rendering error occurred mid-frame, potentially leaving head bone positions incorrect after closing the screen.
- All model state cleanup is now inside the `finally` block, ensuring restoration even on rendering exceptions.
- Fixed stack form preview not being applied correctly: the preview now clears both the active form and active stack form before applying the previewed form, and correctly routes to either a regular or stack form based on the form group type.

*(by @Shokkoh)*

### Story Bosses — Stun Now Fully Respected
- Fixed DBSagas boss entities not properly honoring the Stunned status effect. Stunned bosses now immediately stop navigating, cancel any in-progress cast or combo, and are blocked from initiating melee attacks, new combos, skill casts, or brain decisions until the stun expires. Stunned entities also cannot deal melee damage at all.

*(by @Shokkoh)*

### Quest Reward — Transformation Unlock
- Fixed `TransformationReward` quest rewards not granting the required skill level for the rewarded form. Completing a quest that awards a transformation now also ensures the player's relevant transformation skill (e.g. `superforms`, `legendaryforms`) is set to at least the level required to access that form — preventing forms from being awarded but remaining locked.

*(by @Bruneitor123)*

### Kikono Station & Fuel Generator — Drops & Required Tool
- Fixed the Kikono Station and Fuel Generator not dropping correctly and requiring the wrong mining tool. Kikono Station now correctly requires a diamond tool. Fuel Generator is now tagged as mineable with a pickaxe. Gravity Device was moved from `needs_iron_tool` to `needs_diamond_tool`.

*(by @Shokkoh)*

### Particle Effects — Null Return Fix
- Fixed a crash and visual issue where all 17 particle types (aura, ki blast, ki explosion, ki lightning, dust, rock, punch, and others) could return null during rendering, causing them to fail silently or crash the client.

*(by @Shokkoh)*

### Structure Spawning & Foundation
- Overhauled the structure spawn planning system. Structure positions are now resolved once and saved to the world data file (`dragonminez_structure_plan`) — subsequent server starts load positions from disk instantly, skipping the biome search entirely. The overworld resolves synchronously at first world creation so near-spawn structures (Goku's house, Babidi, etc.) are placed before the first spawn chunk generates; other dimensions (Namek, Sacred Kai) resolve in the background.
- Fixed structures on the Namek dimension causing chunk generation to stall: the biome search now skips dimensions whose biome source cannot produce the required biome, preventing the ring-scan from hunting for biomes that can never appear (e.g. a rocky-biome structure search running in Namek).
- Disabled aquifer generation for the Namek dimension (it was incorrectly enabled), reducing chunk generation cost significantly.
- Added a total search sample budget cap to prevent the structure planner from running indefinitely on unusual world seeds.
- Structure queries from Capsule Corp map trades and CC Namekian NPC trades now guard against off-thread access, preventing potential server-thread crashes.
- The Dragon Ball radar now syncs to players when they change dimensions, ensuring the radar stays accurate after dimension travel.
- Pending Dragon Balls now generate as soon as a player is within 128 blocks and the target chunk is loaded, preventing balls from being invisible until a relog.
- Refactored `DMZStructureSets` registration to a shared `unique()` helper, reducing boilerplate and making it easier to add new structures.
- Fixed the foundation placement on newly added structures.

*(by @yuseix300, @Shokkoh, @Bruneitor123)*

### Ki Laser — Position & Rendering
- Fixed the position of ki laser beams while firing: the beam origin now correctly projects from in front of the caster (using a per-render-type forward offset) rather than defaulting to the entity's center.
- Fixed render type routing: Makkankosanpo (type 1) and the new generic beam style (type 2) now map to the same renderer correctly after a routing mistake caused type 2 to fall through to the default laser.
- Added a generic `setupKiBeamPlayer()` setup method so BEAM-type techniques defined via config use their own configured colors and offset, independent of Makkankosanpo-specific logic. Makkankosanpo's color has been adjusted to a slightly darker purple (`0x8B17CF`).

*(by @yuseix300)*

### Ki Wave — Hitbox
- Enlarged the Ki Wave beam's collision cylinder radius from 1× to 1.5× the wave's size, and recalibrated the per-target hit precision to match. Ki waves now connect more reliably against targets within the visible beam area.

*(by @yuseix300)*

### CC Namekian Entity — Model Pivot
- Fixed an incorrect waist bone pivot on the Capsule Corp Namekian NPC model (was `[0, 0, 0]`, corrected to `[0, 12, 0]`), resolving a visual misalignment on the entity.

*(by @yuseix300)*

### Zamasu Armor — Textures
- Fixed incorrect textures on the Zamasu GI armor set (both worn layers 1 and 2).

*(by @yuseix300)*

### Command Autocomplete — Quest and Saga IDs
- Fixed tab-complete suggestions for `/dmzstory` quest and saga ID arguments. Suggestions now display IDs with `.` as a separator (e.g. `saiyan_saga.1`) instead of `:`, matching the normalization already applied when the argument is parsed — so tab-completed suggestions are valid without manual quoting in the chat bar.

*(by @Bruneitor123)*

### Weapon Registry — Invalid Entry Handling
- The weapon registry now handles missing or invalid weapon registrations gracefully with fallbacks, preventing potential crashes from unregistered or malformed weapon entries.

*(by @Shokkoh)*

### Majin — Alignment Lock
- Fixed a bug where Majin characters could drift away from 0 (neutral) alignment. Majins are now correctly locked at 0 alignment as intended.

*(by @Shokkoh)*

### Server Config Sync — Fallback Behavior
- Fixed config manager methods (`getServerConfig`, `getCombatConfig`, `getTrainingConfig`, `getSkillsConfig`, `getTechniqueConfig`) incorrectly falling back to fresh default configs when server sync was active but the synced config had not yet arrived. They now correctly fall back to the locally-loaded config in that window, preventing brief miscalculations of stats, combat values, or technique parameters immediately after joining a server.

*(by @Shokkoh)*

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
