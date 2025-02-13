package com.namnv.client;

import io.aeron.Publication;
import io.aeron.cluster.client.AeronCluster;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingMillisIdleStrategy;
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


//    private static final int RETRY_COUNT = 3;
//
//    protected IdleStrategy[] idleStrategy = new IdleStrategy[RETRY_COUNT];
//
//    {
//        for (int i = 0; i < RETRY_COUNT; i++) {
//            idleStrategy[i] = new SleepingMillisIdleStrategy((long) Math.min(100 * Math.pow(2, i), 1_000));
//        }
//    }
//    public ClientIngressSender(final AeronCluster aeronCluster) {
//        this.aeronCluster = aeronCluster;
//    }
//
//    public void sendMessageToCluster(final DirectBuffer buffer, final int length) {
//        int retries = 0;
//        while (retries < RETRY_COUNT) {
//            final long result = aeronCluster.offer(buffer, 0, length);
//            if (result > 0L) return;
//            final int resultInt = (int) result;
//            switch (resultInt) {
//                case (int) Publication.ADMIN_ACTION -> log.warn("Admin action on cluster offer");
//                case (int) Publication.BACK_PRESSURED -> log.warn("Backpressure on cluster offer");
//                case (int) Publication.NOT_CONNECTED -> {
//                    log.warn("Cluster is not connected. Message loses.");
//                }
//                case (int) Publication.MAX_POSITION_EXCEEDED ->
//                        log.warn("Maximum position has been exceeded. Message lost.");
//                case (int) Publication.CLOSED -> {
//                    log.error("Cluster connection is closed");
//                }
//            }
//            retries++;
////            idleStrategy[retries++].idle();
//            log.warn("Failed to send message to cluster. Retrying ( {} of {} )", retries, RETRY_COUNT);
//        }
//        log.error("Failed to send message to cluster. Message lost.");
//    }

    public void sendOrderRequestToCluster(final long correlationId, final long fromId, final long todId, final double amount) {
        sendMessageToCluster(encoder.encodeOrderRequest(correlationId, fromId, todId, amount), encoder.ORDER_REQUEST_LENGTH);
    }
}
