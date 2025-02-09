package com.namnv.learner.handler;

import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.namnv.command.disruptor.DisruptorDSL;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ReplayBufferDisruptorDSL implements DisruptorDSL<ReplayBufferEvent> {
  private final ReplayBufferHandler replayBufferHandler;

  @Override
  public Disruptor<ReplayBufferEvent> build(int bufferSize, WaitStrategy waitStrategy) {
    var disruptor =
        new Disruptor<>(
            ReplayBufferEvent::new,
            bufferSize,
            DaemonThreadFactory.INSTANCE,
            ProducerType.SINGLE,
            waitStrategy);
    disruptor.handleEventsWith(replayBufferHandler);
    return disruptor;
  }
}
