package com.github.alexeylapin.proxy.config;

import io.micronaut.core.annotation.TypeHint;

@TypeHint(
        typeNames = {
                "ch.qos.logback.classic.jul.LevelChangePropagator",
        },
        accessType = {
                TypeHint.AccessType.ALL_DECLARED_CONSTRUCTORS,
                TypeHint.AccessType.ALL_DECLARED_FIELDS,
                TypeHint.AccessType.ALL_PUBLIC_METHODS
        }
)
public class GraalConfig {
}
