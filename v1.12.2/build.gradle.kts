plugins {
    idea
    java
    id("gg.essential.loom") version "0.10.0.+"
    id("dev.architectury.architectury-pack200") version "0.1.3"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.gorylenko.gradle-git-properties") version "2.2.1"
    kotlin("jvm") version "2.0.0-Beta4"
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
    forge {
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
    java.srcDir("../shared/java")
    resources.srcDir("../shared/resources")
//    output.resourcesDir = file("$buildDir/classes/java/main")
}

// Dependencies:

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.spongepowered.org/maven/")
    // If you don't want to log in with your real minecraft account, remove this line
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
}

val shadowImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

dependencies {
    minecraft("com.mojang:minecraft:1.12.2")
    mappings("de.oceanlabs.mcp:mcp_stable:39-1.12")
    forge("net.minecraftforge:forge:1.12.2-14.23.5.2847") // For some reason it cant find a version newer than 2847

    // If you don't want mixins, remove these lines
    shadowImpl("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }
    shadowImpl("javazoom:jlayer:1.0.1"){
        isTransitive = false
    }
    shadowImpl("org.jetbrains.kotlin:kotlin-stdlib-jdk8"){
        isTransitive = true
    }
    shadowImpl("org.java-websocket:Java-WebSocket:1.5.4"){
        isTransitive = false
    }
    shadowImpl("org.slf4j:slf4j-api:2.0.6"){
        isTransitive = false
    }
    shadowImpl("org.json:json:20230227"){
        isTransitive = false
    }
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT")
    implementation("org.yaml:snakeyaml:2.0")

    // If you don't want to log in with your real minecraft account, remove this line
//    runtimeOnly("me.djtheredstoner:DevAuth-forge-legacy:1.1.2")
    implementation("javazoom:jlayer:1.0.1")
// https://mvnrepository.com/artifact/net.sourceforge.jtransforms/jtransforms
    implementation("net.sourceforge.jtransforms:jtransforms:2.4.0")

    implementation(kotlin("stdlib-jdk8"))
}

gitProperties {
    gitPropertiesResourceDir = project.file("src/main/resources")
    gitPropertiesDir = project.file("src/main/resources")
    gitPropertiesName = "git.properties"
    keys = arrayOf("git.branch", "git.commit.id", "git.commit.time", "git.commit.id.abbrev").toMutableList()
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
        this["MixinConfigs"] = "mixins.$modid.json"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.processResources {
    inputs.property("version", project.version)
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
    doLast {
        configurations.forEach {
            println("Copying jars into mod: ${it.files}")
        }
    }

    // If you want to include other dependencies and shadow them, you can relocate them in here
    fun relocate(name: String) = relocate(name, "$baseGroup.deps.$name")
}

tasks.register<Copy>("copyDependencies") {
    from(configurations.runtimeClasspath.get())
    into("libs")
}

tasks.assemble.get().dependsOn(tasks.remapJar)

