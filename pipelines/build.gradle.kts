/*
 * This file was generated by the Gradle 'init' task.
 */
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("ai.triton.platform.kotlin-application-conventions")
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

dependencies {
    implementation("org.apache.commons:commons-text")
    implementation(project(":do-fns"))
    implementation(project(":utilities"))
    implementation("com.amazonaws:aws-java-sdk-lambda")
    implementation("org.apache.beam:beam-runners-direct-java")
    implementation("org.apache.beam:beam-runners-spark-3")
    implementation("org.apache.beam:beam-sdks-java-core")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.withType<ShadowJar> {
    archiveFileName.set("pipelines.shadow.jar")
}

application {
    // Define the main class for the application.
    mainClass.set("ai.triton.platform.pipelines.AppKt")
}
