plugins {
    kotlin("jvm") version "2.0.0"
}

group = "funn.j2k"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://m2.dv8tion.net/releases")
}

dependencies {
    val kordVersion = "0.14.0"
    implementation("dev.kord:kord-core:$kordVersion")
    implementation("dev.kord:kord-voice:$kordVersion")

    implementation("com.sedmelluq:lavaplayer:1.3.77")

    implementation("org.slf4j:slf4j-simple:2.1.0-alpha1")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}