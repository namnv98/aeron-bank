//package com.namnv.node2;
//
//import com.lmax.disruptor.BusySpinWaitStrategy;
//import com.namnv.ClusterService;
//import com.namnv.command.disruptor.RequestDisruptorDSL;
//import com.namnv.command.disruptor.reply.CommandReplyHandlerImpl;
//import com.namnv.command.disruptor.request.CommandBufferHandlerImpl;
//import com.namnv.command.disruptor.request.RequestEventDispatcherImpl;
//import com.namnv.command.handler.CommandHandlerImpl;
//import com.namnv.core.repository.Balances;
//import com.namnv.services.OMSService;
//import io.aeron.archive.Archive;
//import io.aeron.cluster.ConsensusModule;
//import io.aeron.cluster.service.ClusteredServiceContainer;
//import io.aeron.driver.MediaDriver;
//import io.aeron.driver.status.SystemCounterDescriptor;
//import io.aeron.samples.cluster.ClusterConfig;
//import lombok.Setter;
//import org.agrona.CloseHelper;
//import org.agrona.ErrorHandler;
//import org.agrona.concurrent.ShutdownSignalBarrier;
//import org.agrona.concurrent.SleepingMillisIdleStrategy;
//import org.agrona.concurrent.status.AtomicCounter;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.File;
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//
//import static io.aeron.cluster.ConsensusModule.Configuration.LOG_CHANNEL_PROP_NAME;
//import static io.aeron.driver.ThreadingMode.DEDICATED;
//import static java.lang.System.setProperty;
//
//@Setter
//public class ClusterNode2 implements AutoCloseable {
//  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterNode2.class);
//
//  ClusteredServiceContainer clusteredServiceContainer;
//  ClusteredMediaDriver clusteredMediaDriver;
//  ClusterService clusterService;
//  File clusterDir;
//  boolean active = false;
//
//  ShutdownSignalBarrier barrier = new ShutdownSignalBarrier();
//
//  public void startNode(int node, int maxNodes, boolean test) throws Exception {
//    var dirBase = System.getProperty("user.dir") + "/aeron-cluster/node-" + node + "/";
//
//    setProperty(LOG_CHANNEL_PROP_NAME, "aeron:udp?term-length=512k");
//    setProperty("aeron.dir", dirBase);
//
//    var baseDir = new File(dirBase);
//
//    var balances = new Balances();
//    balances.newBalance();
//    balances.newBalance();
//
//    var omsService = new OMSService(null, new SleepingMillisIdleStrategy(), balances);
//
//    var commandHandler = new CommandHandlerImpl(balances);
//    var commandBufferHandler = new CommandBufferHandlerImpl(commandHandler);
//
//    var commandReplyHandler = new CommandReplyHandlerImpl();
//
//    var requestEventDisruptor =
//        new RequestDisruptorDSL(commandBufferHandler, commandReplyHandler)
//            .build(1024 * 8, new BusySpinWaitStrategy());
//    requestEventDisruptor.start();
//    var requestEventDispatcher = new RequestEventDispatcherImpl(requestEventDisruptor);
//
//    clusterService = new ClusterService(omsService, commandHandler, requestEventDispatcher);
//
//    barrier = new ShutdownSignalBarrier();
//
//    var archive =
//        new Archive.Context()
//            .archiveDir(new File(baseDir, "archive"))
//            .controlChannel("aeron:udp?endpoint=127.0.0.1:8101")
//            .localControlChannel("aeron:ipc")
//            .recordingEventsChannel("aeron:udp?endpoint=127.0.0.1:8102")
//            .recordingEventsStreamId(1009)
//            .replicationChannel("aeron:udp?endpoint=127.0.0.1:8106")
//            .segmentFileLength(16 * 1024 * 1024)
//            .recordingEventsEnabled(true);
//
//    List<String> hostAddresses = List.of("localhost", "localhost");
//
//    var clusterConfig =
//        ClusterConfig.create(node, hostAddresses, hostAddresses, 8000, clusterService);
//
//    clusterConfig.consensusModuleContext().ingressChannel("aeron:udp");
//
//    clusterDir = new File(baseDir, "cluster");
//    clusterConfig.clusteredServiceContext().clusterDir(clusterDir);
//    clusterConfig.consensusModuleContext().clusterDir(clusterDir);
//
//    clusterConfig
//        .consensusModuleContext()
//        .ingressChannel("aeron:udp?endpoint=localhost:8107|term-length=64k");
//    clusterConfig.consensusModuleContext().logStreamId(100);
//    clusterConfig.consensusModuleContext().deleteDirOnStart(test);
//    clusterConfig.consensusModuleContext().sessionTimeoutNs(TimeUnit.MINUTES.toNanos(10));
//    clusterConfig.consensusModuleContext().leaderHeartbeatTimeoutNs(TimeUnit.SECONDS.toNanos(1));
//
//    clusterConfig.mediaDriverContext().threadingMode(DEDICATED);
//    clusterConfig.consensusModuleContext().leaderHeartbeatTimeoutNs(TimeUnit.SECONDS.toNanos(1));
//
//    try (ClusteredMediaDriver clusteredMediaDriver =
//            launch(
//                clusterConfig.mediaDriverContext(),
//                archive,
//                clusterConfig.consensusModuleContext());
//        ClusteredServiceContainer clusteredServiceContainer =
//            ClusteredServiceContainer.launch(clusterConfig.clusteredServiceContext()); ) {
//
//      this.clusteredServiceContainer = clusteredServiceContainer;
//      this.clusteredMediaDriver = clusteredMediaDriver;
//      LOGGER.info("Started Cluster Node...");
//      setActive(true);
//
//      barrier.await();
//
//      close();
//      LOGGER.info("Exiting");
//    }
//  }
//
//  static ConsensusModule consensusModule = null;
//  static MediaDriver driver = null;
//
//  public static ClusteredMediaDriver launch(
//      final MediaDriver.Context driverCtx,
//      final Archive.Context archiveCtx,
//      final ConsensusModule.Context consensusModuleCtx) {
//
//    Archive archive = null;
//
//    try {
//      driver = MediaDriver.launch(driverCtx);
//
//      final int errorCounterId = SystemCounterDescriptor.ERRORS.id();
//      final AtomicCounter errorCounter =
//          null != archiveCtx.errorCounter()
//              ? archiveCtx.errorCounter()
//              : new AtomicCounter(driverCtx.countersValuesBuffer(), errorCounterId);
//      final ErrorHandler errorHandler =
//          null != archiveCtx.errorHandler() ? archiveCtx.errorHandler() : driverCtx.errorHandler();
//
//      archive =
//          Archive.launch(
//              archiveCtx
//                  .mediaDriverAgentInvoker(driver.sharedAgentInvoker())
//                  .aeronDirectoryName(driver.aeronDirectoryName())
//                  .errorHandler(errorHandler)
//                  .errorCounter(errorCounter));
//
//      //            consensusModuleCtx.initLogPosition(0l);
//
//      consensusModule =
//          ConsensusModule.launch(
//              consensusModuleCtx.aeronDirectoryName(driverCtx.aeronDirectoryName()));
//
//      return new ClusteredMediaDriver(driver, archive, consensusModule);
//    } catch (final Exception ex) {
//      CloseHelper.quietCloseAll(consensusModule, archive, driver);
//      throw ex;
//    }
//  }
//
//  @Override
//  public void close() {
//    CloseHelper.quietClose(clusteredMediaDriver);
//    CloseHelper.quietClose(clusteredServiceContainer);
//    setActive(false);
//  }
//
//  record ClusteredMediaDriver(MediaDriver driver, Archive archive, ConsensusModule consensusModule)
//      implements AutoCloseable {
//    @Override
//    public void close() {
//      CloseHelper.closeAll(this.consensusModule, this.archive, this.driver);
//    }
//  }
//}
