# Getting Started (Addon Development)

This is the prerequisite page before [[Creating a DMZ Addon|Creating-a-DMZ-Addon]]. It covers what you need in place *before* wiring up your own Forge mod against DMZ.

## Who this is for

Forge addon developers who want their own mod jar to react to DMZ (grant bonus TP, listen for form changes, add new quest objective/reward types, add buttons to the utility menu, etc). If you just want to tune numbers or add races/forms/quests/wishes, you don't need any of this — see [[Developer Scope and Prerequisites|Developer-Scope-and-Prerequisites]] instead. DMZ has no Java API to register new races, forms, skills or techniques: new races and forms are added through JSON configs (see [[Custom Races|Custom-Races]] and [[Custom Forms|Custom-Forms]]).

## Required toolchain

| Requirement | Version | Where it's declared |
| :-- | :-- | :-- |
| Minecraft | `1.20.1` | `gradle.properties` |
| Forge | `47.4.10` (accepts `[47,)`) | `gradle.properties`, addon `mods.toml` |
| ForgeGradle | `6.0.30` | `build.gradle.kts` plugins |
| Java | `17` | `build.gradle.kts` toolchain |
| Mappings | Parchment `2023.09.03-1.20.1` | `gradle.properties` |
| GeckoLib | `4.8.3+` | `gradle.properties`, addon `mods.toml` |
| TerraBlender | `3.0.1.10+` | `gradle.properties`, addon `mods.toml` |
| Curios API | `5.14.1+1.20.1` or newer | `gradle.properties`, addon `mods.toml` |

Match these exactly (or with compatible version ranges) in your own addon's `build.gradle(.kts)`/`gradle.properties`/`mods.toml`, or you'll hit classloading/mixin mismatches at runtime. DMZ itself is currently on mod version `2.2-alpha` (`gradle.properties`).

GeckoLib, TerraBlender and Curios are **mandatory** dependencies of DMZ, so they must be on your dev runtime classpath too.

## Incompatible mods

DMZ refuses to start (it throws an `IllegalStateException` while loading) when any of these is installed, and declares them `incompatible` in its `mods.toml`:

- Legendary Tooltips (`legendarytooltips`)
- Epic Fight (`epicfight`)
- Better Combat (`bettercombat`)

Don't depend on them, bundle them, or add them to your dev run configs.

## Pre-release builds

`2.2-alpha` is a pre-release build. Alpha/beta builds of DMZ only let whitelisted usernames join a world or server. The default ForgeGradle dev username `Dev` is on the whitelist, so `runClient` works out of the box — keep that username for local testing.

## Understand the package layout first

Before writing any hook code, skim the source layout (`AI/Context.md` in the DMZ repository documents it in detail):

- `com.dragonminez.client` — rendering/GUI/animation, client-only.
- `com.dragonminez.common` — config, stats, networking, quests, wishes; the surface most addons touch.
- `com.dragonminez.server` — commands, storage, worldgen, server events.
- `com.dragonminez.mixin` — mixins; avoid depending on these from an addon.

Addons should hook `com.dragonminez.common` almost exclusively — it's the shared, most-stable layer. The only client-side hooks meant for addons are `DMZClientEvent` and the utility menu slots (see [[Implementation & Events|Implementation-&-Events]]).

## Two integration styles

1. **Hard dependency** — your addon requires DMZ to be present (`mandatory=true` in `mods.toml`). Simplest, but your addon won't load without DMZ.
2. **Soft/optional dependency** — your addon works standalone and only activates DMZ-specific behavior if DMZ is detected. DMZ uses this pattern itself for its optional integrations (`common/compat/WeaponLevelingCompat.java`, `common/compat/ApotheosisCompat.java`): check `ModList.get().isLoaded("dragonminez")` once, and keep every class that imports DMZ code behind that check. Recommended if your addon has value without DMZ installed. See [[Implementation & Events|Implementation-&-Events#optional-dependency-pattern]].

## What you can safely rely on

- **Events** — the nested classes of `com.dragonminez.common.events.DMZEvent` (server/common) and `com.dragonminez.client.events.DMZClientEvent` (client) are the intended public hook surface. See [[Implementation & Events|Implementation-&-Events]] for the full list.
- **Read-only player state** — `StatsCapability`/`StatsProvider`/`StatsData` for reading race, form, stats, and resources.
- **Extension registries** — `QuestObjectiveRegistry`/`QuestRewardRegistry` (custom quest content types), `DMZTextPlaceholders` (custom `%placeholders%` in quest/dialogue text) and `UtilityMenuScreen.addMenuSlot` (client utility menu buttons).
- **Registries** — `MainItems`, `MainBlocks`, `MainEntities`, `MainAttributes`, `MainEffects`, `MainEnchants` (and the other `Main*` classes) under `common/init/` expose `RegistryObject`/`DeferredRegister` references you can import directly.

## What to avoid relying on

- Client-reported values for anything progression-related — DMZ never trusts them either; read from the server-side capability instead.
- Directly mutating `StatsData` fields outside of events/commands. Prefer reacting to/cancelling events over poking values, and prefer DMZ's existing commands when you need to change something DMZ owns.
- Registering packets on DMZ's network channel — DMZ's packet ids depend on registration order. Use your own channel.
- Depending on `com.dragonminez.mixin` classes or DMZ internals not mentioned above — these can change without notice between versions.

## Next step

Once your dev environment builds against DMZ and you can see it detected at runtime, move on to [[Implementation & Events|Implementation-&-Events]] to wire up actual hooks, then [[Creating a DMZ Addon|Creating-a-DMZ-Addon]] for full Gradle/`mods.toml` setup.
