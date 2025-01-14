dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

plugins {
    id("io.micronaut.platform.catalog") version "4.4.5"
}

rootProject.name="micronaut-proxy"
include("micronaut-proxy-app")