package com.namnv;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.namnv.client.ClientEgressListener;
import com.namnv.client.ClientIngressSender;
import com.namnv.lmax.MyEvent;
import com.namnv.lmax.MyEventFactory;
import com.namnv.lmax.MyEventHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerRequest;

import java.util.concurrent.Executors;

public class HttpServer extends AbstractVerticle {
    ClientIngressSender clientIngressSender;
    final ClientEgressListener clientEgressListener;
    private long id = 0L;

    public HttpServer(final ClientIngressSender clientIngressSender, ClientEgressListener clientEgressListener) {
        this.clientIngressSender = clientIngressSender;
        this.clientEgressListener = clientEgressListener;
    }

    RingBuffer<MyEvent> ringBuffer;

    @Override
    public void start() {

        // Create the factory
        MyEventFactory factory = new MyEventFactory();

        // Set up the ring buffer size (must be a power of 2)
        int bufferSize = 2048;

        // Create the disruptor
        Disruptor<MyEvent> disruptor = new Disruptor<>(factory, bufferSize, Executors.defaultThreadFactory());

        // Create the event handler
        MyEventHandler handler = new MyEventHandler(clientIngressSender, clientEgressListener);

        // Connect the handler to the disruptor
        disruptor.handleEventsWith(handler);

        // Start the disruptor
        disruptor.start();

        ringBuffer = disruptor.getRingBuffer();

        vertx.createHttpServer()
                .requestHandler(this::WSHandler)
                .listen(8080);
    }

    private void WSHandler(HttpServerRequest httpServerRequest) {
        String path = httpServerRequest.path();
        switch (path) {
            case "/transfer" -> transfer(httpServerRequest, ++id);
        }

    }

    private void transfer(HttpServerRequest httpServerRequest, long correlationId) {
        long sequence = ringBuffer.next();
        try {
            MyEvent event = ringBuffer.get(sequence);
            event.setHttpServerRequest(httpServerRequest);
            event.setCorrelationId(correlationId);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

}