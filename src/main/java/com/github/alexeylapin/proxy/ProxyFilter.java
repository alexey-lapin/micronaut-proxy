package com.github.alexeylapin.proxy;

import com.github.alexeylapin.proxy.config.MicronautProxyProperties;
import com.github.alexeylapin.proxy.config.TargetProperties;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.client.ProxyHttpClient;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.hateoas.JsonError;
import io.micronaut.http.hateoas.Link;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Filter("/${micronaut-proxy.targeted-path}/**")
public class ProxyFilter implements HttpServerFilter {

    private static final Logger log = LoggerFactory.getLogger(ProxyFilter.class);

    private final ProxyHttpClient client;
    private final Map<String, TargetProperties> targets;
    private final MicronautProxyProperties micronautProxyProperties;

    public ProxyFilter(ProxyHttpClient client,
                       List<TargetProperties> targets,
                       MicronautProxyProperties micronautProxyProperties) {
        this.client = client;
        this.targets = targets.stream().collect(Collectors.toMap(TargetProperties::name, Function.identity()));
        this.micronautProxyProperties = micronautProxyProperties;
        log.info("targets: {}", this.targets);
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        return Publishers.map(handleRequest(request.mutate()), this::handleResponse);
    }

    private Publisher<MutableHttpResponse<?>> handleRequest(MutableHttpRequest<?> request) {
        int length = micronautProxyProperties.targetedPath().length() + 1;
        String targetedPath = request.getPath().substring(length);
        String[] pathSegments = Arrays.stream(targetedPath.split("/"))
                .filter(s -> s != null && !s.isEmpty())
                .toArray(String[]::new);

        String target = pathSegments[0];
        TargetProperties targetProperties = targets.get(target);

        if (targetProperties == null) {
            return Mono.just(HttpResponse.notFound()
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
