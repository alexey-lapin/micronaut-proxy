import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("io.micronaut.application") version "3.7.5"
}

repositories {
    mavenCentral()
}

graalvmNative {
    toolchainDetection.set(true)
    binaries {
        named("main") {
            buildArgs.add("-H:-UseContainerSupport")
        }
    }
}

micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("com.github.alexeylapin.proxy.*")
    }
}

tasks.named<DockerBuildImage>("dockerBuildNative") {
    val registry = System.getenv("CR_REGISTRY")
    val namespace = System.getenv("CR_NAMESPACE")
    images.set(
        listOf(
            "${registry}/${namespace}/${project.name}:latest",
            "${registry}/${namespace}/${project.name}:${project.version}"
        )
    )
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
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
