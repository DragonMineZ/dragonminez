# Patch Notes — v2.1.1

---

## New Content

### Character Customization

- **Namekian — New Body Types:** Added two new body types for the Namekian race, each with 4 layered textures, expanding character creation options.
- **Namekian — New Eye Styles:** Added 2 new eye styles (eyes 3 & 4) with 4 color variants each for Namekian characters.
- **Majin Antenna & Frost Demon Horns:** Two new race part accessories are now available for character customization.
- **Race Parts Textures:** Updated and expanded the race parts texture system.

*(by @yuseix300)*

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

### `canTransform` on Kill Objectives *(Addon/Developer API)*
- Kill objectives in quest JSON files now support an optional `canTransform` field. This allows addon and quest developers to require a player to be in a specific transformation state when the kill objective is completed.

*(by @Bruneitor123)*

### NPC & Mission Cleanup
- Removed references to unused NPC entries. Added missing translation keys for mission objective text.

*(by @Bruneitor123)*

### Z-Sword — Old Kai Reward
- Completing the **Ultimate Minigame** training with Old Kai now grants the **Z-Sword** directly to your inventory as a reward.

*(by @Shokkoh — [ddda01c](https://github.com/DragonMineZ/dragonminez/commit/ddda01c2a7f4fa8b16cb97e50d0f6dc2e805080b))*

### Saga Balance Pass
- Adjusted difficulty and reward parameters for the **Cell Saga**, **Buu Saga**, and **movie saga** battles for a better difficulty curve overall.

*(by @Shokkoh — [d956451](https://github.com/DragonMineZ/dragonminez/commit/d9564512acaeef3b9e88b0e28a457cf448a35f96))*

---

## Combat & Balance

### Defense Formula Update
- Adjusted the defense formula to perform more consistently across damage types — particularly against **strike attacks**, in **PvP**, and against **story bosses**. The previous formula undervalued defense against these scenarios.

*(by @Shokkoh)*

### Mastery Gain Scaling
- Technique mastery earned in combat now scales with the amount of damage dealt. Landing stronger hits rewards proportionally more mastery progress.

*(by @Shokkoh)*

### Mastery Blacklist
- Added a `masteryBlacklistEntities` config list. **Silverfish** and **training target dummies** are blacklisted by default — hitting them no longer awards technique mastery. The list is fully configurable in `combat.json`.

*(by @Shokkoh — [9013249](https://github.com/DragonMineZ/dragonminez/commit/9013249d1523b498e7021b064d184994d62f32c4))*

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

### Form Drain Reductions
- Reduced overall form energy, stamina, and health drain:
  - The **offensive damage contribution** to drain is now capped at `0.75 × form cost multiplier`, preventing exponentially scaling drain for high-stat players.
  - The **percentage-based energy drain** component is reduced by an additional 25%.
  - The **baseline form drain** config default is lowered from `100` to `80`.

*(by @Shokkoh — [d372ff4](https://github.com/DragonMineZ/dragonminez/commit/d372ff40f40043c028d0e209ecd82c51293afadb), [e4d0201](https://github.com/DragonMineZ/dragonminez/commit/e4d0201fbf9d5d29058f13d765be0971206fac45))*

### Saga NPC Combo Damage
- Story NPC combo attacks (basic, air, and charge combos) were using hardcoded flat damage values regardless of the NPC's actual power. All combo hits now scale off each NPC's **Attack Damage** attribute, so story boss difficulty responds correctly to config adjustments.

*(by @Bruneitor123 — [315a590](https://github.com/DragonMineZ/dragonminez/commit/315a590081e7bcd11e799e1ec7142868a7f3fe18))*

### Majin Absorption — Stats Copy Reduced
- The stat percentage copied when a Majin absorbs a target is reduced from **10%** to **4%** to address overtuned absorption growth.

*(by @Shokkoh — [463ec31](https://github.com/DragonMineZ/dragonminez/commit/463ec31656b1fea08cd721b059b42b49ce3a2777))*

---

## Physics & Movement

### Gravity & Weight System Rework
- Fully reworked the gravity calculation and weight handling logic. Introduced a dedicated `GravityStateSync` to properly synchronize gravity zone state from server to clients.
- Gravity device blocks now correctly notify players entering and exiting their zone.
- New configurable gravity parameters are available in the server config.
- Multiple edge-case bugs tied to the old gravity system have been resolved in this rework.

*(by @Shokkoh)*

### Combat Flight Speed Under Gravity
- Fixed combat flight speed not correctly accounting for the active gravity multiplier when inside a gravity zone.

*(by @Shokkoh)*

### Gravity Applies on Top of Weights
- Fixed a logic error where the HTC gravity multiplier only affected "training gravity" rather than the total active gravity. Equipment weight training now correctly scales with the full gravity zone multiplier. The **gravity sensitivity** config default is also raised from `0.5` to `1.0`, meaning gravity zones are fully effective for weight-based training out of the box.

*(by @Shokkoh — [f587d50](https://github.com/DragonMineZ/dragonminez/commit/f587d50eb8ccbeb0fa8affd3208583a210e9ef80))*

### Ideal Weight Tooltip
- The gravity info tooltip on the character stats screen now shows the **optimal weight range** (low–high) derived from config ratios, instead of only the single ideal weight value.

*(by @Shokkoh — [148d5df](https://github.com/DragonMineZ/dragonminez/commit/148d5df5f6d68c722dc42284ab23cf924407a9b1))*

---

## Skills & Techniques

### Instant Transmission
- Reworked Ki cost handling for Instant Transmission to be accurate and consistent.
- Added a fallback feedback message displayed to the player when no valid teleport destination can be found.

*(by @Shokkoh)*

---

## UI & Controls

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

### Utility Menu — Radial Redesign
- The Utility Menu has been **completely redesigned** as a circular radial wheel. Eight pie-slice sectors surround a live 3D preview of your current form at the center. Hovering a sector highlights it; clicking activates or drills into child sectors on an outer ring. The game world blurs behind the menu with a smooth scale open/close animation.
- Default sectors: Super Forms, More Forms, Movement/Flight, Descend, Actions/Fusion, Stack Skills/Kaioken, and two reserved slots for addon-registered buttons.
- When a category holds more items than fit in the ring, a **"More" sub-panel** lists the overflow options. Items in the sub-panel can be **drag-reordered**, and the custom order is saved per-player.
- Category/action mode changes (e.g. switching to Fusion mode) are now announced in the action bar.

*(by @Shokkoh — [3a087b7](https://github.com/DragonMineZ/dragonminez/commit/3a087b7fdf63b8750cf2d1eb1d688ca02d5a6c69))*

### Utility Menu — Sub-Panel Positioning Fix
- Fixed the "More" sub-panel being rendered centered on screen regardless of which sector opened it. The panel now appears adjacent to the node that triggered it, at the correct outward angle from the ring, and is clamped to stay within screen bounds. The hover highlight also now freezes on the active node while the panel is open.

*(by @Shokkoh — [d82b7ba](https://github.com/DragonMineZ/dragonminez/commit/d82b7ba0e2cbbbadea72164a91af96c3438aea49))*

---

## Addon / Developer API

### `extraAuraLayer` in Form Config
- Forms now support an optional **secondary aura layer**. Set `extraAuraLayer` (1–6), `extraAuraColor` (hex string), and `extraAuraType` in a form's JSON definition to render a second independent aura ring alongside the primary one. This works for both base forms and stack forms.

*(by @Shokkoh — [e4d0201](https://github.com/DragonMineZ/dragonminez/commit/e4d0201fbf9d5d29058f13d765be0971206fac45))*

### Utility Menu — `IUtilityMenuSlot` API
- Two reserved slots (positions 2 and 6) in the radial utility menu are available for addons. Register a custom button with `UtilityMenuScreen.addMenuSlot(IUtilityMenuSlot slot)`. Existing `IUtilityMenuSlot` implementations are automatically adapted to the new radial node system for backward compatibility.

*(by @Shokkoh — [3a087b7](https://github.com/DragonMineZ/dragonminez/commit/3a087b7fdf63b8750cf2d1eb1d688ca02d5a6c69))*

---

## Bug Fixes

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

### Render Buffer Not Reset
- Fixed a visual artifact where the render buffer was not properly flushed after the armor, skin, and third-party rendering layers, which could cause visual corruption in some rendering configurations.

*(by @Shokkoh — [395f2a9](https://github.com/DragonMineZ/dragonminez/commit/395f2a9cc6fecd98b9daa0064f057eb0c35e84f8))*

### Dragon Ball Spawning
- Dragon balls queued to scatter into chunks not yet loaded would silently fail to appear. A periodic rescan now runs every ~5 seconds to retry all pending spawns as world chunks load in.

*(by @Shokkoh — [c0d922d](https://github.com/DragonMineZ/dragonminez/commit/c0d922db3b30669f29d4ee9904baa60eae803ed4))*

### Outline Shader — Performance Overhaul
- Replaced the multi-pass outline shader pipeline (bloom, silhouette, Sobel edge-detect, outline color, unpack — 5 separate shader programs) with a streamlined composite pass. This significantly reduces GPU overhead when transformation outline effects are active, particularly with multiple transformed players on screen.

*(by @Shokkoh — [38daa03](https://github.com/DragonMineZ/dragonminez/commit/38daa03b6469b3f4a26f527891399c339081f6eb))*

### Server Config Sync — Enforcement & Reliability
- Introduced a `serverSyncActive` flag that strictly gates all config reads to server-synced copies while connected to a server, preventing local client configs from bleeding through.
- **Entities config** is now included in the server-to-client config sync.
- Config sync on player login now sends a **batch reset** signal with the first packet, so the client cleanly replaces stale synced data rather than merging into it.
- The **client-only config** (`general-user`) is excluded from the server sync broadcast.
- `StatsSyncC2S` now rejects packets referencing an unknown race name, logging a warning and resending authoritative data to the client.

*(by @Shokkoh — [b45c951](https://github.com/DragonMineZ/dragonminez/commit/b45c951361c22157941141ff8a67642da2d63318))*

### Missing Network Packets
- `SelectFormC2S` and `SelectKiWeaponC2S` were missing from the network packet registry, meaning form selection and Ki weapon toggling were not being validated server-side. Both packets are now registered with proper server-side checks (stun state, unlock eligibility, skill requirements).

*(by @Shokkoh — [9c8c978](https://github.com/DragonMineZ/dragonminez/commit/9c8c9782ac8ce64819d073f59ded6e49d2fc9842))*

---

## Contributors

| Contributor | Area |
|---|---|
| **@Shokkoh** | Combat, balance, physics, story systems, UI, networking, bug fixes |
| **@yuseix300** (Yuse) | Art, models, armor, race customization |
| **@Bruneitor123** (Bruno V.) | Quest systems, developer API, NPC combat, client bug fixes |

---

*Patch notes compiled by Claude — [DragonMineZ/dragonminez](https://github.com/DragonMineZ/dragonminez/tree/v2.1.x)*
