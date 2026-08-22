import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.apache.commons.lang3.SystemUtils
import java.io.File
import java.net.URI
import java.security.MessageDigest

plugins {
    idea
    java
    id("gg.essential.loom") version "0.10.0.5"
    id("dev.architectury.architectury-pack200") version "0.1.3"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.gorylenko.gradle-git-properties") version "2.3.2"

//    kotlin("jvm") version "2.0.0-Beta4"
}

//Constants:

val baseGroup: String by project
val mcVersion: String by project
val version: String by project
val mixinGroup = "$baseGroup.forge.mixin"
val modid: String by project

// Toolchains:
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

val accessTransformerName = "fpsmaster_at.cfg"
// Minecraft configuration:
loom {
    log4jConfigs.from(file("log4j2.xml"))
    launchConfigs {
        "client" {
            // If you don't want mixins, remove these lines
            property("mixin.debug", "true")
            property("asmhelper.verbose", "true")
            arg("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
        }
    }
    runConfigs {
        "client" {
            if (SystemUtils.IS_OS_MAC_OSX) {
                // This argument causes a crash on macOS
                vmArgs.remove("-XstartOnFirstThread")
            }
        }
        remove(getByName("server"))
    }
    forge {
        accessTransformer(rootProject.file("src/main/resources/$accessTransformerName"))
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
        // If you don't want mixins, remove this lines
        mixinConfig("mixins.$modid.json")
    }
    // If you don't want mixins, remove these lines
    mixin {
        defaultRefmapName.set("mixins.$modid.refmap.json")
    }
}

sourceSets.main {
    output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory })
}

// Dependencies:

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.spongepowered.org/maven/")
    // If you don't want to log in with your real minecraft account, remove this line
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")

}

val shadowImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")

    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

    // If you don't want mixins, remove these lines
    shadowImpl("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT")
    shadowImpl("org.java-websocket:Java-WebSocket:1.5.4") {
        isTransitive = true
    }
    // get rid of kotlin
//    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
//    shadowImpl("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.0-Beta4"){
//        isTransitive = true
//    }
    shadowImpl("org.slf4j:slf4j-api:2.0.6") {
        isTransitive = false
    }

    shadowImpl("top.fpsmaster:prism:0.1.0")

    // 音乐能力：Cadence 数据客户端（网易云/QQ 搜索/直链/歌词/歌单/登录）。
    // 由 JitPack 托管 (FPSMasterTeam/Cadence)，坐标即 com.github.<owner>:<repo>:<tag>；
    // 源码里的包名仍是 top.fpsmaster.music.*。
    // gson 由 MC classpath 提供，故不传递依赖；Kotlin 运行时单独引入。
    shadowImpl("com.github.FPSMasterTeam:Cadence:v0.1.1") {
        isTransitive = false
    }
    shadowImpl("org.jetbrains.kotlin:kotlin-stdlib:2.4.0") {
        isTransitive = true
    }
    // mp3 解码（javax.sound SPI）：jlayer + tritonus-share，纯 Java，兼容 Java 8。
    shadowImpl("com.googlecode.soundlibs:mp3spi:1.9.5.4") {
        isTransitive = true
    }
    // 登录二维码生成（网易云 codekey URL → QR 图）：zxing，纯 Java。
    shadowImpl("com.google.zxing:core:3.5.3") {
        isTransitive = false
    }
    // Windows SMTC 桥接：JNA 加载原生 DLL 调用 WinRT。
    shadowImpl("net.java.dev.jna:jna:5.17.0") {
        isTransitive = true
    }
    // If you don't want to log in with your real minecraft account, remove this line
    runtimeOnly("me.djtheredstoner:DevAuth-forge-legacy:1.1.2")
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
    compileOnly("org.jetbrains:annotations:26.0.2")
    annotationProcessor("org.jetbrains:annotations:26.0.2")

}

// Tasks:

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}

tasks.withType(Jar::class) {
    archiveBaseName.set(modid)
    manifest.attributes.run {
        this["FMLCorePluginContainsFMLMod"] = "true"
        this["ForceLoadAsMod"] = "true"

        // If you don't want mixins, remove these lines
        this["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
        this["FMLAT"] = accessTransformerName
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.processResources {
    inputs.property("version", version)
    inputs.property("mcversion", mcVersion)
    inputs.property("modid", modid)
    inputs.property("mixinGroup", mixinGroup)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    dependsOn(tasks.generateGitProperties)
    filesMatching(listOf("mcmod.info", "mixins.$modid.json")) {
        expand(inputs.properties)
    }

    rename("(.+_at.cfg)", "META-INF/$1")
}

gitProperties {
    gitPropertiesResourceDir = project.file("src/main/resources")
    gitPropertiesDir = project.file("src/main/resources")
    gitPropertiesName = "git.properties"
    keys = arrayOf("git.branch", "git.commit.id", "git.commit.time", "git.commit.id.abbrev").toMutableList()
}


val remapJar by tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    archiveClassifier.set("")
    from(tasks.shadowJar)
    input.set(tasks.shadowJar.get().archiveFile)
}

tasks.jar {
    archiveClassifier.set("without-deps")
    destinationDirectory.set(layout.buildDirectory.dir("badjars"))
}

tasks.shadowJar {
    destinationDirectory.set(layout.buildDirectory.dir("badjars"))
    archiveClassifier.set("all-dev")
    configurations = listOf(shadowImpl)
    // 合并 META-INF/services，否则 mp3spi 的 javax.sound SPI provider 不会被注册，mp3 无法解码。
    mergeServiceFiles()
    doLast {
        configurations.forEach {
            println("Copying jars into mod: ${it.files}")
        }
    }

    // If you want to include other dependencies and shadow them, you can relocate them in here
    fun relocate(name: String) = relocate(name, "$baseGroup.deps.$name")
}


tasks.assemble.get().dependsOn(tasks.remapJar)



// ============================================================================
// Stage-0 POC: prove the 1.8.9 client boots with NO Forge runtime —
// raw LaunchWrapper + our own ITweaker + Sponge Mixin, hitting an MCP-named mixin.
// Fully isolated from the Forge main project (own sourceSet, own run task).
// ============================================================================
val runtimeSourceSet: SourceSet = sourceSets.create("runtime") {
    java.setSrcDirs(listOf("runtime/src/main/java"))
    resources.setSrcDirs(listOf("runtime/src/main/resources"))
    // Compile against the same MCP-named Minecraft + Mixin + LaunchWrapper the main
    // project sees. (Runtime classpath below is rebuilt from scratch, Forge excluded.)
    compileClasspath += sourceSets["main"].compileClasspath
}

val runtimeMixin: Configuration by configurations.creating
val runtimeRemapper: Configuration by configurations.creating
dependencies {
    runtimeMixin("org.spongepowered:mixin:0.7.11-SNAPSHOT") { isTransitive = false }
    runtimeRemapper("net.fabricmc:tiny-remapper:0.14.0")
    runtimeRemapper("net.fabricmc:mapping-io:0.2.1")
    runtimeRemapper("org.ow2.asm:asm:9.4")
    runtimeRemapper("org.ow2.asm:asm-commons:9.4")
    runtimeRemapper("org.ow2.asm:asm-tree:9.4")
}

val runtimeNamedJar = layout.buildDirectory.file("runtime/minecraft-1.8.9-named-noforge.jar")

/**
 * Remap the pure Mojang client jar (official/notch) → MCP named, WITHOUT applying Forge patches.
 * Loom's minecraft-*-mapped.jar still embeds Forge hooks in bytecode; this jar does not.
 */
tasks.register<JavaExec>("remapPocMinecraft") {
    group = "fpsmaster-runtime"
    description = "Remap vanilla 1.8.9 client.jar official→named (Forge-free) for the POC."
    dependsOn(runtimeRemapper)

    val loomBase = File(gradle.gradleUserHomeDir, "caches/essential-loom")
    val inputJar = File(loomBase, "1.8.9/minecraft-client.jar")
    val mappings = File(
        loomBase,
        "1.8.9/de.oceanlabs.mcp.mcp_stable.1_8_9.22-1.8.9-forge-1.8.9-11.15.1.2318-1.8.9/mappings.tiny"
    )
    val outputJar = runtimeNamedJar.get().asFile

    inputs.files(inputJar, mappings)
    outputs.file(outputJar)

    classpath = runtimeRemapper
    mainClass.set("net.fabricmc.tinyremapper.Main")
    args(inputJar.absolutePath, outputJar.absolutePath, mappings.absolutePath, "official", "named")

    doFirst {
        outputJar.parentFile.mkdirs()
        require(inputJar.isFile) {
            "Missing vanilla client jar at $inputJar — run a Loom sync / genIntelliJRuns once first."
        }
        require(mappings.isFile) { "Missing mappings.tiny at $mappings" }
        logger.lifecycle("[runtime] remapping ${inputJar.name} → ${outputJar.name} (official→named, no Forge)")
    }
}

tasks.register<JavaExec>("runPocClient") {
    group = "fpsmaster-runtime"
    description = "Launch the 1.8.9 client with NO Forge: LaunchWrapper + FpsMasterTweaker + Mixin."
    dependsOn("runtimeClasses", runtimeMixin, "remapPocMinecraft")

    mainClass.set("net.minecraft.launchwrapper.Launch")

    // The 1.8.9 client MUST run on JDK 8 — LaunchWrapper casts the system classloader to
    // URLClassLoader, which throws on JDK 9+. (Gradle itself still runs on JDK 17/21.)
    // Apple Silicon: LWJGL2 natives are x86_64 → prefer an x86_64 JDK 8 under Rosetta.
    val java8Home = (findProperty("poc.java8") as String?) ?: System.getenv("JAVA8_HOME")
    if (java8Home != null) {
        executable = File(java8Home, "bin/java").absolutePath
    } else {
        javaLauncher.set(javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(8))
        })
    }

    val loomBase = File(gradle.gradleUserHomeDir, "caches/essential-loom")
    val nativesDir = File(loomBase, "1.8.9/natives")
    val assetsDir = File(loomBase, "assets")
    val runDir = layout.buildDirectory.dir("poc-run").get().asFile
    val namedMc = runtimeNamedJar.get().asFile

    // Runtime classpath from loom's resolved list, MINUS Forge / intermediary / patched MC.
    // Keeps LaunchWrapper, ASM, LWJGL, guava, gson, log4j, netty, authlib, commons, …
    val remapCp = file(".gradle/loom-cache/remapClasspath.txt")
    val libJars = remapCp.readLines()
        .filter { it.isNotBlank() }
        .flatMap { it.split(File.pathSeparatorChar) }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .filter { p ->
            !p.contains("intermediary") &&
                !p.contains("net.minecraftforge") &&
                !p.contains("minecraft-project-@-mapped") &&
                !p.contains("minecraft-mapped.jar") &&
                !p.contains("minecraft-srg.jar") &&
                !p.contains("forge-")
        }
        .map { file(it) }

    classpath = files(runtimeSourceSet.output, namedMc, libJars, runtimeMixin)

    systemProperty("java.library.path", nativesDir.absolutePath)
    systemProperty("org.lwjgl.librarypath", nativesDir.absolutePath)
    systemProperty("mixin.debug", "true")

    args(
        "--tweakClass", "top.fpsmaster.runtime.FpsMasterTweaker",
        "--version", "1.8.9",
        "--accessToken", "0",
        "--username", "FPSMasterPOC",
        "--assetIndex", "1.8.9-1.8",
        "--assetsDir", assetsDir.absolutePath,
        "--gameDir", runDir.absolutePath
    )

    doFirst {
        runDir.mkdirs()
        require(namedMc.isFile) { "Forge-free named jar missing: $namedMc (run remapPocMinecraft)" }
        val forgeOnCp = classpath.files.filter {
            it.path.contains("minecraftforge") || it.name.contains("forge-")
        }
        require(forgeOnCp.isEmpty()) { "Forge leaked onto POC classpath: $forgeOnCp" }
        logger.lifecycle("[runtime] classpath jars: ${classpath.files.size} (Forge excluded)")
        logger.lifecycle("[runtime] named MC:      ${namedMc.absolutePath}")
        logger.lifecycle("[runtime] natives:       ${nativesDir.absolutePath}")
    }
}

/**
 * Full vanilla test: classpath uses the real Mojang notch {@code minecraft-client.jar}.
 * Runtime deobf (RuntimeDeobfTransformer) remaps official→named so MCP Mixins still apply.
 */
tasks.register<JavaExec>("runPocClientVanilla") {
    group = "fpsmaster-runtime"
    description = "Launch with REAL notch minecraft-client.jar + runtime deobf (no Forge, no pre-named jar)."
    dependsOn("runtimeClasses", runtimeMixin)

    mainClass.set("net.minecraft.launchwrapper.Launch")

    val java8Home = (findProperty("poc.java8") as String?) ?: System.getenv("JAVA8_HOME")
    if (java8Home != null) {
        executable = File(java8Home, "bin/java").absolutePath
    } else {
        javaLauncher.set(javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(8))
        })
    }

    val loomBase = File(gradle.gradleUserHomeDir, "caches/essential-loom")
    val nativesDir = File(loomBase, "1.8.9/natives")
    val assetsDir = File(loomBase, "assets")
    val runDir = layout.buildDirectory.dir("poc-run-vanilla").get().asFile
    val vanillaJar = File(loomBase, "1.8.9/minecraft-client.jar")
    val mappings = File(
        loomBase,
        "1.8.9/de.oceanlabs.mcp.mcp_stable.1_8_9.22-1.8.9-forge-1.8.9-11.15.1.2318-1.8.9/mappings.tiny"
    )

    val remapCp = file(".gradle/loom-cache/remapClasspath.txt")
    val libJars = remapCp.readLines()
        .filter { it.isNotBlank() }
        .flatMap { it.split(File.pathSeparatorChar) }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .filter { p ->
            !p.contains("intermediary") &&
                !p.contains("net.minecraftforge") &&
                !p.contains("minecraft-project-@-mapped") &&
                !p.contains("minecraft-mapped.jar") &&
                !p.contains("minecraft-srg.jar") &&
                !p.contains("minecraft-client.jar") &&
                !p.contains("forge-")
        }
        .map { file(it) }

    classpath = files(runtimeSourceSet.output, vanillaJar, libJars, runtimeMixin)

    systemProperty("java.library.path", nativesDir.absolutePath)
    systemProperty("org.lwjgl.librarypath", nativesDir.absolutePath)
    systemProperty("mixin.debug", "true")
    systemProperty("fpsmaster.runtime.vanilla", "true")
    systemProperty("fpsmaster.runtime.mappings", mappings.absolutePath)
    systemProperty("fpsmaster.runtime.vanillaJar", vanillaJar.absolutePath)

    args(
        "--tweakClass", "top.fpsmaster.runtime.FpsMasterTweaker",
        "--version", "1.8.9",
        "--accessToken", "0",
        "--username", "FPSMasterPOC",
        "--assetIndex", "1.8.9-1.8",
        "--assetsDir", assetsDir.absolutePath,
        "--gameDir", runDir.absolutePath
    )

    doFirst {
        runDir.mkdirs()
        require(vanillaJar.isFile) { "Missing real vanilla jar: $vanillaJar" }
        require(mappings.isFile) { "Missing mappings: $mappings" }
        // Prove we are not accidentally using a named/Forge-patched jar.
        val forgeOnCp = classpath.files.filter {
            it.path.contains("minecraftforge") || it.name.contains("forge-") ||
                it.name.contains("named-noforge") || it.name.contains("minecraft-mapped")
        }
        require(forgeOnCp.isEmpty()) { "Forbidden jar on vanilla POC classpath: $forgeOnCp" }
        val hasVanilla = classpath.files.any { it == vanillaJar || it.name == "minecraft-client.jar" }
        require(hasVanilla) { "Real minecraft-client.jar missing from classpath" }
        logger.lifecycle("[runtime-vanilla] classpath jars: ${classpath.files.size}")
        logger.lifecycle("[runtime-vanilla] REAL client jar: ${vanillaJar.absolutePath}")
        logger.lifecycle("[runtime-vanilla] mappings:        ${mappings.absolutePath}")
        logger.lifecycle("[runtime-vanilla] natives:         ${nativesDir.absolutePath}")
    }
}


// ============================================================================
// Full-functionality Forge-free run: the ENTIRE FPSMaster Edge client (main
// sourceSet + all mixins in mixins.fpsmaster.json) on a plain 1.8.9 client,
// no Forge/FML. Reuses the Stage-0 POC plumbing (LaunchWrapper + Mixin +
// Forge-free named jar). Kept fully separate from the Forge build; the normal
// `./gradlew build` and the IDE Minecraft Client run are untouched.
// ============================================================================
tasks.register<JavaExec>("runFullClient") {
    group = "fpsmaster-runtime"
    description = "Launch the FULL client (all main mixins) with NO Forge: LaunchWrapper + FpsMasterFullTweaker."
    dependsOn("classes", "runtimeClasses", runtimeMixin, "remapPocMinecraft")

    mainClass.set("net.minecraft.launchwrapper.Launch")

    // 1.8.9 client MUST run on JDK 8 (LaunchWrapper casts the system classloader to URLClassLoader).
    // Apple Silicon: LWJGL2 natives are x86_64 → prefer an x86_64 JDK 8 under Rosetta.
    val java8Home = (findProperty("poc.java8") as String?) ?: System.getenv("JAVA8_HOME")
    if (java8Home != null) {
        executable = File(java8Home, "bin/java").absolutePath
    } else {
        javaLauncher.set(javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(8))
        })
    }

    val loomBase = File(gradle.gradleUserHomeDir, "caches/essential-loom")
    val nativesDir = File(loomBase, "1.8.9/natives")
    val assetsDir = File(loomBase, "assets")
    val runDir = layout.buildDirectory.dir("poc-run-full").get().asFile
    val namedMc = runtimeNamedJar.get().asFile

    // Runtime classpath from loom's resolved list, MINUS Forge / intermediary / patched MC.
    val remapCp = file(".gradle/loom-cache/remapClasspath.txt")
    val libJars = remapCp.readLines()
        .filter { it.isNotBlank() }
        .flatMap { it.split(File.pathSeparatorChar) }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .filter { p ->
            !p.contains("intermediary") &&
                !p.contains("net.minecraftforge") &&
                !p.contains("minecraft-project-@-mapped") &&
                !p.contains("minecraft-mapped.jar") &&
                !p.contains("minecraft-srg.jar") &&
                !p.contains("minecraft-client.jar") &&
                !p.contains("forge-")
        }
        .map { file(it) }

    // main output (classes + resources incl. mixins.fpsmaster.json) + POC tweaker + named MC +
    // the shaded business deps (mixin, websocket, slf4j, kotlin, jlayer, zxing, jna, Cadence) + MC libs.
    classpath = files(
        sourceSets["main"].output,
        runtimeSourceSet.output,
        namedMc,
        configurations["shadowImpl"],
        libJars,
        runtimeMixin
    )

    systemProperty("java.library.path", nativesDir.absolutePath)
    systemProperty("org.lwjgl.librarypath", nativesDir.absolutePath)
    systemProperty("mixin.debug", "true")
    systemProperty("mixin.env.disableRefMap", "true")
    systemProperty("fpsmaster.noforge", "true")
    systemProperty("fpsmaster.full", "true")
    systemProperty("edge.uishot", System.getProperty("edge.uishot", ""))
    systemProperty("edge.uishot.screen", System.getProperty("edge.uishot.screen", ""))

    args(
        "--tweakClass", "top.fpsmaster.runtime.FpsMasterFullTweaker",
        "--version", "1.8.9",
        "--accessToken", "0",
        "--username", "FPSMasterFull",
        "--assetIndex", "1.8.9-1.8",
        "--assetsDir", assetsDir.absolutePath,
        "--gameDir", runDir.absolutePath
    )

    doFirst {
        runDir.mkdirs()
        require(namedMc.isFile) { "Forge-free named jar missing: $namedMc (run remapPocMinecraft)" }
        val forgeOnCp = classpath.files.filter {
            it.path.contains("minecraftforge") || it.name.contains("forge-") ||
                it.name.contains("minecraft-mapped")
        }
        require(forgeOnCp.isEmpty()) { "Forge leaked onto FULL classpath: $forgeOnCp" }
        logger.lifecycle("[runtime-full] classpath jars: ${classpath.files.size} (Forge excluded)")
        logger.lifecycle("[runtime-full] named MC:      ${namedMc.absolutePath}")
        logger.lifecycle("[runtime-full] natives:       ${nativesDir.absolutePath}")
    }
}

// ============================================================================
// vanilla + OptiFine (non-Forge): REAL notch minecraft-client.jar + OptiFineTweaker then
// FpsMasterFullTweaker. OF patches notch names; runtime deobf remaps OF-patched bytes to MCP
// names so mixins still apply. Named-jar runFullClient cannot host OF patches.
// ============================================================================
val optifinePreferredName = "OptiFine_1.8.9_HD_U_M5.jar"
val optifineDir = layout.buildDirectory.dir("poc/optifine")
val optifineJarProvider = optifineDir.map { it.file(optifinePreferredName) }

tasks.register("resolveOptifine") {
    group = "fpsmaster-runtime"
    description = "Resolve non-Forge OptiFine 1.8.9 jar (override with -Poptifine.jar=/path)."
    outputs.file(optifineJarProvider)
    doLast {
        val dest = optifineJarProvider.get().asFile
        dest.parentFile.mkdirs()

        val override = findProperty("optifine.jar") as String?
        if (override != null) {
            val src = file(override)
            require(src.isFile) { "optifine.jar override missing: $src" }
            src.copyTo(dest, overwrite = true)
            logger.lifecycle("[optifine] copied override → ${dest.absolutePath}")
            return@doLast
        }
        if (dest.isFile && dest.length() > 1_000_000L) {
            logger.lifecycle("[optifine] already present: ${dest.absolutePath}")
            return@doLast
        }
        val localCandidates = listOf(
            file("runtime/libs/$optifinePreferredName"),
            file("runtime/libs/OptiFine_1.8.9_HD_U_I7.jar"),
        )
        val local = localCandidates.firstOrNull { it.isFile }
        if (local != null) {
            local.copyTo(dest, overwrite = true)
            logger.lifecycle("[optifine] copied local ${local.name} → ${dest.absolutePath}")
            return@doLast
        }

        // BMCLAPI mirror (same CDN the launcher ecosystem uses for OptiFine downloads).
        val url = "https://bmclapi2.bangbang93.com/optifine/1.8.9/HD_U_M5"
        logger.lifecycle("[optifine] downloading $url …")
        URI(url).toURL().openStream().use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        require(dest.isFile && dest.length() > 1_000_000L) {
            "OptiFine download failed or too small: $dest. Pass -Poptifine.jar=/path/to/OptiFine_1.8.9_*.jar"
        }
        logger.lifecycle("[optifine] downloaded → ${dest.absolutePath} (${dest.length()} bytes)")
    }
}

tasks.register<JavaExec>("runFullClientOf") {
    group = "fpsmaster-runtime"
    description =
        "Launch FULL client + non-Forge OptiFine: notch jar + OptiFineTweaker + FpsMasterFullTweaker."
    dependsOn("classes", "runtimeClasses", runtimeMixin, "resolveOptifine")

    mainClass.set("net.minecraft.launchwrapper.Launch")

    val java8Home = (findProperty("poc.java8") as String?) ?: System.getenv("JAVA8_HOME")
    if (java8Home != null) {
        executable = File(java8Home, "bin/java").absolutePath
    } else {
        javaLauncher.set(javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(8))
        })
    }

    val loomBase = File(gradle.gradleUserHomeDir, "caches/essential-loom")
    val nativesDir = File(loomBase, "1.8.9/natives")
    val assetsDir = File(loomBase, "assets")
    val runDir = layout.buildDirectory.dir("poc-run-full-of").get().asFile
    val vanillaJar = File(loomBase, "1.8.9/minecraft-client.jar")
    val mappings = File(
        loomBase,
        "1.8.9/de.oceanlabs.mcp.mcp_stable.1_8_9.22-1.8.9-forge-1.8.9-11.15.1.2318-1.8.9/mappings.tiny"
    )
    val ofJar = optifineJarProvider.get().asFile

    val remapCp = file(".gradle/loom-cache/remapClasspath.txt")
    val libJars = remapCp.readLines()
        .filter { it.isNotBlank() }
        .flatMap { it.split(File.pathSeparatorChar) }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .filter { p ->
            !p.contains("intermediary") &&
                !p.contains("net.minecraftforge") &&
                !p.contains("minecraft-project-@-mapped") &&
                !p.contains("minecraft-mapped.jar") &&
                !p.contains("minecraft-srg.jar") &&
                !p.contains("minecraft-client.jar") &&
                !p.contains("named-noforge") &&
                !p.contains("forge-")
        }
        .map { file(it) }

    classpath = files(
        sourceSets["main"].output,
        runtimeSourceSet.output,
        vanillaJar,
        ofJar,
        configurations["shadowImpl"],
        libJars,
        runtimeMixin
    )

    systemProperty("java.library.path", nativesDir.absolutePath)
    systemProperty("org.lwjgl.librarypath", nativesDir.absolutePath)
    systemProperty("mixin.debug", "true")
    systemProperty("mixin.env.disableRefMap", "true")
    systemProperty("fpsmaster.noforge", "true")
    systemProperty("fpsmaster.full", "true")
    systemProperty("fpsmaster.withOptifine", "true")
    systemProperty("fpsmaster.runtime.vanilla", "true")
    systemProperty("fpsmaster.runtime.mappings", mappings.absolutePath)
    systemProperty("fpsmaster.runtime.vanillaJar", vanillaJar.absolutePath)

    // OF first (patches notch), then FullTweaker (deobf + Mixin). Explicit OptiFineTweaker —
    // jar Manifest defaults to OptiFineForgeTweaker which is wrong for no-Forge.
    args(
        "--tweakClass", "optifine.OptiFineTweaker",
        "--tweakClass", "top.fpsmaster.runtime.FpsMasterFullTweaker",
        "--version", "1.8.9",
        "--accessToken", "0",
        "--username", "FPSMasterFullOF",
        "--assetIndex", "1.8.9-1.8",
        "--assetsDir", assetsDir.absolutePath,
        "--gameDir", runDir.absolutePath
    )

    doFirst {
        runDir.mkdirs()
        require(vanillaJar.isFile) { "Missing real vanilla jar: $vanillaJar" }
        require(mappings.isFile) { "Missing mappings: $mappings" }
        require(ofJar.isFile) { "Missing OptiFine jar: $ofJar (run resolveOptifine)" }
        val forgeOnCp = classpath.files.filter {
            it.path.contains("minecraftforge") || it.name.contains("forge-") ||
                it.name.contains("named-noforge") || it.name.contains("minecraft-mapped")
        }
        require(forgeOnCp.isEmpty()) { "Forbidden jar on FULL+OF classpath: $forgeOnCp" }
        val hasVanilla = classpath.files.any { it == vanillaJar || it.name == "minecraft-client.jar" }
        require(hasVanilla) { "Real minecraft-client.jar missing from classpath" }
        val hasOf = classpath.files.any { it == ofJar || it.name.startsWith("OptiFine_") }
        require(hasOf) { "OptiFine jar missing from classpath" }
        logger.lifecycle("[runtime-full-of] classpath jars: ${classpath.files.size} (Forge excluded)")
        logger.lifecycle("[runtime-full-of] REAL client jar: ${vanillaJar.absolutePath}")
        logger.lifecycle("[runtime-full-of] OptiFine:        ${ofJar.absolutePath}")
        logger.lifecycle("[runtime-full-of] mappings:        ${mappings.absolutePath}")
        logger.lifecycle("[runtime-full-of] natives:         ${nativesDir.absolutePath}")
    }
}

// ============================================================================
// Production AOT distribution (vanilla / no-Forge):
// Ships ONLY FPSMaster runtime + official↔named mappings. NEVER ships Mojang Minecraft
// jars (notch or named) — that violates Mojang's EULA redistribution rules. The launcher
// (or the developer) supplies the official notch client jar at launch time; runtime
// deobf (RuntimeDeobfTransformer) always remaps FPSMaster named bytecode onto it.
// Parallel to the Forge Modrinth jar (remapJar) — does not replace it.
// ============================================================================
val aotDistName = "fpsmaster-edge-aot-$version"
val aotDistDir = layout.buildDirectory.dir("aot/$aotDistName")
val aotRuntimeJar = aotDistDir.map { it.file("fpsmaster-runtime.jar") }
val aotMappingsFile = aotDistDir.map { it.file("mappings.tiny") }
val aotLoomMappings = File(
    gradle.gradleUserHomeDir,
    "caches/essential-loom/1.8.9/de.oceanlabs.mcp.mcp_stable.1_8_9.22-1.8.9-forge-1.8.9-11.15.1.2318-1.8.9/mappings.tiny"
)
val aotMappingsId =
    "mcp_stable.22-1.8.9/official→named (essential-loom 1.8.9 mappings.tiny)"
// Filenames that must never appear in a redistributed AOT package (Mojang client jars /
// remapped derivatives). Local Gradle POC tasks may still use loom-cache jars in-place.
val aotForbiddenArtifactNames = setOf(
    "client-named.jar",
    "minecraft-client.jar",
    "minecraft-mapped.jar",
    "minecraft-srg.jar",
    "named-noforge.jar",
)

fun aotLibJars(): List<File> {
    val remapCp = file(".gradle/loom-cache/remapClasspath.txt")
    return remapCp.readLines()
        .filter { it.isNotBlank() }
        .flatMap { it.split(File.pathSeparatorChar) }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .filter { p ->
            !p.contains("intermediary") &&
                !p.contains("net.minecraftforge") &&
                !p.contains("minecraft-project-@-mapped") &&
                !p.contains("minecraft-mapped.jar") &&
                !p.contains("minecraft-srg.jar") &&
                !p.contains("minecraft-client.jar") &&
                !p.contains("named-noforge") &&
                !p.contains("forge-") &&
                !p.endsWith("client-named.jar")
        }
        .map { file(it) }
}

tasks.register<ShadowJar>("shadowAotRuntime") {
    group = "fpsmaster-aot"
    description =
        "Shadow main + poc tweaker + deps into named fpsmaster-runtime.jar (no Forge remap)."
    dependsOn("classes", "runtimeClasses")

    archiveFileName.set("fpsmaster-runtime.jar")
    destinationDirectory.set(aotDistDir)
    configurations = listOf(shadowImpl)
    from(sourceSets["main"].output)
    from(runtimeSourceSet.output)
    mergeServiceFiles()
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    // Shaded deps (e.g. Cadence/JNA) may carry META-INF signatures that break JarVerifier.
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.EC")

    // Clear Forge-mod attributes inherited from tasks.withType(Jar) — this jar is LaunchWrapper-only.
    manifest {
        attributes.clear()
        attributes(
            mapOf(
                "TweakClass" to "top.fpsmaster.runtime.FpsMasterFullTweaker",
                "MixinConfigs" to "mixins.fpsmaster.json",
                "Implementation-Title" to "FPSMaster Edge AOT Runtime",
                "Implementation-Version" to version,
            )
        )
    }

    // Keep MCP named — do not feed this jar through remapJar (SRG).
    doLast {
        val out = archiveFile.get().asFile
        require(out.isFile && out.length() > 100_000L) { "AOT runtime jar looks empty: $out" }
        logger.lifecycle("[aot] fpsmaster-runtime.jar → ${out.absolutePath} (${out.length()} bytes)")
    }
}

fun sha256Hex(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buf = ByteArray(8192)
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            digest.update(buf, 0, n)
        }
    }
    return digest.digest().joinToString("") { b -> "%02x".format(b) }
}

tasks.register("packageAotDistribution") {
    group = "fpsmaster-aot"
    description =
        "Assemble AOT zip (runtime + mappings only). Never packages Mojang client jars."
    dependsOn("shadowAotRuntime")

    val zipOut = layout.buildDirectory.file("libs/$aotDistName.zip")
    outputs.dir(aotDistDir)
    outputs.file(zipOut)

    doLast {
        val dir = aotDistDir.get().asFile
        dir.mkdirs()
        // Drop leftovers from older named-client AOT builds so they cannot re-enter the zip.
        aotForbiddenArtifactNames.forEach { name ->
            val stale = File(dir, name)
            if (stale.isFile) {
                check(stale.delete()) { "Failed to delete forbidden AOT artifact: $stale" }
                logger.lifecycle("[aot] removed forbidden leftover $name")
            }
        }

        val runtime = aotRuntimeJar.get().asFile
        val mappingsOut = aotMappingsFile.get().asFile
        require(runtime.isFile) { "Missing $runtime — run shadowAotRuntime" }
        require(aotLoomMappings.isFile) { "Missing mappings: $aotLoomMappings" }
        aotLoomMappings.copyTo(mappingsOut, overwrite = true)

        val runtimeSha = sha256Hex(runtime)
        val mappingsSha = sha256Hex(mappingsOut)

        val fullTweaker = "top.fpsmaster.runtime.FpsMasterFullTweaker"
        val ofTweaker = "optifine.OptiFineTweaker"

        val manifest = """
            {
              "version": "$version",
              "mc": "$mcVersion",
              "tweakClass": "$fullTweaker",
              "mixinConfigs": "mixins.fpsmaster.json",
              "disableRefMap": true,
              "mappingsId": "$aotMappingsId",
              "clientPolicy": "notch-only",
              "clientNote": "AOT never ships Minecraft jars. Launcher/user must provide the official notch client; runtime deobf always runs.",
              "optifine": {
                "mc": "1.8.9",
                "recommended": "HD_U_M5",
                "tweaker": "$ofTweaker",
                "bundled": false
              },
              "profiles": {
                "vanilla": {
                  "useForge": false,
                  "useOptiFine": false,
                  "client": "notch",
                  "runtimeDeobf": true,
                  "tweakClasses": ["$fullTweaker"]
                },
                "vanilla+of": {
                  "useForge": false,
                  "useOptiFine": true,
                  "client": "notch",
                  "runtimeDeobf": true,
                  "tweakClasses": ["$ofTweaker", "$fullTweaker"]
                },
                "forge": {
                  "useForge": true,
                  "useOptiFine": false,
                  "launch": "forge-mod",
                  "note": "Use FPSMaster-edge.jar in mods/; not this AOT LaunchWrapper path"
                },
                "forge+of": {
                  "useForge": true,
                  "useOptiFine": true,
                  "launch": "forge-mod",
                  "note": "Forge + OptiFine jar in mods/ + Edge Forge mod"
                }
              },
              "mappings": {
                "file": "mappings.tiny",
                "sha256": "$mappingsSha"
              },
              "runtime": {
                "file": "fpsmaster-runtime.jar",
                "sha256": "$runtimeSha"
              }
            }
        """.trimIndent() + "\n"
        File(dir, "manifest.json").writeText(manifest)

        val launchProfiles = """
            {
              "fullTweaker": "$fullTweaker",
              "optifineTweaker": "$ofTweaker",
              "runtimeJar": "fpsmaster-runtime.jar",
              "mappingsFile": "mappings.tiny",
              "client": "notch",
              "runtimeDeobf": true,
              "jvm": {
                "disableRefMap": true,
                "noforge": true,
                "runtimeVanilla": true
              },
              "profiles": {
                "vanilla": {
                  "client": "notch",
                  "runtimeDeobf": true,
                  "tweakClasses": ["$fullTweaker"]
                },
                "vanilla+of": {
                  "client": "notch",
                  "runtimeDeobf": true,
                  "requiresOptiFineJar": true,
                  "tweakClasses": ["$ofTweaker", "$fullTweaker"]
                }
              }
            }
        """.trimIndent() + "\n"
        File(dir, "launch-profiles.json").writeText(launchProfiles)

        File(dir, "SHA256SUMS").writeText(
            "$runtimeSha  fpsmaster-runtime.jar\n$mappingsSha  mappings.tiny\n"
        )

        val forbiddenPresent = aotForbiddenArtifactNames.filter { File(dir, it).exists() }
        require(forbiddenPresent.isEmpty()) {
            "AOT package must not contain Mojang client artifacts: $forbiddenPresent"
        }

        val zipFile = zipOut.get().asFile
        zipFile.parentFile.mkdirs()
        if (zipFile.exists()) zipFile.delete()
        ant.withGroovyBuilder {
            "zip"("destfile" to zipFile.absolutePath, "basedir" to dir.absolutePath)
        }
        logger.lifecycle("[aot] packaged ${dir.absolutePath} (runtime + mappings only; no MC jar)")
        logger.lifecycle("[aot] zip       ${zipFile.absolutePath} (${zipFile.length()} bytes)")
    }
}

tasks.register<JavaExec>("runAotClient") {
    group = "fpsmaster-aot"
    description =
        "Launch FULL AOT client against a local notch minecraft-client.jar + runtime deobf (never ships MC)."
    dependsOn("packageAotDistribution")

    mainClass.set("net.minecraft.launchwrapper.Launch")

    val java8Home = (findProperty("poc.java8") as String?) ?: System.getenv("JAVA8_HOME")
    if (java8Home != null) {
        executable = File(java8Home, "bin/java").absolutePath
    } else {
        javaLauncher.set(javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(8))
        })
    }

    val loomBase = File(gradle.gradleUserHomeDir, "caches/essential-loom")
    val nativesDir = File(loomBase, "1.8.9/natives")
    val assetsDir = File(loomBase, "assets")
    val runDir = layout.buildDirectory.dir("poc-run-aot").get().asFile
    val defaultVanilla = File(loomBase, "1.8.9/minecraft-client.jar")
    val vanillaOverride = findProperty("minecraft.client") as String?
    val vanillaJar = if (vanillaOverride != null) file(vanillaOverride) else defaultVanilla
    val runtimeJar = aotRuntimeJar.get().asFile
    val mappings = aotMappingsFile.get().asFile

    classpath = files(runtimeJar, vanillaJar, aotLibJars())

    systemProperty("java.library.path", nativesDir.absolutePath)
    systemProperty("org.lwjgl.librarypath", nativesDir.absolutePath)
    systemProperty("mixin.debug", "true")
    systemProperty("mixin.env.disableRefMap", "true")
    systemProperty("fpsmaster.noforge", "true")
    systemProperty("fpsmaster.full", "true")
    systemProperty("fpsmaster.aot", "true")
    systemProperty("fpsmaster.runtime.vanilla", "true")
    systemProperty("fpsmaster.runtime.mappings", mappings.absolutePath)
    systemProperty("fpsmaster.runtime.vanillaJar", vanillaJar.absolutePath)

    args(
        "--tweakClass", "top.fpsmaster.runtime.FpsMasterFullTweaker",
        "--version", "1.8.9",
        "--accessToken", "0",
        "--username", "FPSMasterAOT",
        "--assetIndex", "1.8.9-1.8",
        "--assetsDir", assetsDir.absolutePath,
        "--gameDir", runDir.absolutePath
    )

    doFirst {
        runDir.mkdirs()
        require(vanillaJar.isFile) {
            "Missing notch client jar: $vanillaJar (override with -Pminecraft.client=/path/to/minecraft-client.jar). AOT never ships Mojang jars."
        }
        require(runtimeJar.isFile) { "AOT fpsmaster-runtime.jar missing: $runtimeJar" }
        require(mappings.isFile) { "AOT mappings.tiny missing: $mappings (run packageAotDistribution)" }
        val forbiddenOnCp = classpath.files.filter {
            it.path.contains("minecraftforge") || it.name.contains("forge-") ||
                it.name.contains("named-noforge") || it.name.contains("minecraft-mapped") ||
                it.name == "client-named.jar"
        }
        require(forbiddenOnCp.isEmpty()) { "Forbidden jar on AOT classpath: $forbiddenOnCp" }
        val hasVanilla = classpath.files.any { it == vanillaJar || it.name == "minecraft-client.jar" }
        require(hasVanilla) { "Notch minecraft-client.jar missing from classpath" }
        logger.lifecycle("[aot] classpath jars: ${classpath.files.size}")
        logger.lifecycle("[aot] REAL notch jar: ${vanillaJar.absolutePath}")
        logger.lifecycle("[aot] runtime:        ${runtimeJar.absolutePath}")
        logger.lifecycle("[aot] mappings:       ${mappings.absolutePath}")
        logger.lifecycle("[aot] natives:        ${nativesDir.absolutePath}")
    }
}

// Alias kept for existing docs/scripts; identical to runAotClient (notch-only).
tasks.register("runAotClientNotch") {
    group = "fpsmaster-aot"
    description = "Alias of runAotClient (notch client + runtime deobf)."
    dependsOn("runAotClient")
}

