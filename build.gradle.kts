import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    java
    `java-library`

    alias(libs.plugins.shadow)
    alias(libs.plugins.pluginyml)
    alias(libs.plugins.spotless)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    compileJava.configure {
        options.release.set(21)
    }

    withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
}

configurations.all {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
}

version = "6.0.2-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://repo.dmulloy2.net/nexus/repository/public/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
    maven { url = uri("https://maven.enginehub.org/repo/") }
}

dependencies {
    implementation(platform("com.intellectualsites.bom:bom-newest:1.47"))
    compileOnly("io.papermc.paper:paper-api")
    compileOnly("com.intellectualsites.plotsquared:plotsquared-core")
    compileOnly("com.intellectualsites.plotsquared:plotsquared-bukkit") { isTransitive = false }
    implementation(libs.packetevents)
    compileOnly(libs.worldedit)
    implementation("org.bstats:bstats-bukkit")
    implementation("org.bstats:bstats-base")
}

spotless {
    java {
        licenseHeaderFile(rootProject.file("HEADER.txt"))
        target("**/*.java")
        endWithNewline()
        trimTrailingWhitespace()
        removeUnusedImports()
    }
}

bukkit {
    name = "PlotHider"
    main = "com.plotsquared.plothider.PlotHiderPlugin"
    authors = listOf("Empire92", "dordsor21")
    apiVersion = "1.13"
    description = "Hide plots from other players"
    version = rootProject.version.toString()
    depend = listOf("PlotSquared")
    website = "https://www.spigotmc.org/resources/20701/"

    permissions {
        register("plots.plothider.bypass") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set(null as String?)

    relocate("org.bstats", "com.plotsquared.plothider.metrics")
    relocate("com.github.retrooper.packetevents", "com.plotsquared.plothider.packetevents")
    relocate("io.github.retrooper.packetevents", "com.plotsquared.plothider.packetevents2")

    dependencies {
        include(dependency("org.bstats:bstats-base"))
        include(dependency("org.bstats:bstats-bukkit"))
        include(dependency("net.kyori:adventure-nbt"))
        include(dependency("com.github.retrooper:packetevents-api"))
        include(dependency("com.github.retrooper:packetevents-netty-common"))
        include(dependency("com.github.retrooper:packetevents-spigot"))
    }

    minimize()
}

tasks.named("build").configure {
    dependsOn("shadowJar")
}
