package com.github.alexeylapin.proxy.service.handler.impl;

import com.github.alexeylapin.proxy.config.handler.StaticTargetHandlerProperties;
import com.github.alexeylapin.proxy.service.handler.Handler;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.util.AntPathMatcher;
import io.micronaut.core.util.PathMatcher;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.client.ProxyHttpClient;
import io.micronaut.http.hateoas.JsonError;
import io.micronaut.http.hateoas.Link;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Arrays;

public class StaticTargetHandler implements Handler {

    private final StaticTargetHandlerProperties properties;
    private final ProxyHttpClient client;
    private final AntPathMatcher pathMatcher;

    public StaticTargetHandler(StaticTargetHandlerProperties properties, ProxyHttpClient client) {
        this.client = client;
        this.properties = properties;
        this.pathMatcher = PathMatcher.ANT;
    }

    @Override
    public boolean matches(HttpRequest<?> request) {
        return pathMatcher.matches(properties.path(), request.getPath());
    }

    @Override
    public Publisher<MutableHttpResponse<?>> handle(MutableHttpRequest<?> request) {
        return Publishers.map(handleRequest(request.mutate()), this::handleResponse);
    }

    private Publisher<MutableHttpResponse<?>> handleRequest(MutableHttpRequest<?> request) {
        String targetedInfo = pathMatcher.extractPathWithinPattern(properties.path(), request.getPath());
        String[] pathSegments = Arrays.stream(targetedInfo.split("/"))
                .filter(s -> s != null && !s.isEmpty())
                .toArray(String[]::new);

        String target = pathSegments[0];
        StaticTargetHandlerProperties.TargetProperties targetProperties = properties.targets().get(target);

        if (targetProperties == null) {
            return Mono.just(HttpResponse.badRequest()
                    .body(new JsonError("Target " + target + " is not found")
                            .link(Link.SELF, Link.of(request.getUri()))));
        }

        MutableHttpRequest<?> req = request.uri(uriBuilder -> {
                            uriBuilder.scheme(targetProperties.scheme())
                                    .host(targetProperties.host())
                                    .port(targetProperties.port())
                                    .replacePath(targetProperties.to())
                                    .path(String.join("/",
                                            Arrays.copyOfRange(pathSegments, 1, pathSegments.length)));
                        }
                )
                .header("X-My-Request-Header", "XXX");

        return client.proxy(req);
    }

    private MutableHttpResponse<?> handleResponse(MutableHttpResponse<?> response) {
        return response.header("X-My-Response-Header", "YYY")
                .headers(headers -> {
                    headers.asMap().keySet().stream()
                            .filter(k -> k.toLowerCase().startsWith("x-")
                                    || k.toLowerCase().startsWith("server")
                                    || k.toLowerCase().startsWith("via")
                                    || k.toLowerCase().startsWith("e"))
                            .forEach(headers::remove);
                });
    }

}
