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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
public class ProxyController {

    private static final Logger logger = LoggerFactory.getLogger(ProxyController.class);

    private final StreamingHttpClient httpClient;
    private final Map<String, ProxyProperties> proxies;

    public ProxyController(StreamingHttpClient httpClient, List<ProxyProperties> proxies) {
        this.httpClient = httpClient;
        this.proxies = proxies.stream().collect(Collectors.toMap(ProxyProperties::getName, Function.identity()));
        logger.info("initialized proxies:");
        logger.info(this.proxies.toString());
    }

    @ExecuteOn(TaskExecutors.IO)
    @Get(uri = "/${proxy.base-path:proxy1}/{to}/{+path}")
    public Publisher<HttpResponse<?>> download(@PathVariable String to, @PathVariable String path) {
        logger.info("downloading to={} path={}", to, path);
        ProxyProperties proxy = proxies.get(to);
        if (proxy == null) {
            logger.info("downloading proxy to=[{}] not found", to);
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
        logger.info("downloading from=[{}]", uri);

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
        logger.info("streaming requested to=[{}] path=[{}]", to, path);
        ProxyProperties proxy = proxies.get(to);
        if (proxy == null) {
            logger.info("streaming proxy to=[{}] not found", to);
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
        logger.info("streaming from=[{}]", uri);

        return Flux.from(httpClient.dataStream(HttpRequest.GET(uri)))
                .doOnComplete(() -> logger.info("streaming completed"))
                .map(ByteBuffer::toByteArray);
    }

}
