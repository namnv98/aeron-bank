package com.namnv.client;

import com.namnv.services.bank.util.Method;
import io.aeron.Publication;
import io.aeron.cluster.client.AeronCluster;
import org.agrona.DirectBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientIngressSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientIngressSender.class);
    private AeronCluster aeronCluster;
    final private ConcurrentLinkedQueue<DirectBuffer> bufferQueue = new ConcurrentLinkedQueue<>();
    final private Queue<Integer> lengthQueue = new LinkedList<>();
    final private Encoder encoder = new Encoder();

    public ClientIngressSender(final AeronCluster aeronCluster) {
        this.aeronCluster = aeronCluster;
    }

    public void sendMessageToCluster(final DirectBuffer buffer, final int length) {
        long offerResponse = 0;
        bufferQueue.add(buffer);
        lengthQueue.add(length);
        while (!bufferQueue.isEmpty() && !lengthQueue.isEmpty()) {
            offerResponse = aeronCluster.offer(bufferQueue.peek(), 0, lengthQueue.peek());
            if (offerResponse < 0) break;
            else {
                bufferQueue.remove();
                lengthQueue.remove();
            }
        }

        if (offerResponse == (int) Publication.MAX_POSITION_EXCEEDED ||
                offerResponse == (int) Publication.CLOSED ||
                offerResponse == (int) Publication.NOT_CONNECTED) {
            bufferQueue.clear();
            lengthQueue.clear();
        }
    }

    public void sendOrderRequestToCluster(final long correlationId, final long fromId, final long todId, final double amount) {
        LOGGER.info("OrderRequest is being sent to cluster");
        sendMessageToCluster(encoder.encodeOrderRequest(correlationId, fromId, todId, amount), encoder.ORDER_REQUEST_LENGTH);
    }


    public void sendHeaderMessageToCluster(long correlationId, Method method) {
        LOGGER.info("HeaderMessage is being sent to cluster");
        sendMessageToCluster(encoder.encodeHeaderMessage(correlationId, method), encoder.HEADER_LENGTH);
    }
}
