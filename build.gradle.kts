import java.text.SimpleDateFormat
import java.util.Date

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    java
    idea
    id("net.minecraftforge.gradle") version "6.0.30"
    id("org.parchmentmc.librarian.forgegradle") version "1.+"
    id("org.spongepowered.mixin") version "0.7.38"
}

val mod_version: String by project
val mod_group_id: String by project
val mod_id: String by project

version = mod_version
group = mod_group_id

base {
    archivesName.set(mod_id)
}

java {
    withSourcesJar()
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

println("Java: ${System.getProperty("java.version")}, " +
        "JVM: ${System.getProperty("java.vm.version")} (${System.getProperty("java.vendor")}), " +
        "Arch: ${System.getProperty("os.arch")}")


extensions.configure<org.spongepowered.asm.gradle.plugins.MixinExtension>("mixin") {
    add(sourceSets.main.get(), "dragonminez.refmap.json")
    config("dragonminez.mixins.json")
}

tasks.jarJar.configure {
    archiveClassifier.set("")
    finalizedBy("reobfJarJar")
}

tasks.jar.configure {
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
        url = uri("https://cursemaven.com")
    }
    maven {
        name = "ModMaven"
        url = uri("https://modmaven.dev")
    }
    mavenCentral()
}

val minecraft_version: String by project
val forge_version: String by project
val mapping_channel: String by project
val mapping_version: String by project
val jei_version: String by project

minecraft {
    mappings(mapping_channel, mapping_version)
    copyIdeResources.set(true)

    jarJar {
        enable()
    }

    accessTransformer(file("src/main/resources/META-INF/accesstransformer.cfg"))

    runs {
        configureEach {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            property("fml.earlyprogresswindow", "false")
            property("geckolib.disable_examples", "true")
            mods {
                create(mod_id) {
                    source(sourceSets.main.get())
                }
            }
        }

        create("client") {
            property("forge.logging.console.level", "debug")
        }

        create("server") {
            property("forge.logging.console.level", "debug")
        }

        create("gameTestServer") {
            property("forge.enabledGameTestNamespaces", mod_id)
        }

        create("data") {
            workingDirectory(project.file("run-data"))
            args("--mod", mod_id, "--all", "--output", file("src/generated/resources/"),
                 "--existing", file("src/main/resources/"))
        }
    }
}

dependencies {
    minecraft("net.minecraftforge:forge:$minecraft_version-$forge_version")
    annotationProcessor("org.spongepowered:mixin:0.8.7:processor")

    // Vulnerability corrections
    implementation("com.google.guava:guava:33.5.0-jre")
    implementation("io.netty:netty-codec:4.2.7.Final")
    implementation("io.netty:netty-handler:4.2.7.Final")
    implementation("org.apache.commons:commons-compress:1.27.1")

    // GeckoLib & Terrablender
    implementation(fg.deobf("software.bernie.geckolib:geckolib-forge-1.20.1:4.8.2"))
    implementation("com.eliotlash.mclib:mclib:20")
    implementation(fg.deobf("com.github.glitchfiend:TerraBlender-forge:1.20.1-3.0.1.10"))

    // Database Libraries for database storage lol
    jarJar(group = "org.mariadb.jdbc", name = "mariadb-java-client", version = "[3.0.8,3.1)") { jarJar.ranged(this, "[3.0.8,3.1)") }
    jarJar(group = "com.zaxxer", name = "HikariCP", version = "[7.0.2,5.0)") { jarJar.ranged(this, "[7.0.2,5.0)") }
    compileOnly("org.mariadb.jdbc:mariadb-java-client:3.0.8")
    compileOnly("com.zaxxer:HikariCP:7.0.2")

    // Dev utility mods (not included while building)
    // JEI for recipe viewing and testing, also we need the API to test the integration with the Kikono Station
    compileOnly(fg.deobf("mezz.jei:jei-$minecraft_version-common-api:$jei_version"))
    compileOnly(fg.deobf("mezz.jei:jei-$minecraft_version-forge-api:$jei_version"))
    runtimeOnly(fg.deobf("mezz.jei:jei-$minecraft_version-forge:$jei_version"))
    // Embeddium for optimizations, WorldEdit for in-game building, Cyanide for crash reporting
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

val minecraft_version_range: String by project
val forge_version_range: String by project
val loader_version_range: String by project
val mod_name: String by project
val mod_license: String by project
val mod_authors: String by project
val mod_description: String by project
val geckolib_version_range: String by project
val terrablender_version_range: String by project

tasks.named<ProcessResources>("processResources") {
    val replaceProperties = mapOf(
        "minecraft_version" to minecraft_version,
        "minecraft_version_range" to minecraft_version_range,
        "forge_version" to forge_version,
        "forge_version_range" to forge_version_range,
        "loader_version_range" to loader_version_range,
        "mod_id" to mod_id,
        "mod_name" to mod_name,
        "mod_license" to mod_license,
        "mod_version" to mod_version,
        "mod_authors" to mod_authors,
        "mod_description" to mod_description,
        "geckolib_version_range" to geckolib_version_range,
        "terrablender_version_range" to terrablender_version_range
    )

    inputs.properties(replaceProperties)

    filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta")) {
        expand(replaceProperties + mapOf("project" to project))
    }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
            "Specification-Title" to mod_id,
            "Specification-Vendor" to mod_authors,
            "Specification-Version" to mod_version,
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to mod_authors,
            "Implementation-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(Date())
        ))
    }
    finalizedBy("reobfJar")
}

