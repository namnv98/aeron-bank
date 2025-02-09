package com.namnv.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.namnv.ClusterNodeBootstrap;
import com.namnv.ClusterService;
import com.namnv.command.disruptor.RequestDisruptorDSL;
import com.namnv.command.disruptor.event.RequestEvent;
import com.namnv.command.disruptor.event.RequestEventDispatcher;
import com.namnv.command.disruptor.reply.CommandReplyHandler;
import com.namnv.command.disruptor.reply.CommandReplyHandlerImpl;
import com.namnv.command.disruptor.request.CommandBufferHandler;
import com.namnv.command.disruptor.request.CommandBufferHandlerImpl;
import com.namnv.command.disruptor.request.RequestEventDispatcherImpl;
import com.namnv.command.handler.CommandHandler;
import com.namnv.command.handler.CommandHandlerImpl;
import com.namnv.config.ApplicationConfig;
import com.namnv.core.ClusterBootstrap;
import com.namnv.core.repository.BalanceRepositoryImpl;
import com.namnv.core.repository.Balances;
import com.namnv.core.state.StateMachineManager;
import com.namnv.core.state.StateMachineManagerImpl;
import com.namnv.ClusterNode;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ClusterAppModule extends AbstractModule {
  private final ApplicationConfig applicationConfig;

  @Override
  protected void configure() {}

  @Provides
  @Singleton
  CommandHandler commandHandler(Balances balances) {
    return new CommandHandlerImpl(balances);
  }

  @Provides
  @Singleton
  CommandReplyHandler commandReplyHandler() {
    return new CommandReplyHandlerImpl();
  }

  @Provides
  @Singleton
  Disruptor<RequestEvent> requestEventDisruptor(
      CommandBufferHandler commandHandler, CommandReplyHandler commandReplyHandler) {
    return new RequestDisruptorDSL(commandHandler, commandReplyHandler)
        .build(1024 * 8, new BusySpinWaitStrategy());
  }

  @Provides
  @Singleton
  RequestEventDispatcher requestEventDispatcher(Disruptor<RequestEvent> requestEventDisruptor) {
    requestEventDisruptor.start();
    return new RequestEventDispatcherImpl(requestEventDisruptor);
  }

  @Provides
  @Singleton
  CommandBufferHandler commandBufferHandler(CommandHandler commandHandler) {
    return new CommandBufferHandlerImpl(commandHandler);
  }

  @Provides
  @Singleton
  ClusterService clusterService(RequestEventDispatcher requestEventDispatcher) {
    return new ClusterService(requestEventDispatcher);
  }

  @Provides
  @Singleton
  ClusterNode ClusterNode(ClusterService clusterService) {
    return new ClusterNode(clusterService);
  }

  @Provides
  @Singleton
  ClusterBootstrap clusterBootstrap(
      StateMachineManager stateMachineManager, Balances balances, ClusterNode clusterNode) {
    return new ClusterNodeBootstrap(stateMachineManager, balances, clusterNode, applicationConfig);
  }

  @Provides
  @Singleton
  StateMachineManager provideStateMachineManager(
      Balances balances, BalanceRepositoryImpl balanceRepository) {
    return new StateMachineManagerImpl(balances, balanceRepository);
  }
}
