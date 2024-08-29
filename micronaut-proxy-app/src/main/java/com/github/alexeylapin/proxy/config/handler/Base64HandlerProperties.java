package com.github.alexeylapin.proxy.config.handler;

import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("micronaut-proxy.base64")
public record Base64HandlerProperties(
        String path) {
}
