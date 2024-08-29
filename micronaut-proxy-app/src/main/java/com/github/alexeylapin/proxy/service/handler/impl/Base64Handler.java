package com.github.alexeylapin.proxy.service.handler.impl;

import com.github.alexeylapin.proxy.config.handler.Base64HandlerProperties;
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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Base64Handler implements Handler {

    private final Base64HandlerProperties properties;
    private final ProxyHttpClient client;
    private final AntPathMatcher pathMatcher;

    public Base64Handler(Base64HandlerProperties properties, ProxyHttpClient client) {
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
        String targetInfo = pathMatcher.extractPathWithinPattern(properties.path(), request.getPath());

        String targetString;
        try {
            byte[] bytes = Base64.getDecoder().decode(targetInfo);
            targetString = new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return Mono.just(HttpResponse.badRequest()
                    .body(new JsonError("Target is not properly encoded")
                            .link(Link.SELF, Link.of(request.getUri()))));
        }

        URI targetUri;
        try {
            targetUri = URI.create(targetString);
        } catch (Exception ex) {
            return Mono.just(HttpResponse.badRequest()
                    .body(new JsonError("Target is not a valid URI")
                            .link(Link.SELF, Link.of(request.getUri()))));
        }

        MutableHttpRequest<?> req = request.uri(targetUri)
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
