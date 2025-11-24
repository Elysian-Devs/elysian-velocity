plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("kapt") version "2.0.21"
    id("com.gradleup.shadow") version "8.3.5"
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
    implementation("com.google.code.gson:gson:2.11.0")

    // YAML Configuration
    implementation("org.yaml:snakeyaml:2.3")
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
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        filesMatching("velocity-plugin.json") {
            expand(props)
        }
    }

    jar {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    shadowJar {
        archiveClassifier.set("")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        // Relocate dependencies to avoid conflicts
        relocate("redis.clients.jedis", "org.elysian.velocity.lib.jedis")
        relocate("com.google.gson", "org.elysian.velocity.lib.gson")
        relocate("kotlinx.coroutines", "org.elysian.velocity.lib.coroutines")
        relocate("org.yaml.snakeyaml", "org.elysian.velocity.lib.snakeyaml")

        // Exclude unnecessary files
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")

        from(sourceSets.main.get().resources)
    }

    register<Copy>("dev") {
        group = "development"
        description = "Build and copy plugin to dev server"
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