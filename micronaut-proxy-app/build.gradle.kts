import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask
import org.gradle.internal.os.OperatingSystem
import pl.allegro.tech.build.axion.release.domain.VersionConfig

plugins {
    alias(libs.plugins.micronaut.application)
    alias(libs.plugins.shadow)
}

version = rootProject.extensions.getByType(VersionConfig::class).version

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
            imageName.set(rootProject.name)
            buildArgs.add("--verbose")
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

tasks {

    named<ShadowJar>("shadowJar") {
        archiveBaseName = rootProject.name
        archiveClassifier = "dist"
    }

    named<DockerBuildImage>("dockerBuildNative") {
        val registry = System.getenv("CR_REGISTRY")
        val namespace = System.getenv("CR_NAMESPACE")
        images.set(
            listOf(
                "${registry}/${namespace}/${rootProject.name}:latest",
                "${registry}/${namespace}/${rootProject.name}:${project.version}"
            )
        )
    }

    val writeArtifactFile by registering {
        doLast {
            val outputDirectory = getByName<BuildNativeImageTask>("nativeCompile").outputDirectory
            outputDirectory.get().asFile.mkdirs()
            outputDirectory.file("gradle-artifact.txt")
                .get().asFile
                .writeText("${rootProject.name}-${project.version}-${platform()}")
        }
    }

    named("nativeCompile") {
        finalizedBy(writeArtifactFile)
    }

}

fun platform(): String {
    val os = OperatingSystem.current()
    val arc = System.getProperty("os.arch")
    return when {
        OperatingSystem.current().isWindows -> "windows-${arc}"
        OperatingSystem.current().isLinux -> "linux-${arc}"
        OperatingSystem.current().isMacOsX -> "darwin-${arc}"
        else -> os.nativePrefix
    }
}
