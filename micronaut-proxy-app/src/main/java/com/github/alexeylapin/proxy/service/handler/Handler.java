package com.github.alexeylapin.proxy.service.handler;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.MutableHttpResponse;
import org.reactivestreams.Publisher;

public interface Handler {

    boolean matches(HttpRequest<?> request);

    Publisher<MutableHttpResponse<?>> handle(MutableHttpRequest<?> request);

}
