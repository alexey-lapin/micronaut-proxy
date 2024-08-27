package com.github.alexeylapin.proxy.config;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;

@EachProperty("micronaut-proxy.targets")
public record TargetProperties(
        @Parameter String name,
        String scheme,
        String host,
        int port,
        String to
) {
}
