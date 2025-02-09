package com.namnv.learner.replay;

import io.aeron.logbuffer.Header;
import java.util.ArrayList;
import java.util.List;
import org.agrona.DirectBuffer;

public class MessageSubscriberManager {
  private final List<MessageSubscriber> messageSubscribers = new ArrayList<>();

  public void addSubscriber(MessageSubscriber subscriber) {
    messageSubscribers.add(subscriber);
  }

  public void handleMessage(
      long clusterSessionId,
      long timestamp,
      DirectBuffer buffer,
      int offset,
      int length,
      Header header) {
    for (MessageSubscriber subscriber : messageSubscribers) {
      subscriber.onSessionMessage(clusterSessionId, timestamp, buffer, offset, length, header);
    }
  }
}
