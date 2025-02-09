package com.namnv.learner.replay;

public interface ReplayMergeReceiver {

  void start(long lastPosition);

  void addSubscriber(MessageSubscriber subscriber);
}
