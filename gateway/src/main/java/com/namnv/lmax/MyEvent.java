package com.namnv.lmax;

import io.vertx.core.http.HttpServerRequest;

public class MyEvent {
    long correlationId;
    HttpServerRequest httpServerRequest;

    public MyEvent() {
    }

    public MyEvent(long correlationId, HttpServerRequest httpServerRequest) {
        this.correlationId = correlationId;
        this.httpServerRequest = httpServerRequest;
    }

    public long getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(long correlationId) {
        this.correlationId = correlationId;
    }

    public HttpServerRequest getHttpServerRequest() {
        return httpServerRequest;
    }

    public void setHttpServerRequest(HttpServerRequest httpServerRequest) {
        this.httpServerRequest = httpServerRequest;
    }

}
