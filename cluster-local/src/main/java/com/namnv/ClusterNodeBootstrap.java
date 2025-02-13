package com.namnv;

import com.namnv.config.ApplicationConfig;
import com.namnv.core.model.ClusterStatus;
import com.namnv.core.repository.Balances;
import com.namnv.core.state.StateMachineManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ClusterNodeBootstrap implements com.namnv.core.ClusterBootstrap {
  private final StateMachineManager stateMachineManager;
  private final Balances balances;
  private final ClusterNode clusterNode;
  private final ApplicationConfig applicationConfig;

  @Override
  public void onStart() {
    try {
      log.info("Learner start");
      loadingStateMachine();
      activeReplayChannel();
      startNode();
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

  private void activeReplayChannel() {}

  private void startNode() {
    clusterNode.startNode(
        applicationConfig.getNodeID(), applicationConfig.getMaxNodes(), balances.getOffset());
  }

  private void activeCLuster() {
    ClusterStatus.STATE.set(ClusterStatus.ACTIVE);
  }

  @Override
  public void onStop() {}
}
