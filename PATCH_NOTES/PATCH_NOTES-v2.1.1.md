# Patch Notes — v2.1.1

---

## New Content

### World Generation

- **Capsule Corp Villager Structure:** A Capsule Corp-themed structure now spawns inside vanilla Minecraft villages, blending the Dragon Ball world into regular Minecraft exploration.
- **Gamerule — `allowKiGriefingMasterStructures`:** Added a new gamerule that controls whether ki attacks can destroy or damage master training structures. Servers can now protect these key structures from ki attack collateral damage.
- **Structure Spacing Server Config:** Two new server config fields let operators tune how far apart repeating structure copies spawn: `structureSpacing` (default 6000 blocks; region size — lower means you encounter structures sooner while exploring) and `structureSeparation` (default 2000 blocks; minimum buffer between placements). Both are configurable at runtime without a datapack override.
- **Namek Caves — GETE Ore Debris:** Gete ore now spawns as scattered debris clusters deep in Namek caves (Y −64 to −32). Two cluster sizes — large (up to 3 blocks) and small (up to 2 blocks) — generate per chunk using scattered placement (100% air-exposed), adding a new source of the material and a reason to mine the Namek underground.

*(by @yuseix300, @Shokkoh)*

### Character Customization

- **Namekian — New Body Types:** Added two new body types for the Namekian race, each with 4 layered textures, expanding character creation options.
- **Namekian — New Eye Styles:** Added 2 new eye styles (eyes 3 & 4) with 4 color variants each for Namekian characters.
- **Majin Antenna & Frost Demon Horns:** Two new race part accessories are now available for character customization.
- **Race Parts Textures:** Updated and expanded the race parts texture system.
- **BioAndroid — New Body Types:** Added 2 base body types, 2 Semi-Perfect body types, and 2 Perfect body types for the BioAndroid race, each with 5 layered textures, expanding character creation options for BioAndroid players.
- **Broly — Texture Variants:** Broly NPC now has multiple texture variants for both his Super Saiyan and Legendary Super Saiyan forms.
- **Form Skin Tint — `formTint` Config Field** *(Addon/Developer API)*: Forms can now specify a `tintColor` (hex) and `tintIntensity` (0.0–1.0) in their form JSON config to tint the player's skin, hair, and race parts while in that form. Replaces the old hardcoded Kaioken-only red tint. Kaioken (x2–x100), Shiyoken, Shin Shiyoken, and Chou Shiyoken all use this new system. User-defined form files are automatically version-migrated on load.
- **Tail Color Transition:** The Saiyan tail now displays a smooth color gradient transition, adding visual polish to tail rendering during form changes and race part previews.

*(by @yuseix300, @Shokkoh)*

### Music Discs

- **Music Discs in World Loot:** DragonMineZ music discs can now be found in the world via the global loot modifier system — they're discoverable in regular Minecraft loot sources.

*(by @Shokkoh)*

### Armor

- Added item inventory textures for the following armor sets: **Capsule Corp**, **Cooler Soldier**, **Gero**, **Gilgamesh** (including worn armor layers), **Great Saiyaman 2**, **Raditz**, **Subaru Natsuki** (base and Arc 6 variant).

*(by @yuseix300)*

### Weapons

- **Dimensional Sword:** A new katana-class weapon is now craftable. The Dimensional Sword features a fully custom GeckoLib 3D model and 2D inventory icon, 75 base damage, and inherits katana weapon attributes with a 25% crit chance and +10% crit damage bonus. Tooltip: *"A wicked blade capable of slicing through the very fabric of space."*
- **Weapon Texture Updates:** Refreshed inventory textures for the **Brave Sword**, **Yajirobe Katana**, and **Z-Sword** items.

*(by @yuseix300)*

### Masters

- **Form Skills for Masters:** Master NPCs can now teach skills linked to specific transformation forms. Their skill list in both the Master Skills screen and the character Skills Menu now includes form-specific skill entries, letting players unlock and train form abilities directly through a master's teaching interface. Race character configs now support a dedicated form-skill section, and the server correctly handles the level 0→1 unlock transition for these form-linked skills.

*(by @Shokkoh)*

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

### Buu Saga — Quest NPC Correction
- The Buu Saga step 4 quest ("Enter the World Tournament") now correctly directs players to speak with **Piccolo** instead of Shin. The quest description has been updated to match.

*(by @Shokkoh)*

### Future Gohan — Super Saiyan NPC Model
- Future Gohan now has a dedicated custom 3D model when appearing in his Super Saiyan form during story sequences, replacing the previous shared model.

*(by @yuseix300)*

### Quest-Tunable Transformation Stats *(Addon/Developer API)*
- Kill objectives in quest JSON now support seven new optional fields for fine-tuning how a saga enemy's stats change when it transforms mid-fight:
  - **Absolute overrides** (party-scaled at spawn, difficulty-scaled at transform time): `TransformHealth`, `TransformMeleeDamage`, `TransformKiDamage`
  - **Multiplier overrides** (relative to the pre-transform form's values): `TransformHealthMultiplier`, `TransformMeleeMultiplier`, `TransformKiMultiplier`
  - **Trigger threshold:** `TransformTriggerPercent` — fraction (0.0–1.0) of max HP at which the enemy triggers its transformation (default: 0.5)
- Absolute values take precedence over multipliers when both are set. All overrides carry forward across multi-stage transformations.
- A new `transformDefaults` block in `entities.json` sets global defaults for all transforming saga enemies; per-quest overrides always win over these global values.

*(by @Bruneitor123)*

### Quest Rewards Per Difficulty *(Addon/Developer API)*
- Quest rewards in JSON now support a `difficulty` field (also accepted as `difficultyType` or `minDifficulty`) with values `ALL`, `NORMAL`, or `HARD`. Rewards gated behind a difficulty threshold are shown but grayed out in both the Quest Tree and NPC dialogue screens when the player's difficulty is below the requirement.
- **Now fully implemented across all reward types** (`ItemReward`, `TPSReward`, `AlignmentReward`): difficulty gating is consistently applied and synced to clients via the quest registry packet. Quest reward text rendering has been unified into a new `QuestTextFormatter` class, ensuring consistent formatting across the Quest Tree and NPC Dialogue screens.

*(by @Bruneitor123)*

### Absorption Counts Toward Kill Objectives
- Absorbing an enemy (e.g., as Cell or Buu) now correctly registers as a kill for quest kill-tracking objectives.

*(by @Shokkoh)*

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

### TP Multiplier — Calculation Fix
- Fixed the TP multiplier formula. All bonuses (class, racial skill, HTC, gravity, weight bells, global, potions, mutant, difficulty, progression) are now combined **additively**: `total = 1 + Σ(bonus − 1)`. Previously, several sources (weight bells, global, potions, mutant, difficulty) were applied multiplicatively on top of the additive group, causing compounding overstatements of TP gains — especially in scenarios where multiple bonuses were active at once.
- As part of this rebalance, the **Hyperbolic Time Chamber** default TP multiplier has been reduced from **2.5× to 1.75×**.

*(by @Shokkoh)*

### Party Mob Scaling Rework
- Enemy scaling in party play has been changed from an exponential formula to a **linear per-player model**. Each additional party member now adds a configurable percentage to enemy HP and damage (defaults: +25% HP, +10% damage per member). New server config fields: `enemyHealthPerPartyPlayer` and `enemyDamagePerPartyPlayer`.

*(by @Shokkoh)*

### Defense Formula Update
- Adjusted the defense formula to perform more consistently across damage types — particularly against **strike attacks**, in **PvP**, and against **story bosses**. The previous formula undervalued defense against these scenarios.

*(by @Shokkoh)*

### Defense Reduction — Further Tuning
- Refined two key defense parameters for a more accurate combat feel: `flatMitigationMaxAbsorbFraction` raised from **0.5 → 0.65** (high-defense characters absorb a larger fraction of incoming damage) and `defenseReductionScale` lowered from **0.25 → 0.15** (defense penetration is slightly less punishing). Both values remain configurable in the combat config.

*(by @Shokkoh)*

### Android — Lock-On Range Improved
- The lock-on range bonus for Android Upgraded players has been increased from **+10 → +25** blocks over the base lock-on range.
- Players in an active god form can no longer be targeted by lock-on from players who do not have the godforms skill.

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

### Ozaru Fist & Dragon Fist — Rework & Fixes
- Corrected initial damage values for Ozaru Fist and Dragon Fist attacks.
- **OozaruFist** received a complete rework — the `StrikeAttackHandler` logic for the attack was rebuilt from scratch with substantially revised timing, hitbox handling, and attack application flow.
- **DragonFist** received additional fixes to its entity behavior (`SPDragonFistEntity`) and the strike execution pipeline, improving consistency and damage application.

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

### Drain Scaling — Bonus Stats Excluded
- Ki and energy drain scaling now correctly ignores bonus stat values when calculating drain rates. Previously, bonus stats (from gear, effects, or passive abilities) were factored into drain, causing inflated consumption.

*(by @Shokkoh)*

### Battle Power Formula — VIT & ENE Now Included

- VIT (Vitality) and ENE (Energy) stats now contribute to Battle Power at **50% weight** alongside the existing STR, SKP, DEF, and PWR contributions. Support stats factor in proportionally without overshadowing offensive contributors, making high-VIT and high-ENE builds reflect more accurately in BP display and combat scaling.

*(by @Shokkoh)*

### Battle Power — Accurate Mob Formula

- Non-DMZ mobs (vanilla Minecraft entities, animals, and mobs from other mods) now have their Battle Power calculated using an accurate formula based on their actual entity attributes: **max health, attack damage, armor, armor toughness, ki/ranged damage, and movement speed**. Previously all non-player entities used the naive `health + attack × 5` estimate, which grossly underestimated many modded or high-stat enemies.
- The new `MobBattlePowerHelper` class handles this calculation and is automatically skipped for DMZ-managed entities, which continue to use the native DMZ BP system.
- The `accurateMobBattlePower` combat config option (default `true`) can disable the accurate formula if needed.
- Config version updated to **2.1.2** to reflect these and the defense system additions.

*(by @Shokkoh)*

### Adaptive Defense Mitigation

- A new final reduction layer — **Adaptive Defense Mitigation** — is applied after all flat, power-divider, and enchantment reductions. When incoming damage is at or below your effective defense level, an additional proportional reduction is applied. The closer the damage-to-defense ratio is to 1:1 (parity), the stronger this layer; it decays to zero as incoming damage significantly exceeds defense. Default cap: **70%**.
- When raw flat mitigation equals or exceeds **3× the incoming damage** (configurable via `cancelDamageMitigationThreshold`, default 3.0), the hit is **fully negated** — the damage event is cancelled and a brief block-impact sound plays.
- `flatMitigationMaxAbsorbFraction` further raised from **0.65 → 0.70**.
- New combat config fields: `enableAdaptativeDefenseMitigation`, `adaptativeMitigationParityRatio`, `adaptativeMitigationParityValue`, `adaptativeMitigationZeroRatio`, `adaptativeDefenseMitigationCap`, `cancelDamageEventIfMitigationTooHigh`, `cancelDamageMitigationThreshold`.

*(by @Shokkoh)*

### Zenkai & Assimilation Boosts Nerfed

- The default bonus multipliers for **Saiyan Zenkai** (the HP-scaling power boost earned after recovering from near-death) and **BioAndroid Assimilation** (the damage boost from absorbing enemies) have been reduced in the server config defaults. Existing configs that have already been generated will retain the old values until manually updated or reset via `/dmzrestoreupdate`.

*(by @Shokkoh)*

### Story Boss & Enemy AI — Skills Rebalanced

- Saga enemy AI for skill usage and combo execution has been substantially overhauled. `ComboManager` and `SkillManager` now apply improved timing windows and selection criteria, preventing bosses from spamming certain abilities while ignoring others.
- Special ki attack entities used by story bosses have been tuned: **Blue Hurricane, Dragon Fist, OozaruFist, and Majin Candy** projectile and strike behaviors adjusted for fairer, more consistent encounters.
- `DBSagasEntity` skill dispatch logic received a major overhaul — AI decisions are now better gated by cooldowns and health thresholds.

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

### Fly Burst — Removed
- The double-tap fly key "burst launch" has been removed. Double-tapping the fly key while on the ground no longer fires the player forward at high speed — the key now initiates standard flight take-off in all cases. The burst deceleration immunity phase and its associated `isBurst` network packet field have also been removed.

*(by @Shokkoh)*

---

## Skills & Techniques

### Instant Transmission
- Reworked Ki cost handling for Instant Transmission to be accurate and consistent.
- Added a fallback feedback message displayed to the player when no valid teleport destination can be found.
- When interacting with a Master NPC for the first time, players with the Instant Transmission skill now receive a chat message confirming the Ki signature has been memorized: *"You now know the Ki of [Master Name]."* Players without the skill no longer register masters' signatures on interaction.

*(by @Shokkoh, @yuseix300)*

### Instant Transform — Stack Form Support
- Fixed Instant Transform not routing correctly when the player's selected action mode is **Stack**. The action now properly applies the next available stack form with all compatibility, mastery, and ki-cost checks — rather than always targeting the base form group. If switching the base form to one that is incompatible with the currently active stack form, the stack form is automatically cleared and the player is notified.

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

### X Menu — Forms Grouped by Type
- Transformation forms in the radial X menu are now **grouped by form group**: the first form in each group is the head node, and additional forms within the group are accessible by expanding it. This reduces clutter for races with large transformation pools and makes navigating to a specific form more intuitive.

*(by @Shokkoh)*

### X Menu — Fusion Action Node
- A dedicated **Fusion** action button has been added to the radial X menu. It toggles the Fusion action mode directly from the wheel and is only visible when the player has the fusion skill unlocked.

*(by @Shokkoh)*

### Skills Menu — Per-Race Filtering
- Skills, form skills, stack forms, and ki/strike techniques that are not allowed for the player's race are now correctly hidden across all tabs of the Skills Menu (SKILLS, FORMS, KI, STRIKE). The radial X menu also respects race filtering for both standard and stack forms, preventing forms from other races from cluttering the wheel.
- Server-side validation for form skill purchases was also improved: the buy button is correctly gated by race permission checks, and the `level 0 → 1` transition for form skills is handled properly.

*(by @Shokkoh)*

### Quest Tree — Enemy Preview Accurate Stats
- The enemy preview card in the Quest Tree screen now displays the enemy's **actual spawn stats**: base objective values scaled by the player's current difficulty multiplier and party size. Previously the card always showed raw base values regardless of these modifiers.

*(by @Shokkoh)*

### Over Shoulder Camera
- A fully configurable **over-shoulder camera** is now available. Open it from the Config screen → "Over Shoulder Camera" → **Open »**.
- **Three modes:** Disabled, Lock-On Only (activates only while locked onto a target), or Always.
- **Adjustable settings:** shoulder side (Left / Right), back distance (1–6), height (−1 to 2), side offset (0–3), and smoothing speed (0.05–0.6).
- The camera clips intelligently against walls using 8 corner rays to prevent geometry phasing.
- Default: Always mode, right shoulder, distance 3.

*(by @Shokkoh)*

### HUD Bars — Damage & Heal Chip Animations
- Both HUD styles (Xenoverse and Alternative) now display **animated damage and heal chips** on the HP bar:
  - Taking damage leaves a brief **red ghost bar** that lingers for ~1.5 seconds before catching up to the current HP value.
  - Large burst heals show a brief **green chip** that fills ahead of the bar before snapping into place.
- All three bars (HP, Ki, Stamina) now use a time-based eased animator for smoother, frame-rate-independent transitions.
- The damage ghost system has been further refined: the animator now tracks **multiple simultaneous damage segments** in a segment list rather than a single shared ghost bar. Rapid consecutive hits each create their own ghost segment that fades independently, giving a more accurate visual readout of multi-hit burst damage.

*(by @Shokkoh)*

### `/dmzmastery all` Command
- The `/dmzmastery` admin command now accepts `all` as a target, instantly mastering every technique at once instead of requiring individual entries.

*(by @Shokkoh)*

### `/dmzrestoreupdate` Command
- Added the `/dmzrestoreupdate` admin command. Running it displays a **clickable confirmation prompt** in chat (expires after 15 seconds). On confirmation, it deletes all world-save files inside the `wishes/`, `sagas/`, and `quests/` directories and immediately triggers a full `/dmzreload all` — useful for resetting server-customized quest and wish data back to the mod's current defaults after an update.

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

### Quest Reward Display — Commands, Transformations & Ki Techniques
- Rewards granted via commands (e.g., `/dmzstory`), transformation unlocks, and ki technique grants now display a visual reward notification, making it immediately clear what the player received.

*(by @Shokkoh)*

### Client Config — Hide Outlines
- A new client-side configuration option allows players to hide entity and player outlines globally, for those who prefer a cleaner visual presentation.

*(by @Shokkoh)*

### Utility Menu — Manual Scaling
- The X radial utility menu now supports a manual scale override in the client config, letting players set a custom size multiplier independent of the current GUI scale setting.

*(by @Shokkoh)*

---

## Bug Fixes

### Aura Sound — Permanent Aura State
- Fixed aura loop sounds not playing or stopping incorrectly during **permanent aura** states. Both the aura sound handler and the aura loop sound tick now check for permanent aura in addition to the regular active aura flag. Several sound files were also updated.

*(by @Shokkoh)*

### Aura Layer Renderer
- Reworked the aura layer rendering system to support per-layer alpha. Each `AuraLayer` now carries an `alpha` value, enabling smooth fade-in transitions when charging toward a form whose aura uses a different layer slot than the current one. Previously, cross-layer aura transitions could cause an abrupt swap; the incoming layer now fades in proportionally to charge progress while the current layer remains visible.
- Fixed the charge-state logic for stack forms: when the target form's stack layer matches the base layer, color is interpolated in-place; when it differs, the new layer blends in by alpha. Both the GUI aura and the in-world aura pulse draws now respect per-layer alpha.

*(by @Shokkoh)*

### Aura Rotation — Reduced
- Reduced excessive aura rotation speed for a cleaner, less distracting visual.

*(by @Shokkoh)*

### Aura on Over-Shoulder Camera
- Fixed the player aura rendering incorrectly or clipping when the over-shoulder camera mode is active.

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

### Entity Stun
- Fixed a general issue where entities were not correctly applying or respecting the Stun status effect under certain combat scenarios outside of story boss fights.

*(by @Shokkoh)*

### Quest Reward — Transformation Unlock
- Fixed `TransformationReward` quest rewards not granting the required skill level for the rewarded form. Completing a quest that awards a transformation now also ensures the player's relevant transformation skill (e.g. `superforms`, `legendaryforms`) is set to at least the level required to access that form — preventing forms from being awarded but remaining locked.

*(by @Bruneitor123)*

### Quest Entity Spawning
- Fixed quest-related entities (NPCs and enemies tied to story objectives) failing to spawn correctly under certain conditions.

*(by @Shokkoh)*

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
- Further structure generation and placement corrections (v3 iteration), addressing additional edge cases in structure spawning logic.

*(by @yuseix300, @Shokkoh, @Bruneitor123)*

### Duplicated Dragon Ball Generation
- Fixed an issue where Dragon Balls were generating multiple times in the same session, resulting in duplicate balls appearing in the world.

*(by @Shokkoh)*

### GETE Ore Debris Generation
- Fixed GETE ore debris not generating correctly in Namek cave chunks after the feature was introduced.

*(by @Shokkoh)*

### Hyperbolic Time Chamber — Exit Portal Fix
- Fixed players being teleported to world spawn when exiting the Hyperbolic Time Chamber if the exit portal could not be located. Exit now correctly resolves the Kami's Lookout structure position and teleports the player there, only falling back to world spawn if the structure genuinely cannot be found.
- Fixed a structural issue where the portal block search failed due to Y-axis bounding box mismatch in the structure start lookup. Added a chunk-level fallback and force-loads all chunks within the structure's bounds so the portal block is placed before the scan runs.

*(by @yuseix300)*

### Oozaru — Idle Animation Stall Fix
- Fixed the Oozaru idle animation getting stuck or not updating correctly after entering or leaving a stack form. The Oozaru model cache is now refreshed whenever a stack form is set, cleared, or copied (e.g. on respawn or dimension change).

*(by @yuseix300)*

### Ki Laser — Position & Rendering
- Fixed the position of ki laser beams while firing: the beam origin now correctly projects from in front of the caster (using a per-render-type forward offset) rather than defaulting to the entity's center.
- Fixed render type routing: Makkankosanpo (type 1) and the new generic beam style (type 2) now map to the same renderer correctly after a routing mistake caused type 2 to fall through to the default laser.
- Added a generic `setupKiBeamPlayer()` setup method so BEAM-type techniques defined via config use their own configured colors and offset, independent of Makkankosanpo-specific logic. Makkankosanpo's color has been adjusted to a slightly darker purple (`0x8B17CF`).

*(by @yuseix300)*

### Ki Wave — Hitbox
- Enlarged the Ki Wave beam's collision cylinder radius from 1× to 1.5× the wave's size, and recalibrated the per-target hit precision to match. Ki waves now connect more reliably against targets within the visible beam area.

*(by @yuseix300)*

### Ki Weapon & Movement
- Fixed multiple issues related to Ki weapon behavior and player movement interactions, improving reliability and feel.

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

### Config Restoring
- Fixed an issue with config file restoration behavior under certain load and backup conditions.

*(by @Shokkoh)*

### Missing Network Packets
- Fixed missing network packets that caused certain client-server actions to silently fail.

*(by @Shokkoh)*

### Custom Skills Reset on `/dmzreload`
- Fixed custom-defined skills (from config or addons) being reset back to defaults when `/dmzreload` was executed on a running server.

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

### Black Tail
- Fixed a visual bug where the Saiyan tail was rendering solid black instead of the correct skin texture.

*(by @Shokkoh)*

### Custom Human Models
- Fixed the renderer rejecting custom human model definitions supplied by resource packs or addons.

*(by @Shokkoh)*

### Infinite Stat Growth
- Fixed a bug in the Dynamic Growth system where player stats could grow infinitely under certain conditions due to missing or incorrect growth cap validation.

*(by @Shokkoh)*

### Smart Config Regeneration
- The mod now ships baseline config snapshots (in `previousConfigs/`) inside the jar for all major config types: combat, entities, general-server, general-user, and all race configs (character, forms, stats). When a user's config is found to be outdated, migration now diffs against these bundled snapshots to apply only what changed — preserving server/user customizations while cleanly introducing new fields and corrections. Previously, migration could discard non-default customizations in edge cases.

*(by @Shokkoh)*

### Curios API — Essential Mod Crash
- Refactored Curios API usage into a centralized utility class (`CuriosUtil`), resolving crashes that occurred when the **Essential** mod was installed alongside DragonMineZ.

*(by @Shokkoh)*

### Arclight — LivingEntity Crash
- Fixed a server crash on **Arclight** software caused by incompatible `LivingEntity` handling in combat event processing.

*(by @Shokkoh)*

### X Menu — Radial Scaling
- Fixed the radial X menu rendering too large on high-resolution or high-GUI-scale displays. The menu now applies a 2/3 scale factor relative to the available UI scale, matching the intended on-screen proportions.

*(by @Shokkoh)*

### Dende NPC — Reset Stats Confirmation
- Clicking "Reset Stats" when speaking with Dende now shows a **warning message** and requires clicking "Reset everything" to confirm before the full character reset is performed. Selecting "Cancel" returns to the normal dialogue without any action. Previously the reset would execute immediately with no warning or confirmation step.

*(by @Shokkoh)*

### Instant Transform — Non-Stackable Forms
- Fixed non-stackable stack-type forms incorrectly appearing as purchasable in the transformation skill tree. The buy button is now hidden for these forms' base slot, and the server rejects any attempt to purchase or upgrade a stack skill from level 0 via the normal TP path (they must be granted through quest rewards or other means).
- When using Instant Transform to switch to a new base form, any currently active stack form that is incompatible with the new base form (due to stackability, mastery, or compatibility flags) is now automatically cleared and the player is notified.

*(by @Shokkoh)*

### Stack Forms Buyable Again
- Fixed a regression that prevented stack transformation forms from being purchased in the skill tree.

*(by @Shokkoh)*

### Fusion — Teleport Issues
- Fixed multiple issues with player teleportation occurring incorrectly during or after the fusion sequence.

*(by @Shokkoh)*

### Flying Animations on Multiplayer
- Fixed flying animations not playing or syncing correctly for other players in multiplayer sessions.

*(by @Shokkoh)*

### Camera Roll During Search Fly — Reduced
- Slightly reduced the camera banking/roll effect during "search fly" mode for a more comfortable flying camera feel.

*(by @Shokkoh)*

### Forms Order on X Menu
- Fixed the ordering of transformation forms in the radial X utility menu, ensuring forms appear in the correct intended order.

*(by @Shokkoh)*

### Hair Physics
- Fixed hair physics simulation not behaving correctly in certain cases.

*(by @Shokkoh)*

### TP Multiplier Tooltip
- Fixed the TP multiplier tooltip displaying incorrect or malformed values.

*(by @Shokkoh)*

### Quest Conditions — Race & Class Sync
- Fixed RACE and CLASS prerequisite conditions in quest definitions not being serialized into the quest registry sync packet sent to clients. On multiplayer servers, quests gated by a player's race or class will now correctly transmit those requirements, preventing them from being evaluated as always-met on the client side.

*(by @Bruneitor123)*

### Form Free-Transform Config — API Change *(Addon/Developer API)*
- The `canAlwaysTransform` and `directTransformationIfUsed` / `directTransformIfUsedOnMastery` form JSON config fields have been **removed** and replaced by the single `allowFreeTransformOnMastery` field. Set to `0.0` (or omit) for forms that are always freely accessible from the radial menu; set to a mastery percentage value for forms that require reaching that threshold first. Addons using the old fields must migrate to `allowFreeTransformOnMastery` — the old fields are no longer read.

*(by @Shokkoh)*

### Hair Editor — Mirror & Sliders
- Fixed the mirror functionality and axis sliders in the Hair Editor screen not working correctly. Slider interactions are now properly registered and mirror mode correctly propagates symmetrical changes to the opposite side.

*(by @Shokkoh)*

### Ki Attack Position — Additional Fixes
- Fixed the origin position of ki attacks during certain gameplay states. `TechniqueDispatcher` now correctly resolves attack origins when firing or dispatching techniques from flight or transitional movement states. Ki Laser entity behavior was also corrected to track caster movement more accurately.

*(by @yuseix300)*

### Sparks Sound Effects
- Fixed clash spark sound effects not playing correctly under certain combat conditions.

*(by @yuseix300)*

### Explosion Density
- Fixed particle density on Ki Blast and Ki Wave explosions appearing too sparse or too dense under certain conditions.

*(by @yuseix300)*

### Capsule Corp Villager — Zombie Texture
- Added the missing zombie villager texture for the Capsule Corp Assistant profession. CC Villagers that are turned into zombies now display the correct zombified texture.

*(by @yuseix300)*

### Entity Spawning & Namek Warrior Crash
- Fixed biome modifier data for several entity groups — dinosaurs, robots, sabertooth tigers, and saibamans — that were not spawning in their intended biomes due to incorrect modifier configuration.
- Fixed a server crash triggered by the Namek Warrior entity under certain initialization conditions.
- Sacred Kai dimension monster data was also updated.

*(by @Shokkoh)*

### Keybind Handling — Additional Fixes
- Fixed several keybind detection and handling issues in `ClientStatsEvents` and `KeyBinds`. Technique hotbar keybind registration was corrected to properly detect and respond to configured key combinations.

*(by @Shokkoh)*

### Flight + Attack Movement Lock
- Fixed players being able to freely move while charging or firing ki attacks during flight. Player movement is now correctly locked when using attacks mid-fly, preventing unintended repositioning during attack animations and charge phases.

*(by @Shokkoh)*

### Dragon Ball Radar — Sync Fixes
- Fixed the Dragon Ball radar failing to update reliably after dimension changes or certain server-side events. Added a delayed sync mechanism and additional sync triggers in the server-side `DragonBallsHandler` so the radar display stays accurate when ball positions change.

*(by @Shokkoh)*

### Racial Skill Display on X Menu
- Fixed racial skill action nodes not displaying correctly in the radial X utility menu.

*(by @Shokkoh)*

### Saga Enemy Party Scaling
- Fixed party player count scaling not correctly applying to saga boss encounter stats under certain conditions.

*(by @Shokkoh)*

---

## Contributors

| Contributor | Area |
|---|---|
| **@Shokkoh** | Combat, balance, boss AI, physics, story systems, UI, networking, bug fixes |
| **@yuseix300** (Yuse) | Art, models, armor, race customization, world generation, story NPCs |
| **@Bruneitor123** (Bruno V.) | Quest systems, developer API, structures, client bug fixes |

---

*Patch notes compiled by Claude — [DragonMineZ/dragonminez](https://github.com/DragonMineZ/dragonminez/tree/v2.1.x)*
