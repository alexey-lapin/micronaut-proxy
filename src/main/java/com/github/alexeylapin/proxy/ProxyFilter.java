package com.github.alexeylapin.proxy;

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.client.ProxyHttpClient;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Filter("/proxy4/**")
public class ProxyFilter implements HttpServerFilter {

    private final ProxyHttpClient client;
    private final Map<String, ProxyProperties> proxies;

    public ProxyFilter(ProxyHttpClient client, List<ProxyProperties> proxies) { // (2)
        this.client = client;
        this.proxies = proxies.stream().collect(Collectors.toMap(ProxyProperties::getName, Function.identity()));
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request,
                                                      ServerFilterChain chain) {
        return Publishers.map(client.proxy( // (3)
                request.mutate() // (4)
                        .uri(b -> {
                                    String[] r = Arrays.stream(request.getPath().substring("/proxy4".length())
                                                    .split("/"))
                                            .filter(s -> s != null && !s.isEmpty())
                                            .toArray(String[]::new);
                                    ProxyProperties p = proxies.get(r[0]);

                                    b // (5)
                                            .scheme(p.getScheme())
                                            .host(p.getHost())
                                            .port(p.getPort())
                                            .replacePath(p.getTo())
                                            .path(String.join("/", Arrays.copyOfRange(r, 1, r.length)));
                                }
                        )
                        .header("X-My-Request-Header", "XXX") // (6)
        ), response -> response.header("X-My-Response-Header", "YYY")
                .headers(h -> {
                    h.asMap().keySet().stream()
                            .filter(k -> {
                                return k.toLowerCase().startsWith("x-")
                                        || k.toLowerCase().startsWith("server")
                                        || k.toLowerCase().startsWith("via")
                                        || k.toLowerCase().startsWith("e");
                            })
                            .forEach(h::remove);
                }));
    }

}
