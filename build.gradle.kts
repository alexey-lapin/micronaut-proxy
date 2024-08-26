import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage

plugins {
    alias(libs.plugins.shadow)
    alias(libs.plugins.micronaut.application)
}

application {
    mainClass.set("com.github.alexeylapin.proxy.Application")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("com.github.alexeylapin.proxy.*")
    }
}

graalvmNative {
    toolchainDetection.set(true)
    binaries {
        named("main") {
            buildArgs.add("-H:-UseContainerSupport")
        }
    }
}

dependencies {
    annotationProcessor(mn.micronaut.serde.processor)

    implementation(mn.micronaut.http.client)
    implementation(mn.micronaut.reactor)
    implementation(mn.micronaut.serde.jackson)

    runtimeOnly(mn.logback.classic)
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
