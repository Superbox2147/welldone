plugins {
    kotlin("jvm") version "2.3.20"
    kotlin("plugin.serialization") version "2.3.20"
}

group = "org.whatever"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    implementation("dev.kord:kord-core:0.18.1")
    implementation("com.sksamuel.scrimage:scrimage-core:4.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

//    val jnaVersion = "5.19.1"
//
//    implementation("net.java.dev.jna:jna:$jnaVersion")
//    implementation("net.java.dev.jna:jna-platform:$jnaVersion")

    implementation("com.madgag:animated-gif-lib:1.4")
    implementation("dev.matrixlab.webp4j:webp4j-core:2.5.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
