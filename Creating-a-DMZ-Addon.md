# Creating a DMZ Addon

This page is the starting point for a full Forge addon that integrates with DragonMineZ. Read [[Getting Started|Getting-Started]] first for the required toolchain and versions.

## Goal

Build your own mod jar that depends on DMZ and can react to DMZ events and data.

## Step 1 - Create a Forge 1.20.1 project

Start from the Forge MDK for Minecraft `1.20.1` with Forge `47.4.10`, ForgeGradle `6.0.30` and Java `17`, matching DMZ.

Useful references from the DMZ repository for building your project structure and setup:

- `gradle.properties`
- `build.gradle.kts`
- `src/main/resources/META-INF/mods.toml`

## Step 1.1 - Set up your gradle

DMZ uses the Kotlin Gradle DSL (`build.gradle.kts`), but you can use Groovy Gradle or any other setup you prefer for your addon. Just make sure to match the Forge version and Java version.

`build.gradle.kts` example:

```kotlin
plugins {
    java
    idea
    id("net.minecraftforge.gradle") version "6.0.30"
    id("org.parchmentmc.librarian.forgegradle") version "1.+"
    id("org.spongepowered.mixin") version "0.7.38"
}

repositories {
    maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/") // GeckoLib
    maven("https://maven.blamejared.com/")                             // TerraBlender
    maven("https://maven.theillusivec4.top/")                          // Curios
    flatDir { dirs("libs") }                                            // local DMZ jar
    mavenCentral()
}

minecraft {
    mappings("parchment", "2023.09.03-1.20.1")
    runs {
        configureEach {
            workingDirectory(project.file("run"))
            // DMZ ships mixins: let the dev runtime remap their refmap
            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", "${projectDir}/build/createSrgToMcp/output.srg")
        }
        create("client")
        create("server")
    }
}

dependencies {
    minecraft("net.minecraftforge:forge:1.20.1-47.4.10")
    annotationProcessor("org.spongepowered:mixin:0.8.7:processor")

    // DMZ's mandatory dependencies: GeckoLib, TerraBlender & Curios
    implementation(fg.deobf("software.bernie.geckolib:geckolib-forge-1.20.1:4.8.3"))
    implementation("com.eliotlash.mclib:mclib:20")
    implementation(fg.deobf("com.github.glitchfiend:TerraBlender-forge:1.20.1-3.0.1.10"))
    compileOnly(fg.deobf("top.theillusivec4.curios:curios-forge:5.14.1+1.20.1:api"))
    runtimeOnly(fg.deobf("top.theillusivec4.curios:curios-forge:5.14.1+1.20.1"))

    // DragonMineZ itself (libs/dragonminez-2.2-alpha.jar)
    implementation(fg.deobf("local:dragonminez:2.2-alpha"))
}
```

## Step 2 - Declare DMZ dependency in `mods.toml`

In your addon `mods.toml`, add a dependency entry for `dragonminez`.

```toml
[[dependencies.your_addon_modid]]
    modId="dragonminez"
    mandatory=true # false if your addon also works without DMZ
    versionRange="[2.2-alpha]" # Only allow the DMZ version you built against to avoid compatibility issues.
    ordering="AFTER"
    side="BOTH" # Choose between "CLIENT", "SERVER", or "BOTH" based on your addon needs.
```

DMZ's own `mods.toml` uses `displayTest = "MATCH_VERSION"`, so the client and server must run the same DMZ version anyway.

## Step 3 - Add DMZ to the compile classpath

DMZ's build does not publish a Maven artifact, so use a local jar during development:

1. Download the DMZ release you target, or build it yourself with `./gradlew build` in the DMZ repository.
2. Use the **no-classifier** jar (`dragonminez-<version>.jar`) — it bundles DMZ's database libraries. The `-slim` jar is not meant to be used.
3. Put it in your project's `libs/` folder and reference it through `fg.deobf(...)` as shown above, so ForgeGradle remaps it to your dev mappings.

Use `compileOnly` instead of `implementation` if you only want to compile against DMZ and run it from a separate instance.

## Step 4 - Listen to DMZ events

DMZ publishes its server/common Forge events as nested classes of `com.dragonminez.common.events.DMZEvent`, and its client events as nested classes of `com.dragonminez.client.events.DMZClientEvent`.

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

Event source files:

- `src/main/java/com/dragonminez/common/events/DMZEvent.java`
- `src/main/java/com/dragonminez/client/events/DMZClientEvent.java`

The full list, plus quest objective/reward registries, text placeholders and utility menu slots, is on [[Implementation & Events|Implementation-&-Events]].

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
- Don't bundle or depend on Legendary Tooltips, Epic Fight or Better Combat — DMZ refuses to start alongside them.
- Use your own network channel and never trust client input for progression (see [[Implementation & Events|Implementation-&-Events#networking]]).
