package com.github.alexeylapin.proxy.config;

import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("micronaut-proxy")
public record MicronautProxyProperties(
        String targetedPath
) {
}
