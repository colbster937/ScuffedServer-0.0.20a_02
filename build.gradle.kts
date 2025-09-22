plugins { 
    application
    java
    id("com.gradleup.shadow") version "9.1.0"
}

group = "com.mojang.minecraft"
version = "1.0.0"
val main = "dev.colbster937.scuffed.server.ScuffedMinecraftServer"

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
    mainClass.set(main)
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveFileName.set("server.jar")
    manifest {
        attributes["Main-Class"] = main
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}