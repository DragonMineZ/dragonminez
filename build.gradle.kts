import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.language.jvm.tasks.ProcessResources
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

plugins {
    java
    idea
    id("net.minecraftforge.gradle") version "6.0.30"
    id("org.parchmentmc.librarian.forgegradle") version "1.+"
    id("org.spongepowered.mixin") version "0.7.38"
}

/** Fail-fast property access (keeps the build deterministic and debuggable). */
fun requiredProp(name: String): String =
    providers.gradleProperty(name).orNull
        ?: error("Missing Gradle property '$name'. Add it to gradle.properties or pass -P$name=...")

val modVersion = requiredProp("mod_version")
val modGroupId = requiredProp("mod_group_id")
val modId = requiredProp("mod_id")

version = modVersion
group = modGroupId

base {
    archivesName.set(modId)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withSourcesJar()
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}

/**
 * (Optional) Enable with: -PprintBuildInfo=true
 */
val printBuildInfo: Provider<Boolean> =
    providers.gradleProperty("printBuildInfo")
        .map { it.toBoolean() }
        .orElse(false)

tasks.register("printBuildInfo") {
    onlyIf { printBuildInfo.get() }
    doLast {
        logger.lifecycle(
            "Java: ${System.getProperty("java.version")}, " +
                    "JVM: ${System.getProperty("java.vm.version")} (${System.getProperty("java.vendor")}), " +
                    "Arch: ${System.getProperty("os.arch")}"
        )
    }
}
tasks.named("build").configure { dependsOn("printBuildInfo") }

extensions.configure<org.spongepowered.asm.gradle.plugins.MixinExtension>("mixin") {
    add(sourceSets.main.get(), "dragonminez.refmap.json")
    config("dragonminez.mixins.json")
}

tasks.named<Jar>("jarJar").configure {
    archiveClassifier.set("")
    finalizedBy("reobfJarJar")
}

tasks.named<Jar>("jar").configure {
    archiveClassifier.set("slim")
}

repositories {
    maven {
        name = "GeckoLib"
        url = uri("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
        content {
            includeGroupByRegex("software\\.bernie.*")
            includeGroup("com.eliotlash.mclib")
        }
    }
    maven {
        name = "Jared's maven"
        url = uri("https://maven.blamejared.com/")
    }
    maven {
        name = "CurseMaven"
        url = uri("https://cursemaven.com")
        content { includeGroup("curse.maven") }
    }
    maven {
        name = "ModMaven"
        url = uri("https://modmaven.dev")
    }
    mavenCentral()
}

val minecraftVersion = requiredProp("minecraft_version")
val forgeVersion = requiredProp("forge_version")
val mappingChannelProp = requiredProp("mapping_channel")
val mappingVersionProp = requiredProp("mapping_version")
val jeiVersion = requiredProp("jei_version")

minecraft {
    mappings(mappingChannelProp, mappingVersionProp)
    copyIdeResources.set(true)

    jarJar { enable() }

    accessTransformer(file("src/main/resources/META-INF/accesstransformer.cfg"))

    runs {
        configureEach {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            property("fml.earlyprogresswindow", "false")
            property("geckolib.disable_examples", "true")
            mods {
                create(modId) {
                    source(sourceSets.main.get())
                }
            }
        }

        create("client") { /* inherits defaults */ }
        create("server") { /* inherits defaults */ }

        create("gameTestServer") {
            property("forge.enabledGameTestNamespaces", modId)
        }

        create("data") {
            workingDirectory(project.file("run-data"))
            args(
                "--mod", modId,
                "--all",
                "--output", file("src/generated/resources/"),
                "--existing", file("src/main/resources/")
            )
        }
    }
}

dependencies {
    minecraft("net.minecraftforge:forge:$minecraftVersion-$forgeVersion")

    annotationProcessor("org.spongepowered:mixin:0.8.7:processor")

    // Vulnerability corrections
    implementation("com.google.guava:guava:33.5.0-jre") { because("Security/compat override requested.") }
    implementation("io.netty:netty-codec:4.2.7.Final") { because("Security/compat override requested.") }
    implementation("io.netty:netty-handler:4.2.7.Final") { because("Security/compat override requested.") }
    implementation("org.apache.commons:commons-compress:1.27.1") { because("Security/compat override requested.") }

    // GeckoLib & Terrablender
    implementation(fg.deobf("software.bernie.geckolib:geckolib-forge-1.20.1:4.8.3"))
    implementation("com.eliotlash.mclib:mclib:20")
    implementation(fg.deobf("com.github.glitchfiend:TerraBlender-forge:1.20.1-3.0.1.10"))

    // Database libraries
    jarJar(group = "org.mariadb.jdbc", name = "mariadb-java-client", version = "[3.5.7,)") { jarJar.ranged(this, "[3.5.7,)") }
    jarJar(group = "com.zaxxer", name = "HikariCP", version = "[7.0.2,)") { jarJar.ranged(this, "[7.0.2,)") }
    compileOnly("org.mariadb.jdbc:mariadb-java-client:3.5.7")
    compileOnly("com.zaxxer:HikariCP:7.0.2")

    // Dev utility mods
    compileOnly(fg.deobf("mezz.jei:jei-$minecraftVersion-common-api:$jeiVersion"))
    compileOnly(fg.deobf("mezz.jei:jei-$minecraftVersion-forge-api:$jeiVersion"))
    runtimeOnly(fg.deobf("mezz.jei:jei-$minecraftVersion-forge:$jeiVersion"))

    runtimeOnly(fg.deobf("org.embeddedt:embeddium-1.20.1:0.3.9-git.f603a93+mc1.20.1"))
    runtimeOnly(fg.deobf("curse.maven:worldedit-225608:4586218"))
    runtimeOnly(fg.deobf("curse.maven:cyanide-541676:5778405"))

    // Explorer's Compass and Nature's Compass for easier navigation during testing (structures, biomes)
    //runtimeOnly(fg.deobf("curse.maven:explorerscompass-491794:4712194"))
    //runtimeOnly(fg.deobf("curse.maven:naturecompass-252848:4712189"))
    // Armors mods for testing armor layer on Oozaru/Majin models, we may delete this once fully finished
    //runtimeOnly(fg.deobf("curse.maven:fantasy-armor-1083998:7328423"))
    //runtimeOnly(fg.deobf("curse.maven:epic-paladins-635165:6227566"))
}

sourceSets.main {
    resources.srcDir("src/generated/resources/")
}

val minecraftVersionRange = requiredProp("minecraft_version_range")
val forgeVersionRange = requiredProp("forge_version_range")
val loaderVersionRange = requiredProp("loader_version_range")
val modName = requiredProp("mod_name")
val modLicense = requiredProp("mod_license")
val modAuthors = requiredProp("mod_authors")
val modDescription = requiredProp("mod_description")
val geckolibVersionRange = requiredProp("geckolib_version_range")
val terrablenderVersionRange = requiredProp("terrablender_version_range")

tasks.named<ProcessResources>("processResources").configure {
    filteringCharset = "UTF-8"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    val replaceProperties = mapOf(
        "minecraft_version" to minecraftVersion,
        "minecraft_version_range" to minecraftVersionRange,
        "forge_version" to forgeVersion,
        "forge_version_range" to forgeVersionRange,
        "loader_version_range" to loaderVersionRange,
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_license" to modLicense,
        "mod_version" to modVersion,
        "mod_authors" to modAuthors,
        "mod_description" to modDescription,
        "geckolib_version_range" to geckolibVersionRange,
        "terrablender_version_range" to terrablenderVersionRange
    )

    inputs.properties(replaceProperties)

    filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta")) {
        expand(replaceProperties + mapOf("project" to project))
    }
}

/**
 * Optional manifest timestamp
 * Enable with: -PincludeTimestamp=true
 */
val includeTimestamp: Provider<Boolean> =
    providers.gradleProperty("includeTimestamp")
        .map { it.toBoolean() }
        .orElse(false)

tasks.named("build") {
    dependsOn("reobfJar", "reobfJarJar")
}

tasks.named<Jar>("jar").configure {
    doFirst {
        val attrs = linkedMapOf<String, Any>(
            "Specification-Title" to modId,
            "Specification-Vendor" to modAuthors,
            "Specification-Version" to modVersion,
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version.toString(),
            "Implementation-Vendor" to modAuthors
        )

        if (includeTimestamp.get()) {
            attrs["Implementation-Timestamp"] =
                OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        }

        manifest.attributes(attrs)
    }

    finalizedBy("reobfJar")
}
