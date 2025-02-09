package com.namnv.lmax;

import com.lmax.disruptor.EventHandler;
import com.namnv.client.ClientEgressListener;
import com.namnv.client.ClientIngressSender;

public class MyEventHandler implements EventHandler<MyEvent> {
    ClientIngressSender clientIngressSender;
    final ClientEgressListener clientEgressListener;

    public MyEventHandler(ClientIngressSender clientIngressSender, ClientEgressListener clientEgressListener) {
        this.clientIngressSender = clientIngressSender;
        this.clientEgressListener = clientEgressListener;
    }

    @Override
    public void onEvent(MyEvent event, long sequence, boolean endOfBatch) {
        clientEgressListener.addHttpRequest(event.correlationId, event.httpServerRequest);
        clientIngressSender.sendOrderRequestToCluster(event.correlationId, 1, 2, 1);
    }
}

