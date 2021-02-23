import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.30"
    kotlin("plugin.serialization") version "1.4.30"
}

group = "com.github.singleton11"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val ktorVersion = "1.5.1"
val logbackVersion = "1.2.3"
val slf4jVersion = "1.7.30"
val kotlinLoggingVersion = "1.12.0"
val jacksonVersion = "2.12.1"
val junitVersion = "5.6.0"
val kotlinVersion = "1.4.30"
val kotlinxCoroutinesVersion = "1.4.2"
val kotlinRetryVersion = "1.0.8"
val mavenVersion = "3.6.3"

dependencies {
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
    implementation("com.michael-bull.kotlin-retry:kotlin-retry:$kotlinRetryVersion")
    implementation("org.apache.maven:maven-model:$mavenVersion")
    implementation("org.apache.maven:maven-resolver-provider:3.6.3")
    implementation("org.apache.maven:maven-model-builder:3.6.3")
    implementation("org.apache.maven:maven:3.6.3")
    implementation("com.github.zafarkhaja:java-semver:0.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")


    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}