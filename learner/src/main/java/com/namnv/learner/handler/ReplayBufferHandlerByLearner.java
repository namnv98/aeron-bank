package com.namnv.learner.handler;

import java.time.Instant;

import com.namnv.command.handler.CommandHandler;
import com.namnv.core.state.StateMachineManager;
import com.namnv.learner.config.ApplicationConfig;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class ReplayBufferHandlerByLearner implements ReplayBufferHandler {
  private final CommandHandler commandHandler;
  private final StateMachineManager stateMachineManager;
  private final ApplicationConfig.LearnerProperties learnerProperties;

  @Setter private int eventCount;
  private Instant lastSnapshot = Instant.now();

  @Override
  public void onEvent(ReplayBufferEvent event, long sequence, boolean endOfBatch) throws Exception {
    commandHandler.onCommand(event.getCommand());
    eventCount--;
    if (endOfBatch || shouldSnapshot()) {
//      System.out.println("snapshot: " + (learnerProperties.getSnapshotFragmentSize() - eventCount));
      stateMachineManager.takeSnapshot();
      resetAfterSnapshot();
    }
  }

  private boolean shouldSnapshot() {
    return eventCount < 0
        || Instant.now().compareTo(lastSnapshot.plus(learnerProperties.getSnapshotLifeTime())) > 0;
  }

  private void resetAfterSnapshot() {
    eventCount = learnerProperties.getSnapshotFragmentSize();
    lastSnapshot = Instant.now();
  }
}
