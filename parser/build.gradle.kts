plugins {
    kotlin("jvm") version "2.0.0"
    application
}

group = "ut.isep"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.11.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.3")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("ut.isep.MainKt")
}

tasks.named<JavaExec>("run") {
    workingDir = file("../")
}
