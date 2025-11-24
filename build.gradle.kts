plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("kapt") version "2.2.21"
    id("com.gradleup.shadow") version "8.3.0"
}

group = "org.elysian"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}

dependencies {
    // Velocity API
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    kapt("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // Redis (for cross-server communication)
    implementation("redis.clients:jedis:5.1.0")

    // JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // Configuration
    implementation("com.moandjiezana.toml:toml4j:0.7.2")

    // Cache
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // FigLet (optional, for nice console logos)
    implementation("com.github.lalyos:jfiglet:0.0.9")
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks {
    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        duplicatesStrategy = DuplicatesStrategy.WARN

        filesMatching("velocity-plugin.json") {
            expand(props)
        }
    }

    shadowJar {
        archiveClassifier.set("")

        relocate("redis.clients.jedis", "org.elysian.lib.jedis")
        relocate("com.google.gson", "org.elysian.lib.gson")
        relocate("kotlinx.coroutines", "org.elysian.lib.coroutines")
        relocate("com.github.benmanes.caffeine", "org.elysian.lib.caffeine")
        relocate("com.moandjiezana.toml", "org.elysian.lib.toml")
        relocate("com.github.lalyos.jfiglet", "org.elysian.lib.jfiglet")

        from(sourceSets.main.get().resources)
    }

    register<Copy>("dev") {
        group = "development"
        dependsOn("shadowJar")
        from(shadowJar.get().archiveFile)
        into(file("run/plugins"))
        doLast {
            println("\n✅ Plugin copied to run/plugins/")
            println("💡 Restart Velocity proxy to load the plugin\n")
        }
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