dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

plugins {
    id("io.micronaut.platform.catalog") version "4.6.1"
}

rootProject.name="micronaut-proxy"
include("micronaut-proxy-app")