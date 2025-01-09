package com.namnv.client;

import com.namnv.services.bank.util.Method;
import io.aeron.Publication;
import io.aeron.cluster.client.AeronCluster;
import org.agrona.BitUtil;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
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

  private final MutableDirectBuffer actionBidBuffer = new ExpandableArrayBuffer();
  private final IdleStrategy idleStrategy = new BackoffIdleStrategy();
  private static final long RETRY_COUNT = 10;

  public void sendOrderRequestToCluster(long correlationId, final long fromId, final long todId, final double amount) {
    int offset = 0;
    actionBidBuffer.putLong(offset, correlationId);
    offset += BitUtil.SIZE_OF_LONG;

    int retries = 0;
    do {
      final long result = aeronCluster.offer(actionBidBuffer, 0, offset);
      if (result > 0L) {
        return;
      } else if (result == Publication.ADMIN_ACTION || result == Publication.BACK_PRESSURED) {
        System.out.println("backpressure or admin action on cluster offer");
      } else if (result == Publication.NOT_CONNECTED || result == Publication.MAX_POSITION_EXCEEDED) {
        System.out.println("Cluster is not connected, or maximum position has been exceeded. Message lost.");
        return;
      }

      idleStrategy.idle();
      retries += 1;
      System.out.println("failed to send message to cluster. Retrying (" + retries + " of " + RETRY_COUNT + ")");
    }
    while (retries < RETRY_COUNT);


  }
}
