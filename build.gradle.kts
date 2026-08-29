import java.net.URI
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import groovy.json.JsonSlurper

plugins {
    java
    idea
    id("net.neoforged.moddev") version "2.0.143"
    id("io.github.goooler.shadow") version "8.1.8"
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

// Mojang ships Java 21 from 1.20.5+; NeoForge 1.21.1 requires it.
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:-deprecation", "-Xlint:-removal", "-Xmaxerrs", "10000", "-Xmaxwarns", "10000"))
    options.encoding = "UTF-8"
    options.release.set(21)
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

val minecraftVersion = requiredProp("minecraft_version")
val neoVersion = requiredProp("neo_version")
val parchmentMinecraftVersion = requiredProp("parchment_minecraft_version")
val parchmentMappingsVersion = requiredProp("parchment_mappings_version")
val jeiVersion = requiredProp("jei_version")
val geckolibVersion = requiredProp("geckolib_version")
val terrablenderVersion = requiredProp("terrablender_version")
val curiosVersion = requiredProp("curios_version")

val requestedTasks = gradle.startParameter.taskNames.map { it.lowercase() }
// Client/dev-only optional mods only when launching the client.
val clientRunRequested = requestedTasks.any { it.contains("runclient") }
val includeClientOnlyDevMods = providers.gradleProperty("includeClientOnlyDevMods")
    .map { it.toBoolean() }
    .orElse(clientRunRequested)
val serverGameDirectory = providers.gradleProperty("serverGameDirectory").orElse("run")

neoForge {
    version = neoVersion

    parchment {
        minecraftVersion = parchmentMinecraftVersion
        mappingsVersion = parchmentMappingsVersion
    }

    runs {
        register("client") {
            client()
            gameDirectory = file("run")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
            systemProperty("geckolib.disable_examples", "true")
        }
        register("server") {
            server()
            gameDirectory = file(serverGameDirectory.get())
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
            systemProperty("geckolib.disable_examples", "true")
        }
        register("gameTestServer") {
            type = "gameTestServer"
            gameDirectory = file("run")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        register("data") {
            data()
            gameDirectory = file("run-data")
            programArguments.addAll(
                "--mod", modId,
                "--all",
                "--output", file("src/generated/resources/").absolutePath,
                "--existing", file("src/main/resources/").absolutePath
            )
        }
        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }

    unitTest {
        testedMod = mods[modId]
        enable()
    }
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
        name = "TerraBlender / Forge"
        url = uri("https://maven.minecraftforge.net/")
        content {
            includeGroup("com.github.glitchfiend")
        }
    }
    maven {
        name = "Illusive Soulworks maven"
        url = uri("https://maven.theillusivec4.top/")
    }
    mavenCentral()
}

// Optional runtime-only configuration (not published as a hard dependency).
val localRuntime by configurations.creating
configurations.named("runtimeClasspath") { extendsFrom(localRuntime) }
val mariaRelocation by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

dependencies {
    // Mandatory libraries — coordinates verified on official Maven metadata (HTTP 200).
    implementation("software.bernie.geckolib:geckolib-neoforge-1.21.1:$geckolibVersion")
    implementation("com.github.glitchfiend:TerraBlender-neoforge:$terrablenderVersion")
    compileOnly("top.theillusivec4.curios:curios-neoforge:$curiosVersion:api")
    runtimeOnly("top.theillusivec4.curios:curios-neoforge:$curiosVersion")

    // Source: https://mvnrepository.com/artifact/org.projectlombok/lombok
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")

    // Database libraries (pure Java; jar-in-jar for distribution).
    // Exclude HikariCP's slf4j pin so it does not fight Minecraft's strictly 2.0.9.
    // Versions verified on Maven Central (same pins as pre-port project).
    compileOnly("org.mariadb.jdbc:mariadb-java-client:3.5.9")
    jarJar("org.mariadb.jdbc:mariadb-java-client:3.5.9")
    add(mariaRelocation.name, "org.mariadb.jdbc:mariadb-java-client:3.5.9")
    compileOnly("com.zaxxer:HikariCP:7.1.0") {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    jarJar("com.zaxxer:HikariCP:7.1.0") {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    testImplementation("org.junit.jupiter:junit-jupiter:6.1.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("top.theillusivec4.curios:curios-neoforge:$curiosVersion:api")

    // JEI API only (optional compile). Full JEI for client runs is optional localRuntime.
    compileOnly("mezz.jei:jei-$minecraftVersion-common-api:$jeiVersion")
    compileOnly("mezz.jei:jei-$minecraftVersion-neoforge-api:$jeiVersion")

    if (includeClientOnlyDevMods.get()) {
        add("localRuntime", "mezz.jei:jei-$minecraftVersion-neoforge:$jeiVersion")
    }
}

sourceSets.main {
    resources.srcDir("src/generated/resources/")
}

fun validateJsonFiles(files: Collection<File>, label: String) {
    val failures = mutableListOf<String>()
    files.asSequence()
        .filter { it.isFile && it.extension.equals("json", ignoreCase = true) }
        .sortedBy { it.invariantSeparatorsPath }
        .forEach { file ->
            try {
                JsonSlurper().parse(file)
            } catch (exception: Exception) {
                failures += "${file.relativeTo(projectDir).invariantSeparatorsPath}: ${exception.message}"
            }
        }
    if (failures.isNotEmpty()) {
        throw GradleException("Malformed JSON in $label:\n" + failures.joinToString("\n"))
    }
}

val sourceJsonFiles = files(
    fileTree("src/main/resources") { include("**/*.json") },
    fileTree("src/generated/resources") { include("**/*.json") }
)

val validateJsonResources by tasks.registering {
    group = "verification"
    description = "Parses every JSON file in the source and generated resource roots."
    inputs.files(sourceJsonFiles)
    doLast { validateJsonFiles(sourceJsonFiles.files, "resource source roots") }
}

val generatedResourcesDir = layout.projectDirectory.dir("src/generated/resources")
val copyGeneratedResourcesToOutput by tasks.registering(Copy::class) {
    // Datagen runs before release packaging so clean checkouts do not silently
    // omit DMZ's dimensions, biomes, structures, recipes and registry tags.
    from(generatedResourcesDir) {
        exclude(".cache/**")
    }
    into(layout.buildDirectory.dir("resources/main"))
    includeEmptyDirs = false
    onlyIf { generatedResourcesDir.asFile.exists() }
}
val generateReleaseData = tasks.named("runData")
copyGeneratedResourcesToOutput.configure {
    mustRunAfter(generateReleaseData)
}

val validateGeneratedJsonAfterData by tasks.registering {
    group = "verification"
    description = "Parses generated JSON after runData completes."
    dependsOn(generateReleaseData)
    mustRunAfter(generateReleaseData)
    val generatedJson = fileTree(generatedResourcesDir) { include("**/*.json") }
    inputs.files(generatedJson)
    doLast { validateJsonFiles(generatedJson.files, "post-datagen resources") }
}

val validateProcessedJson by tasks.registering {
    group = "verification"
    description = "Parses every processed JSON resource immediately before packaging."
    dependsOn("processResources", copyGeneratedResourcesToOutput)
    mustRunAfter(copyGeneratedResourcesToOutput)
    val processedJson = fileTree(layout.buildDirectory.dir("resources/main")) { include("**/*.json") }
    inputs.files(processedJson)
    doLast { validateJsonFiles(processedJson.files, "processed resources") }
}

tasks.named<Jar>("jar").configure {
    dependsOn(generateReleaseData, copyGeneratedResourcesToOutput, validateGeneratedJsonAfterData, validateProcessedJson)
    archiveClassifier.set("")
}

tasks.named<Jar>("sourcesJar").configure {
    // Four critical spawn tags are also kept in src/main/resources so clean
    // development runs work before datagen; avoid duplicating them in sources.
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

//Helps with some AI Run Tests
tasks.named<JavaCompile>("compileTestJava").configure {
    mustRunAfter(copyGeneratedResourcesToOutput)
}

val minecraftVersionRange = requiredProp("minecraft_version_range")
val neoVersionRange = requiredProp("neo_version_range")
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
    dependsOn(validateJsonResources)
    filteringCharset = "UTF-8"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    includeEmptyDirs = false

    val replaceProperties = mapOf(
        "minecraft_version" to minecraftVersion,
        "minecraft_version_range" to minecraftVersionRange,
        "neo_version" to neoVersion,
        "neo_version_range" to neoVersionRange,
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

    filesMatching(listOf("META-INF/neoforge.mods.toml", "pack.mcmeta")) {
        expand(replaceProperties + mapOf("project" to project))
    }
}

// ============================================================================
// Resource optimization via PackSquash (https://github.com/ComunidadAylas/PackSquash)
// Enabled by default for release-sized artifacts. Override with
// -PoptimizeResources=false only when iterating on raw resource output.
// ============================================================================
val packSquashVersion = "v0.4.1"

val optimizeResourcesEnabled: Provider<Boolean> =
    providers.gradleProperty("optimizeResources")
        .map { it.toBoolean() }
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

        project.copy {
            from(srcDir) {
                include("assets/**")
                include("dmz_icon.png")
                include("dmz_logo.png")
                exclude("assets/**/shaders/**")
            }
            into(stagingDir)
        }

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

        val settings = File(work, "settings.toml")
        settings.writeText(
            buildString {
                appendLine("pack_directory = ${tomlString(stagingDir.absolutePath)}")
                appendLine("output_file_path = ${tomlString(outputZip.absolutePath)}")
                appendLine()
                append(optionsFile.asFile.readText())
            }
        )

        val result = project.exec {
            commandLine(binary.absolutePath, settings.absolutePath)
            isIgnoreExitValue = true
        }
        if (result.exitValue != 0) error("PackSquash failed with exit code ${result.exitValue}")
        if (!outputZip.exists()) error("PackSquash did not produce output: $outputZip")

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

// Youer ships MariaDB packages inside LibrariesVault. Relocate our Connector/J
// packages in-place before the normal jar is assembled so one artifact works on
// both standard NeoForge and hybrid servers without a JPMS split-package failure.
val relocateMariaDb by tasks.registering(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    group = "build"
    description = "Relocates the bundled MariaDB driver to avoid hybrid-server module conflicts."
    dependsOn("jarJar")
    configurations = listOf(mariaRelocation)
    archiveFileName.set("mariadb-java-client-3.5.9.jar")
    destinationDirectory.set(layout.buildDirectory.dir("generated/jarJar/META-INF/jarjar"))
    relocate("org.mariadb.jdbc", "com.dragonminez.libs.mariadb.jdbc")
    mergeServiceFiles()
    exclude("module-info.class")
    exclude("META-INF/versions/**/module-info.class")
    manifest {
        attributes["Automatic-Module-Name"] = "com.dragonminez.mariadb"
    }
}
tasks.named<Jar>("jar").configure { dependsOn(relocateMariaDb) }

/**
 * Optional manifest timestamp
 * Enable with: -PincludeTimestamp=true
 */
val includeTimestamp: Provider<Boolean> =
    providers.gradleProperty("includeTimestamp")
        .map { it.toBoolean() }
        .orElse(false)

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
            "MixinConfigs" to "dragonminez.mixins.json,dragonminez.sable.mixins.json"
        )

        if (includeTimestamp.get()) {
            attrs["Implementation-Timestamp"] =
                OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        }

        attributes(attrs)
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
