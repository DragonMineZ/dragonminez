import java.net.URI
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
    options.compilerArgs.addAll(listOf("-Xlint:-deprecation", "-Xlint:-removal"))
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
    maven {
        name = "Illusive Soulworks maven"
        url = uri("https://maven.theillusivec4.top/")
    }
    mavenCentral()
}

val minecraftVersion = requiredProp("minecraft_version")
val forgeVersion = requiredProp("forge_version")
val mappingChannelProp = requiredProp("mapping_channel")
val mappingVersionProp = requiredProp("mapping_version")
val jeiVersion = requiredProp("jei_version")
val requestedTasks = gradle.startParameter.taskNames.map { it.lowercase() }
// Client/dev-only mods belong on the classpath only when a client is actually launched.
// Keeping them out of every other task graph is critical for data generation: `build`
// re-runs runData, and codec-altering dev mods (e.g. Huge Structure Blocks' jigsaw
// limit patch) would otherwise bake values into the generated JSON that vanilla
// codecs reject at load time in production.
val clientRunRequested = requestedTasks.any { it.contains("runclient") }
val includeClientOnlyDevMods = providers.gradleProperty("includeClientOnlyDevMods")
    .map { it.toBoolean() }
    .orElse(clientRunRequested)

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
    implementation("com.google.guava:guava:33.6.0-jre") { because("Security/compat override requested.") }
    implementation("io.netty:netty-codec:4.2.7.Final") { because("Security/compat override requested.") }
    implementation("io.netty:netty-handler:4.2.7.Final") { because("Security/compat override requested.") }
    implementation("org.apache.commons:commons-compress:1.27.1") { because("Security/compat override requested.") }

    // GeckoLib, Terrablender & Curios
    implementation(fg.deobf("software.bernie.geckolib:geckolib-forge-1.20.1:4.8.3"))
    implementation("com.eliotlash.mclib:mclib:20")
    implementation(fg.deobf("com.github.glitchfiend:TerraBlender-forge:1.20.1-3.0.1.10"))
    compileOnly(fg.deobf("top.theillusivec4.curios:curios-forge:5.14.1+1.20.1:api"))
    runtimeOnly(fg.deobf("top.theillusivec4.curios:curios-forge:5.14.1+1.20.1"))

    // Source: https://mvnrepository.com/artifact/org.projectlombok/lombok
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")

    // Database libraries

    jarJar("org.mariadb.jdbc:mariadb-java-client:[3.5.8,)") {
        jarJar.ranged(
            this,
            "[3.5.7,)"
        )
    }
    jarJar("com.zaxxer:HikariCP:[7.0.2,)") { jarJar.ranged(this, "[7.0.2,)") }
    compileOnly("org.mariadb.jdbc:mariadb-java-client:3.5.8")
    compileOnly("com.zaxxer:HikariCP:7.0.2")

    testImplementation("org.junit.jupiter:junit-jupiter:6.1.0")

    // Dev utility mods
    compileOnly(fg.deobf("mezz.jei:jei-$minecraftVersion-common-api:$jeiVersion"))
    compileOnly(fg.deobf("mezz.jei:jei-$minecraftVersion-forge-api:$jeiVersion"))
    runtimeOnly(fg.deobf("curse.maven:worldedit-225608:4586218"))
    runtimeOnly(fg.deobf("curse.maven:cyanide-541676:5778405"))
    runtimeOnly(fg.deobf("curse.maven:spark-361579:4738952"))

    // Client-only visual/testing mods must stay off dedicated server and data runs.
    // Huge Structure Blocks in particular mixes into JigsawStructure's codec and
    // would poison generated structure JSON with out-of-vanilla-range values.
    if (includeClientOnlyDevMods.get()) {
        runtimeOnly(fg.deobf("curse.maven:huge-structure-blocks-474114:4803547"))
        runtimeOnly(fg.deobf("mezz.jei:jei-$minecraftVersion-forge:$jeiVersion"))
        runtimeOnly(fg.deobf("curse.maven:xenon-564239:5752040"))
        runtimeOnly(fg.deobf("curse.maven:oculus-581495:6020952"))
        runtimeOnly(fg.deobf("curse.maven:free-cam-557076:4643128"))
    }

    // Explorer's Compass and Nature's Compass for easier navigation during testing (structures, biomes)
    //runtimeOnly(fg.deobf("curse.maven:explorerscompass-491794:4712194"))
    //runtimeOnly(fg.deobf("curse.maven:naturecompass-252848:4712189"))

    // "Layers" mods for testing compatibility
    //runtimeOnly(fg.deobf("curse.maven:travelers-backpack-321117:7573110"))
    runtimeOnly(fg.deobf("curse.maven:cosmetic-armor-reworked-237307:4600191"))
    //runtimeOnly(fg.deobf("curse.maven:artifacts-312353:6399828"))
    //runtimeOnly(fg.deobf("curse.maven:cloth-config-api-348521:5729105"))
    //runtimeOnly(fg.deobf("curse.maven:architectury-api-419699:5137938"))
    //runtimeOnly(fg.deobf("curse.maven:expandability-465066:5301414"))
}

sourceSets.main {
    resources.srcDir("src/generated/resources/")
}

val generatedResourcesDir = layout.projectDirectory.dir("src/generated/resources")
val copyGeneratedResourcesToOutput by tasks.registering(Copy::class) {
    dependsOn("runData")
    from(generatedResourcesDir) {
        exclude(".cache/**")
    }
    into(layout.buildDirectory.dir("resources/main"))
}

tasks.named<Jar>("jar").configure {
    dependsOn(copyGeneratedResourcesToOutput)
}

tasks.named<Jar>("jarJar").configure {
    dependsOn(copyGeneratedResourcesToOutput)
}

//Helps with some AI Run Tests
tasks.named<JavaCompile>("compileTestJava").configure {
    mustRunAfter(copyGeneratedResourcesToOutput)
}

val minecraftVersionRange = requiredProp("minecraft_version_range")
val forgeVersionRange = requiredProp("forge_version_range")
val loaderVersionRange = requiredProp("loader_version_range")
val modName = requiredProp("mod_name")
val modLicense = requiredProp("mod_license")
val modAuthors = requiredProp("mod_authors")
val modCredits = requiredProp("mod_credits")
val modDescription = requiredProp("mod_description")
val geckolibVersionRange = requiredProp("geckolib_version_range")
val terrablenderVersionRange = requiredProp("terrablender_version_range")
val curiosVersionRange = requiredProp("curios_version_range")

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
        "mod_credits" to modCredits,
        "mod_description" to modDescription,
        "geckolib_version_range" to geckolibVersionRange,
        "terrablender_version_range" to terrablenderVersionRange,
        "curios_version_range" to curiosVersionRange
    )

    inputs.properties(replaceProperties)

    filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta")) {
        expand(replaceProperties + mapOf("project" to project))
    }
}

// ============================================================================
// Resource optimization via PackSquash (https://github.com/ComunidadAylas/PackSquash)
//
// Optimizes bundled textures (.png), sounds (.ogg) and JSON in `build/resources/main`
// before the jar is packed, so every produced jar (local, dev, release) is optimized
// identically. Options live in `packsquash.toml`; this task injects the build paths.
//
// Opt-in: runs automatically on CI (env CI is set by GitHub Actions); locally it is
// off by default to keep dev builds fast. Force on/off with -PoptimizeResources=true|false.
// ============================================================================
val packSquashVersion = "v0.4.1"

val optimizeResourcesEnabled: Provider<Boolean> =
    providers.gradleProperty("optimizeResources")
        .map { it.toBoolean() }
        .orElse(providers.environmentVariable("CI").map { it.equals("true", ignoreCase = true) || it == "1" })
        .orElse(true)

/** Escapes a path into a TOML basic string. */
fun tomlString(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val optimizeResources by tasks.registering {
    group = "build"
    description = "Optimizes bundled resources (PNG/OGG/JSON) with PackSquash before packaging."
    dependsOn("processResources")
    mustRunAfter(copyGeneratedResourcesToOutput)
    onlyIf { optimizeResourcesEnabled.get() }
    // Mutates resources/main in place; never treat as up-to-date.
    outputs.upToDateWhen { false }

    val resourcesOutput = layout.buildDirectory.dir("resources/main")
    val sourceResources = layout.projectDirectory.dir("src/main/resources")
    val workDir = layout.buildDirectory.dir("packsquash")
    val optionsFile = layout.projectDirectory.file("packsquash.toml")
    val version = packSquashVersion

    doLast {
        val resDir = resourcesOutput.get().asFile
        if (!resDir.exists()) error("Resources output not found: $resDir")
        val srcDir = sourceResources.asFile

        val work = workDir.get().asFile
        val stagingDir = File(work, "input")
        val outputZip = File(work, "optimized.zip")
        work.mkdirs()
        if (stagingDir.exists()) stagingDir.deleteRecursively()
        stagingDir.mkdirs()
        outputZip.delete()

        // 1) Stage PackSquash-friendly files. Textures/sounds/icons come from the
        //    PRISTINE source (assets are not template-expanded, so this is identical
        //    to the build output but guarantees idempotent re-runs — important so
        //    lossy OGG re-encoding never compounds across repeated local builds).
        //    Excluded on purpose:
        //      - shaders/: negligible savings; PackSquash's GLSL validator rejects
        //        some non-standard cores.
        //      - pack.mcmeta: the expanded copy contains literal control chars from
        //        § / \n escapes (Minecraft-tolerated, but invalid strict JSON);
        //        it has zero optimization value, so leave it untouched.
        //      - data/: mostly binary (nbt/mca) with negligible gains; skipped for speed.
        project.copy {
            from(srcDir) {
                include("assets/**")
                include("dmz_icon.png")
                include("dmz_logo.png")
                exclude("assets/**/shaders/**")
            }
            into(stagingDir)
        }

        // 2) Resolve (download + cache) the PackSquash binary for this OS/arch.
        val osName = System.getProperty("os.name").lowercase()
        val osArch = System.getProperty("os.arch").lowercase()
        val (asset, binaryName) = when {
            osName.contains("win") -> "packsquash.exe-x86_64-pc-windows-gnu.zip" to "packsquash.exe"
            osName.contains("mac") || osName.contains("darwin") -> "packsquash-universal2-apple-darwin.zip" to "packsquash"
            osArch.contains("aarch64") || osArch.contains("arm64") -> "packsquash-aarch64-unknown-linux-gnu.zip" to "packsquash"
            else -> "packsquash-x86_64-unknown-linux-gnu.zip" to "packsquash"
        }
        val toolDir = File(work, "bin/$version")
        val binary = File(toolDir, binaryName)
        if (!binary.exists()) {
            toolDir.mkdirs()
            val zipFile = File(toolDir, asset)
            val url = "https://github.com/ComunidadAylas/PackSquash/releases/download/$version/$asset"
            logger.lifecycle("Downloading PackSquash $version ($asset)...")
            URI(url).toURL().openStream().use { input ->
                zipFile.outputStream().use { output -> input.copyTo(output) }
            }
            project.copy {
                from(project.zipTree(zipFile))
                into(toolDir)
            }
            if (!binary.exists()) {
                val found = toolDir.walkTopDown().firstOrNull { it.isFile && it.name == binaryName }
                    ?: error("PackSquash binary '$binaryName' not found in $asset")
                found.copyTo(binary, overwrite = true)
            }
            if (!osName.contains("win")) binary.setExecutable(true)
        }

        // 3) Compose the settings file: dynamic paths + committed options.
        val settings = File(work, "settings.toml")
        settings.writeText(
            buildString {
                appendLine("pack_directory = ${tomlString(stagingDir.absolutePath)}")
                appendLine("output_file_path = ${tomlString(outputZip.absolutePath)}")
                appendLine()
                append(optionsFile.asFile.readText())
            }
        )

        // 4) Run PackSquash.
        val result = project.exec {
            commandLine(binary.absolutePath, settings.absolutePath)
            isIgnoreExitValue = true
        }
        if (result.exitValue != 0) error("PackSquash failed with exit code ${result.exitValue}")
        if (!outputZip.exists()) error("PackSquash did not produce output: $outputZip")

        // 5) Overlay optimized files back over resources/main. PackSquash drops file
        //    types it does not recognize (.mca, .icns, ...); an overlay copy (no delete)
        //    keeps those originals while replacing png/ogg/json/mcmeta with smaller ones.
        fun dirSize(dir: File) = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        val before = dirSize(resDir)
        project.copy {
            from(project.zipTree(outputZip))
            into(resDir)
        }
        val after = dirSize(resDir)
        logger.lifecycle("PackSquash: resources/main ${before / 1024} KiB -> ${after / 1024} KiB")
    }
}

tasks.named<Jar>("jar").configure { dependsOn(optimizeResources) }
tasks.named<Jar>("jarJar").configure { dependsOn(optimizeResources) }

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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<Jar>().configureEach {
    manifest {
        val attrs = linkedMapOf<String, Any>(
            "Specification-Title" to modId,
            "Specification-Vendor" to modAuthors,
            "Specification-Version" to modVersion,
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version.toString(),
            "Implementation-Vendor" to modAuthors,
            "MixinConfigs" to "dragonminez.mixins.json"
        )

        if (includeTimestamp.get()) {
            attrs["Implementation-Timestamp"] =
                OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        }

        attributes(attrs)
    }
}

tasks.named<Jar>("jar").configure {
    finalizedBy("reobfJar")
}
