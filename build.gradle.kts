plugins {
    kotlin("jvm") version "2.3.0"
    id("com.gradleup.shadow") version "8.3.0"
    id("com.google.devtools.ksp") version "2.3.0-1.0.28"
}

group = "org.elysian"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
}

dependencies {
    // Velocity API
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    ksp("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // Redis (optional for cross-server communication)
    implementation("redis.clients:jedis:5.1.0")

    // JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")

    // Configuration
    implementation("org.yaml:snakeyaml:2.2")
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks {
    shadowJar {
        archiveClassifier.set("")

        // Relocate dependencies to avoid conflicts
        relocate("redis.clients.jedis", "org.elysian.lib.jedis")
        relocate("com.google.gson", "org.elysian.lib.gson")
        relocate("kotlinx.coroutines", "org.elysian.lib.coroutines")
        relocate("org.yaml.snakeyaml", "org.elysian.lib.snakeyaml")

        // Include resources
        from(sourceSets.main.get().resources)
    }

    build {
        dependsOn("shadowJar")
    }
}

sourceSets {
    main {
        resources {
            srcDir("src/main/resources")
        }
    }
}