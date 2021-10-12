package com.github.alexeylapin.proxy;

import io.micronaut.core.io.buffer.ByteBuffer;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.client.StreamingHttpClient;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
public class ProxyController {

    private final StreamingHttpClient httpClient;
    private final Map<String, ProxyProperties> proxies;

    public ProxyController(StreamingHttpClient httpClient, List<ProxyProperties> proxies) {
        this.httpClient = httpClient;
        this.proxies = proxies.stream().collect(Collectors.toMap(ProxyProperties::getName, Function.identity()));
    }

    @ExecuteOn(TaskExecutors.IO)
    @Get(uri = "/${proxy.base-path:proxy1}/{to}/{+path}")
    public Publisher<HttpResponse<?>> get(@PathVariable String to, @PathVariable String path) {
        ProxyProperties proxy = proxies.get(to);
        if (proxy == null) {
            return Mono.just(HttpResponse.badRequest("definition [" + to + "] not found")
                    .contentType(MediaType.TEXT_PLAIN_TYPE));
        }

        URI uri = UriBuilder.of("")
                .scheme(proxy.getScheme())
                .host(proxy.getHost())
                .port(proxy.getPort())
                .path(proxy.getTo())
                .path(path)
                .build();

        return Mono.from(httpClient.exchange(HttpRequest.GET(uri), Argument.of(byte[].class)))
                .map(r -> HttpResponse.ok()
                        .status(r.status())
                        .contentLength(r.getContentLength())
                        .contentType(r.getContentType().orElse(MediaType.APPLICATION_OCTET_STREAM_TYPE))
                        .header(HttpHeaders.CONTENT_DISPOSITION, r.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION))
                        .body(r.getBody().orElse(null)));
    }

    @ExecuteOn(TaskExecutors.IO)
    @Get(uri = "/${proxy.base-path-streaming:proxy2}/{to}/{+path}", processes = MediaType.APPLICATION_OCTET_STREAM)
    public Flux<?> stream(@PathVariable String to, @PathVariable String path) {
        ProxyProperties proxy = proxies.get(to);
        if (proxy == null) {
            return Flux.just(HttpResponse.badRequest("definition [" + to + "] not found")
                    .contentType(MediaType.TEXT_PLAIN_TYPE));
        }

        URI uri = UriBuilder.of("")
                .scheme(proxy.getScheme())
                .host(proxy.getHost())
                .port(proxy.getPort())
                .path(proxy.getTo())
                .path(path)
                .build();

        return Flux.from(httpClient.dataStream(HttpRequest.GET(uri)))
                .doOnComplete(() -> System.out.println("completed"))
                .map(ByteBuffer::toByteArray);
    }

}
