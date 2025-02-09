package com.namnv.learner.module;

import static java.lang.System.setProperty;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.namnv.command.handler.CommandHandler;
import com.namnv.command.handler.CommandHandlerImpl;
import com.namnv.core.ClusterBootstrap;
import com.namnv.core.repository.BalanceRepositoryImpl;
import com.namnv.core.repository.Balances;
import com.namnv.core.state.StateMachineManager;
import com.namnv.core.state.StateMachineManagerImpl;
import com.namnv.learner.LearnerBootstrap;
import com.namnv.learner.config.ApplicationConfig;
import com.namnv.learner.handler.*;
import com.namnv.learner.replay.ReplayMergeReceiver;
import com.namnv.learner.replay.ReplayMergeReceiverImpl;
import io.aeron.Aeron;
import io.aeron.CommonContext;
import io.aeron.archive.client.AeronArchive;
import io.aeron.driver.MediaDriver;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class LearnerAppModule extends AbstractModule {
  private final ApplicationConfig applicationConfig;

  @Override
  protected void configure() {}

  @Provides
  @Singleton
  MediaDriver MediaDriver() {
    setProperty("aeron.socket.so_sndbuf", "2M");
    setProperty("aeron.socket.so_rcvbuf", "2M");

    setProperty("agrona.disable.bounds.checks", "true");
    setProperty("aeron.rcv.initial.window.length", "2M");

    Path aeronPath = Paths.get(CommonContext.generateRandomDirName());

    return MediaDriver.launch(
        new MediaDriver.Context()
            .aeronDirectoryName(aeronPath.toString())
            .spiesSimulateConnection(true));
  }

  @Provides
  @Singleton
  Aeron aeron(MediaDriver mediaDriver) {
    return Aeron.connect(new Aeron.Context().aeronDirectoryName(mediaDriver.aeronDirectoryName()));
  }

  @Provides
  @Singleton
  AeronArchive AeronArchive(Aeron aeron) {
    String AERON_UDP_ENDPOINT = "aeron:udp?endpoint=";
    String THIS_HOST = "127.0.0.1";
    int ARCHIVE_CONTROL_PORT = 8001;
    int ARCHIVE_EVENT_PORT = 8002;
    return AeronArchive.connect(
        new AeronArchive.Context()
            .aeron(aeron)
            .controlRequestChannel(AERON_UDP_ENDPOINT + "localhost" + ":" + ARCHIVE_CONTROL_PORT)
            .recordingEventsChannel(AERON_UDP_ENDPOINT + "localhost" + ":" + ARCHIVE_EVENT_PORT)
            .controlResponseChannel(AERON_UDP_ENDPOINT + THIS_HOST + ":0"));
  }

  @Provides
  @Singleton
  ReplayMergeReceiver replayMergeReceiver(
      Aeron aeron, AeronArchive aeronArchive, Balances balances) {
    String THIS_HOST = "127.0.0.1";
    return ReplayMergeReceiverImpl.builder()
        .liveHost(THIS_HOST)
        .livePort(8001)
        .thisHost(THIS_HOST)
        .streamId(100)
        .aeron(aeron)
        .aeronArchive(aeronArchive)
        .fragmentLimit(3000)
        .build();
  }

  @Provides
  @Singleton
  ClusterBootstrap clusterBootstrap(
      StateMachineManager stateMachineManager,
      Disruptor<ReplayBufferEvent> replayBufferEventDisruptor,
      ReplayMergeReceiver replayMergeReceiver,
      Balances balances) {
    return new LearnerBootstrap(
        stateMachineManager, replayMergeReceiver, replayBufferEventDisruptor, balances);
  }

  @Provides
  @Singleton
  CommandHandler provideCommandHandler(Balances balances) {
    return new CommandHandlerImpl(balances);
  }

  @Provides
  @Singleton
  StateMachineManager provideStateMachineManager(
      Balances balances, BalanceRepositoryImpl balanceRepository) {
    return new StateMachineManagerImpl(balances, balanceRepository);
  }

  @Provides
  @Singleton
  ReplayBufferHandler replayBufferHandlerByLearner(
      CommandHandler commandHandler, StateMachineManager stateMachineManager) {
    var replayHandler =
        new ReplayBufferHandlerByLearner(
            commandHandler, stateMachineManager, applicationConfig.getLearner());
    replayHandler.setEventCount(applicationConfig.getLearner().getBufferSize());
    return replayHandler;
  }

  @Provides
  @Singleton
  Disruptor<ReplayBufferEvent> replayBufferEventDisruptor(ReplayBufferHandler replayBufferHandler) {
    return new ReplayBufferDisruptorDSL(replayBufferHandler)
        .build(2048 * 2, new SleepingWaitStrategy());
  }
}
