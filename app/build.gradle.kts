/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("ai.triton.platform.kotlin-application-conventions")
}

dependencies {
    implementation("org.apache.commons:commons-text")
    implementation(project(":do-fns"))
    implementation(platform("org.apache.beam:beam-sdks-java-bom:2.39.0"))
    implementation("org.apache.beam:beam-runners-direct-java")
    implementation("org.apache.beam:beam-runners-spark")
    implementation("org.apache.beam:beam-sdks-java-core")
}

application {
    // Define the main class for the application.
    mainClass.set("ai.triton.platform.app.AppKt")
}
