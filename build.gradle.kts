import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.util.VersionNumber

plugins {
    java
}

repositories {
    maven {
        url = uri("https://repo.runelite.net")
    }
    mavenCentral()
}

val runeliteLocks by configurations.creating
val runeLiteVersion = "1.6.21.1"

dependencies {
    runeliteLocks(group = "net.runelite", name = "client", version = runeLiteVersion)
    compileOnly(group = "net.runelite", name = "client", version = runeLiteVersion)

    compileOnly("org.projectlombok:lombok:1.18.4")
    annotationProcessor("org.projectlombok:lombok:1.18.4")

    // Do not increase this until Runelite proper depends on a
    // modern version of okhttp
    implementation("org.influxdb:influxdb-java:2.5")

    testImplementation("junit:junit:4.12")
    testImplementation("org.slf4j:slf4j-simple:1.7.12")
    testImplementation(group = "net.runelite", name = "client", version = runeLiteVersion) {
        exclude(group = "ch.qos.logback", module = "logback-classic")
    }
}

dependencyLocking {
    lockAllConfigurations()
}

group = "net.machpi.runelite"
version = "0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

val verifyRuneliteLocks by tasks.registering {
    doLast {
        fun extractVersions(cfg: Configuration): Map<ModuleIdentifier, VersionNumber> {
            return cfg.resolvedConfiguration.resolvedArtifacts.associateBy(
                    { it.moduleVersion.id.module },
                    { VersionNumber.parse(it.moduleVersion.id.version) })
        }

        val runtimeDeps = extractVersions(configurations.testRuntimeClasspath.get())
        val lockDeps = extractVersions(runeliteLocks)
        lockDeps.forEach { id, lockedVersion ->
            val actualVersion = runtimeDeps.get(id)
            if (actualVersion != null && lockedVersion < actualVersion)
                throw RuntimeException("Dependency " + id + " resolved to " + actualVersion + " in runtime, but runelite uses " + lockedVersion)
        }
    }
}

tasks.named("test").configure {
    dependsOn(verifyRuneliteLocks)
}
