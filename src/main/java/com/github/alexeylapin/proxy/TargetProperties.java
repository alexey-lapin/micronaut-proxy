package com.github.alexeylapin.proxy;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;

@EachProperty("micronaut-proxy.targets")
public class TargetProperties {

    private final String name;
    private String scheme = "http";
    private String host;
    private int port = 80;
    private String from = "";
    private String to = "";

    public TargetProperties(@Parameter String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

}
//@EachProperty("micronaut-proxy.targets")
//public record TargetProperties(@Parameter String name, String scheme, String host, int port, String from, String to) {
//
//}
