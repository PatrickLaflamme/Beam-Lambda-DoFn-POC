/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    // Support convention plugins written in Kotlin. Convention plugins are build scripts in 'src/main' that automatically become available as plugins in the main build.
    `kotlin-dsl`

    kotlin("jvm") version "1.7.0"
    kotlin("plugin.serialization") version "1.7.0"
    //id("com.google.protobuf") version "0.8.8"
}

repositories {
    // Use the plugin portal to apply community plugins in convention plugins.
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    implementation("org.jetbrains.kotlin:kotlin-serialization")
}
