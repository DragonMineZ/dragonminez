# Getting Started (Addon Development)

This is the prerequisite page before [[Creating a DMZ Addon|Creating-a-DMZ-Addon]]. It covers what you need in place *before* wiring up your own Forge mod against DMZ.

## Who this is for

Forge addon developers who want their own mod jar to react to DMZ (grant bonus TP, listen for form changes, add new quest reward types via their own systems, etc). If you just want to tune numbers or add races/forms/quests, you don't need any of this — see [[Developer Scope and Prerequisites|Developer-Scope-and-Prerequisites]] instead.

## Required toolchain

| Requirement | Version | Where it's declared |
| :-- | :-- | :-- |
| Minecraft | `1.20.1` | `gradle.properties` |
| Forge | `47.4.10` (accepts `[47,)`) | `gradle.properties`, addon `mods.toml` |
| Java | `17` | `build.gradle.kts` toolchain |
| GeckoLib | `4.8.3+` | `gradle.properties`, addon `mods.toml` |
| TerraBlender | `3.0.1.10+` | `gradle.properties`, addon `mods.toml` |
| Curios API | `5.14.1+1.20.1` or newer | `gradle.properties`, addon `mods.toml` |

Match these exactly (or with compatible version ranges) in your own addon's `build.gradle.kts`/`gradle.properties`/`mods.toml`, or you'll hit classloading/mixin mismatches at runtime. DMZ itself is currently on mod version `2.1.1-beta` (`gradle.properties`).

## Understand the package layout first

Before writing any hook code, skim `AI/Context.md` in this repo (or the equivalent section of this wiki) for the source layout:

- `com.dragonminez.client` — rendering/GUI/animation, client-only.
- `com.dragonminez.common` — config, stats, networking, quests, wishes; the surface most addons touch.
- `com.dragonminez.server` — commands, storage, worldgen, server events.
- `com.dragonminez.mixin` — mixins; avoid depending on these from an addon.

Addons should hook `com.dragonminez.common` almost exclusively — it's the shared, most-stable layer.

## Two integration styles

1. **Hard dependency** — your addon requires DMZ to be present (`mandatory=true` in `mods.toml`). Simplest, but your addon won't load without DMZ.
2. **Soft/optional dependency** — your addon works standalone and only activates DMZ-specific behavior if DMZ is detected. DMZ itself uses this pattern for WorldGuard (`common/compat/WorldGuardCompat.java`): probe with `Class.forName("...")` in a try/catch during setup, flip an availability flag, and always guard feature calls behind that flag with a safe fallback. Recommended if your addon has value without DMZ installed.

## What you can safely rely on

- **Events** — `com.dragonminez.common.events.DMZEvent` is the intended public hook surface. See [[Implementation & Events|Implementation-&-Events]] for the full list.
- **Read-only player state** — `StatsCapability`/`StatsProvider`/`StatsData` for reading race, form, stats, and resources.
- **Registries** — `MainItems`, `MainBlocks`, `MainEntities`, `MainAttributes`, `MainEffects`, `MainEnchants` under `common/init/` expose `RegistryObject`/`DeferredRegister` references you can import directly.

## What to avoid relying on

- Client-reported values for anything progression-related — DMZ never trusts them either; read from the server-side capability instead.
- Directly mutating `StatsData` fields outside of events/commands. Prefer reacting to/cancelling events over poking values, and prefer DMZ's existing commands/network packets when you need to change something DMZ owns.
- Depending on `com.dragonminez.mixin` classes or DMZ internals not mentioned above — these can change without notice between versions.

## Next step

Once your dev environment builds against DMZ and you can see it detected at runtime, move on to [[Implementation & Events|Implementation-&-Events]] to wire up actual hooks, then [[Creating a DMZ Addon|Creating-a-DMZ-Addon]] for full Gradle/`mods.toml` setup.
