package com.github.alexeylapin.proxy.config;

import com.github.alexeylapin.proxy.config.handler.Base64HandlerProperties;
import com.github.alexeylapin.proxy.config.handler.StaticTargetHandlerProperties;
import com.github.alexeylapin.proxy.service.handler.impl.Base64Handler;
import com.github.alexeylapin.proxy.service.handler.impl.StaticTargetHandler;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.http.client.ProxyHttpClient;

@Factory
public class AppConfig {

    @Bean
    public StaticTargetHandler staticTargetHandler(StaticTargetHandlerProperties properties, ProxyHttpClient client) {
        return new StaticTargetHandler(properties, client);
    }

    @Bean
    public Base64Handler base64Handler(Base64HandlerProperties properties, ProxyHttpClient client) {
        return new Base64Handler(properties, client);
    }

}
