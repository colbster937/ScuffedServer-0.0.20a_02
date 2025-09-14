plugins { 
    application
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.mojang.minecraft"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

sourceSets {
    named("main") {
        java.setSrcDirs(listOf("src/game", "src/main"))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-nop:2.0.13")
    implementation("net.dv8tion:JDA:5.0.0-beta.24")
}

application {
    mainClass.set("dev.colbster937.scuffed.server.ScuffedMinecraftServer")
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveFileName.set("server.jar")
    manifest {
        attributes["Main-Class"] = "dev.colbster937.scuffed.server.ScuffedMinecraftServer"
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}