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

  public void sendOrderRequestToCluster(long correlationId, final long fromId, final long todId, final double amount) {
      int offset = 0;
      actionBidBuffer.putLong(offset, correlationId);
      offset += BitUtil.SIZE_OF_LONG;

      actionBidBuffer.putLong(offset, 1);
      offset += BitUtil.SIZE_OF_LONG;

      byte[] fullName = "nguyen van nam".getBytes(StandardCharsets.UTF_8);
      actionBidBuffer.putInt(offset, fullName.length);
      offset += BitUtil.SIZE_OF_INT;

      actionBidBuffer.putBytes(offset, fullName);
      offset += fullName.length;

      idleStrategy.reset();
      while (aeronCluster.offer(actionBidBuffer, 0, offset) < 0) {
        idleStrategy.idle(aeronCluster.pollEgress());
      }
      LOGGER.info("OrderRequest is being sent to cluster " + ++correlationId);

//        sendMessageToCluster(encoder.encodeOrderRequest(correlationId, fromId, todId, amount), encoder.ORDER_REQUEST_LENGTH);

  }
}
