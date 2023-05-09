package com.github.alexeylapin.proxy;

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.client.ProxyHttpClient;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Filter("${micronaut-proxy.targeted-path}")
public class ProxyFilter implements HttpServerFilter {

    private static final Logger log = LoggerFactory.getLogger(ProxyFilter.class);

    private final ProxyHttpClient client;
    private final Map<String, TargetProperties> targets;
    private final MicronautProxyProperties properties;

    public ProxyFilter(ProxyHttpClient client, List<TargetProperties> targets, MicronautProxyProperties properties) {
        this.client = client;
        this.targets = targets.stream().collect(Collectors.toMap(TargetProperties::name, Function.identity()));
        this.properties = properties;
        log.info("targets: {}", this.targets);
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        return Publishers.map(client.proxy(
                request.mutate()
                        .uri(b -> {
                                    String[] r = Arrays.stream(request.getPath().substring(properties.targetedPath().length())
                                                    .split("/"))
                                            .filter(s -> s != null && !s.isEmpty())
                                            .toArray(String[]::new);
                                    TargetProperties p = targets.get(r[0]);

                                    b
                                            .scheme(p.scheme())
                                            .host(p.host())
                                            .port(p.port())
                                            .replacePath(p.to())
                                            .path(String.join("/", Arrays.copyOfRange(r, 1, r.length)));
                                }
                        )
                        .header("X-My-Request-Header", "XXX")
        ), response -> response.header("X-My-Response-Header", "YYY")
                .headers(h -> {
                    h.asMap().keySet().stream()
                            .filter(k -> k.toLowerCase().startsWith("x-")
                                    || k.toLowerCase().startsWith("server")
                                    || k.toLowerCase().startsWith("via")
                                    || k.toLowerCase().startsWith("e"))
                            .forEach(h::remove);
                }));
    }

}
