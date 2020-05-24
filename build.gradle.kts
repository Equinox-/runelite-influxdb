plugins {
    java
}

repositories {
    maven {
        url = uri("https://repo.runelite.net")
    }
    mavenCentral()
}

val runeLiteVersion = "1.6.15"

dependencies {
    compileOnly(group = "net.runelite", name = "client", version = runeLiteVersion)

    compileOnly("org.projectlombok:lombok:1.18.4")
    annotationProcessor("org.projectlombok:lombok:1.18.4")

    implementation("org.influxdb:influxdb-java:2.15")

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
