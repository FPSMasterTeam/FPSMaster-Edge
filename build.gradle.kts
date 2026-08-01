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

tasks.register<Copy>("copyDependencies") {
    from(configurations.runtimeClasspath.get())
    into("libs")
}


tasks.assemble.get().dependsOn(tasks.remapJar)

