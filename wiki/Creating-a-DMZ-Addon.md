# Creating a DMZ Addon

This page is the starting point for a full Forge addon that integrates with DragonMineZ.

## Goal

Build your own mod jar that depends on DMZ and can react to DMZ events and data.

## Step 1 - Create a Forge 1.20.1 project

Use Forge for Minecraft `1.20.1` version `6.0.30` and Java `17`, matching DMZ.

Useful references from this repo for building your project structure and setup:

- `gradle.properties`
- `build.gradle.kts`
- `src/main/resources/META-INF/mods.toml`

## Step 1.1 - Set up your gradle

For our convenience, we use Groovy Gradle for DMZ, but you can use Kotlin Gradle 
or any other setup you prefer for your addon. Just make sure to match the Forge version and Java version.

`build.gradle.kts` example:

```groovy
plugins {
    java
    idea
    id("net.minecraftforge.gradle") version "6.0.30"
    id("org.parchmentmc.librarian.forgegradle") version "1.+"
    id("org.spongepowered.mixin") version "0.7.38"
}
//...
dependencies {
    minecraft("net.minecraftforge:forge:$minecraftVersion-$forgeVersion")
    annotationProcessor("org.spongepowered:mixin:0.8.7:processor")
    //...

    // GeckoLib & Terrablender
    implementation(fg.deobf("software.bernie.geckolib:geckolib-forge-1.20.1:4.8.3"))
    implementation("com.eliotlash.mclib:mclib:20")
    implementation(fg.deobf("com.github.glitchfiend:TerraBlender-forge:1.20.1-3.0.1.10"))
}
```
## Step 2 - Declare DMZ dependency in `mods.toml`

In your addon `mods.toml`, add a dependency entry for `dragonminez`.

```toml
[[dependencies.your_addon_modid]]
    modId="dragonminez"
    mandatory=true # This has to be true...
    versionRange="[2.0.3]" # Only allow the latest version to avoid compatibility issues.
    ordering="AFTER"
    side="BOTH" # Choose between "CLIENT", "SERVER", or "BOTH" based on your addon needs.
```

## Step 3 - Add DMZ API classes to compile classpath

If you do not have a published Maven artifact yet, use a local deobf jar during development.

Typical options:

- `compileOnly` DMZ jar in `libs/`
- `runtimeOnly` DMZ jar for local run configs

Keep this flexible based on how your team publishes DMZ builds.

## Step 4 - Listen to DMZ events

DMZ publishes some forge events under `com.dragonminez.common.events.DMZEvent`. Some of them are on other classes,
depending on wht you want to add/modify/remove.

Example:

```java
@Mod.EventBusSubscriber(modid = "your_addon_modid", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DmzHooks {
    @SubscribeEvent
    public static void onTpGain(com.dragonminez.common.events.DMZEvent.TPGainEvent event) {
        // Example: increase TP gain by 10%
        int boosted = Math.round(event.getTpGain() * 1.10f);
        event.setTpGain(boosted);
    }
}
```

Event source file:

- `src/main/java/com/dragonminez/common/events/DMZEvent.java`

## Suggested addon structure

- `your/addon/MainModClass.java`
- `your/addon/event/DmzHooks.java`
- `src/main/resources/META-INF/mods.toml`
- `src/main/resources/pack.mcmeta`

## First milestone checklist

1. Addon loads without crash in dev client.
2. DMZ is detected when present.
3. One DMZ event is received in logs.
4. One behavior change is visible in-game.

## Compatibility recommendations

- Guard DMZ-only code paths behind mod-presence checks.
- Log clear warnings when DMZ is missing or version is incompatible.

