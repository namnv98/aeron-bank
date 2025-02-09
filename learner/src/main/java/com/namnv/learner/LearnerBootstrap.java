package com.namnv.learner;

import com.lmax.disruptor.dsl.Disruptor;
import com.namnv.command.core.BaseCommand;
import com.namnv.command.core.CommandLog;
import com.namnv.command.core.TransferCommand;
import com.namnv.core.repository.Balances;
import com.namnv.core.ClusterBootstrap;
import com.namnv.core.model.ClusterStatus;
import com.namnv.core.state.StateMachineManager;
import com.namnv.learner.handler.ReplayBufferEvent;
import com.namnv.learner.replay.MessageSubscriber;
import com.namnv.learner.replay.ReplayMergeReceiver;
import com.weareadaptive.sbe.MessageHeaderDecoder;
import com.weareadaptive.sbe.TransferRequestDecoder;
import io.aeron.logbuffer.Header;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.agrona.DirectBuffer;

@Slf4j
@RequiredArgsConstructor
public class LearnerBootstrap implements ClusterBootstrap {
  private final StateMachineManager stateMachineManager;
  private final ReplayMergeReceiver replayMergeReceiver;
  private final Disruptor<ReplayBufferEvent> replayBufferEventDisruptor;
  private final Balances balances;

  @Override
  public void onStart() {
    try {
      log.info("Learner start");
      loadingStateMachine();
      activeReplayChannel();
      startReplayMessage();
      activeCLuster();
      log.info("Learner started");
    } catch (Exception e) {
      log.error("Learner start failed", e);
      System.exit(-9);
    }
  }

  private void loadingStateMachine() {
    stateMachineManager.reloadSnapshot();
  }

  private void activeReplayChannel() {
    replayBufferEventDisruptor.start();
  }

  private void startReplayMessage() {
    replayMergeReceiver.addSubscriber(
        new MessageSubscriber() {
          @Override
          public void onSessionMessage(
              long clusterSessionId,
              long timestamp,
              DirectBuffer buffer,
              int offset,
              int length,
              Header header) {
            var headerDecoder = new MessageHeaderDecoder();
            headerDecoder.wrap(buffer, offset);
            long correlationId = headerDecoder.correlationId();

            var transferRequestDecoder = new TransferRequestDecoder();
            var bufferOffset = offset + headerDecoder.encodedLength();
            transferRequestDecoder.wrap(
                buffer, bufferOffset, headerDecoder.blockLength(), headerDecoder.version());

            double fromId = transferRequestDecoder.fromId();
            double toId = transferRequestDecoder.toId();
            double amount = transferRequestDecoder.amount();
            var position = header.position();
            replayBufferEventDisruptor.publishEvent(
                (event, sequence) -> {
                  event.setCommand(
                      new BaseCommand(
                          new CommandLog(4, position, new TransferCommand(fromId, toId, amount))));
                });
          }
        });
    replayMergeReceiver.start(balances.getOffset());
  }

  private void activeCLuster() {
    ClusterStatus.STATE.set(ClusterStatus.ACTIVE);
  }

  @Override
  public void onStop() {}
}
