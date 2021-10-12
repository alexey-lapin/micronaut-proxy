plugins {
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("io.micronaut.application") version "2.0.6"
}

version = "0.1"

repositories {
    mavenCentral()
}

micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("com.github.alexeylapin.proxy.*")
    }
}

dependencies {
    compileOnly("io.micronaut.reactor:micronaut-reactor")

    implementation("io.micronaut:micronaut-http-client")
    implementation("io.micronaut:micronaut-runtime")

    runtimeOnly("ch.qos.logback:logback-classic")
}

application {
    mainClass.set("com.github.alexeylapin.proxy.Application")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
