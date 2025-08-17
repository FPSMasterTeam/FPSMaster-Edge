import org.apache.commons.lang3.SystemUtils

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

val accessTransformerName = "patcher_at.cfg"
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
    java.srcDir("../shared/java")
    resources.srcDir("../shared/resources")
    output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory })
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
    shadowImpl("party.iroiro.luajava:luajava:4.0.2")
    shadowImpl("party.iroiro.luajava:lua53-platform:4.0.2:natives-desktop")
    shadowImpl("party.iroiro.luajava:lua53:4.0.2")
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT")
    shadowImpl("javazoom:jlayer:1.0.1") {
        isTransitive = false
    }
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
    // If you don't want to log in with your real minecraft account, remove this line
    runtimeOnly("me.djtheredstoner:DevAuth-forge-legacy:1.1.2")
    implementation("javazoom:jlayer:1.0.1")
// https://mvnrepository.com/artifact/net.sourceforge.jtransforms/jtransforms
    implementation("net.sourceforge.jtransforms:jtransforms:2.4.0")
    shadowImpl("net.sourceforge.jtransforms:jtransforms:2.4.0") {
        isTransitive = true
    }
    implementation("com.github.FPSMasterTeam:JLuaParser:master-SNAPSHOT")
    shadowImpl("com.github.FPSMasterTeam:JLuaParser:master-SNAPSHOT") {
        isTransitive = true
    }
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
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
        this["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
        this["FMLAT"] = "patcher_at.cfg"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.processResources {
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

