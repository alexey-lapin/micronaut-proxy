package com.github.alexeylapin.proxy;

import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("micronaut-proxy")
public record MicronautProxyProperties(String targetedPath) {
}
