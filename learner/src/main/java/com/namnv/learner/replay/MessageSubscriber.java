package com.namnv.learner.replay;

import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;

public interface MessageSubscriber {
  void onSessionMessage(
      long clusterSessionId,
      long timestamp,
      DirectBuffer buffer,
      int offset,
      int length,
      Header header);
}
