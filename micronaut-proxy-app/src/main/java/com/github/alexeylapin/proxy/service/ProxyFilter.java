package com.github.alexeylapin.proxy.service;

import com.github.alexeylapin.proxy.service.handler.Handler;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.hateoas.JsonError;
import io.micronaut.http.hateoas.Link;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;

@Filter("/${micronaut-proxy.proxy-filter-path:}/**")
public class ProxyFilter implements HttpServerFilter {

    private static final Logger log = LoggerFactory.getLogger(ProxyFilter.class);

    private final List<Handler> handlers;

    public ProxyFilter(List<Handler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        for (Handler handler : handlers) {
            if (handler.matches(request)) {
                return handler.handle(request.mutate());
            }
        }

        return Mono.just(HttpResponse.badRequest()
                .body(new JsonError("Request does not match any handler")
                        .link(Link.SELF, Link.of(request.getUri()))));
    }

}
