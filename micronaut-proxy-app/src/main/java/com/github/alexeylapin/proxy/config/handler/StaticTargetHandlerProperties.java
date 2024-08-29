package com.github.alexeylapin.proxy.config.handler;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.serde.annotation.Serdeable;

import java.util.Map;

@ConfigurationProperties("micronaut-proxy.static")
public record StaticTargetHandlerProperties(
        String path,
        Map<String, TargetProperties> targets
) {

    @Serdeable
    public record TargetProperties(
            @Parameter String name,
            String scheme,
            String host,
            int port,
            String to
    ) {
    }

}
