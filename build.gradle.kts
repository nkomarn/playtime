import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.22"
    id("io.papermc.paperweight.userdev") version "1.5.5"
    id("com.github.johnrengelman.shadow") version "7.1.1"
}

group = "gay.kyta"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io")
}

dependencies {
    paperweight.paperDevBundle("1.20.1-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("org.jetbrains.exposed", "exposed-core", "0.40.1")
    implementation("org.jetbrains.exposed", "exposed-jdbc", "0.40.1")
    implementation("org.jetbrains.exposed", "exposed-java-time", "0.40.1")
    implementation("com.github.Revxrsal.Lamp", "common", "3.1.5")
    implementation("com.github.Revxrsal.Lamp", "bukkit", "3.1.5")
    implementation("joda-time", "joda-time", "2.12.5")

    /* plugin dependencies */
    compileOnly("me.clip:placeholderapi:2.11.3")
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    languageVersion = "1.9"
}