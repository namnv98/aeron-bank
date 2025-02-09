package com.namnv;

import static io.aeron.cluster.ConsensusModule.Configuration.LOG_CHANNEL_PROP_NAME;
import static io.aeron.driver.ThreadingMode.DEDICATED;
import static java.lang.System.setProperty;

import io.aeron.archive.Archive;
import io.aeron.cluster.ConsensusModule;
import io.aeron.cluster.service.ClusteredServiceContainer;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.status.SystemCounterDescriptor;
import io.aeron.samples.cluster.ClusterConfig;
import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Setter;
import org.agrona.CloseHelper;
import org.agrona.ErrorHandler;
import org.agrona.concurrent.ShutdownSignalBarrier;
import org.agrona.concurrent.status.AtomicCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Setter
@AllArgsConstructor
public class ClusterNode1 implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterNode1.class);

  private ClusteredServiceContainer clusteredServiceContainer;
  ClusteredMediaDriver clusteredMediaDriver;
  final ClusterService clusterService;
  File clusterDir;

  ShutdownSignalBarrier barrier = new ShutdownSignalBarrier();

  public ClusterNode1(ClusterService clusterService) {
    this.clusterService = clusterService;
  }

  public void startNode(int node) {
    startNode(node, 0);
  }

  public void startNode(int node, long initLogPosition) {
    var dirBase = System.getProperty("user.dir") + "/aeron-cluster/node-" + node + "/";

    setProperty(LOG_CHANNEL_PROP_NAME, "aeron:udp?term-length=512k");
    setProperty("aeron.dir", dirBase);

    var baseDir = new File(dirBase);

    barrier = new ShutdownSignalBarrier();

    var archive =
        new Archive.Context()
            .archiveDir(new File(baseDir, "archive"))
            .controlChannel("aeron:udp?endpoint=127.0.0.1:8001")
            .localControlChannel("aeron:ipc")
            .recordingEventsChannel("aeron:udp?endpoint=127.0.0.1:8002")
            .recordingEventsStreamId(1009)
            .replicationChannel("aeron:udp?endpoint=127.0.0.1:8006")
            .segmentFileLength(16 * 1024 * 1024)
            .recordingEventsEnabled(true);

    List<String> hostAddresses = List.of("localhost");

    var clusterConfig =
        ClusterConfig.create(node, hostAddresses, hostAddresses, 8000, clusterService);

    clusterConfig.consensusModuleContext().ingressChannel("aeron:udp");

    clusterDir = new File(baseDir, "cluster");
    clusterConfig.clusteredServiceContext().clusterDir(clusterDir);
    clusterConfig.consensusModuleContext().clusterDir(clusterDir);

    clusterConfig
        .consensusModuleContext()
        .ingressChannel("aeron:udp?endpoint=localhost:8007|term-length=64k");
    clusterConfig.consensusModuleContext().logStreamId(100);
    clusterConfig.consensusModuleContext().deleteDirOnStart(false);
    clusterConfig.consensusModuleContext().sessionTimeoutNs(TimeUnit.MINUTES.toNanos(10));
    clusterConfig.consensusModuleContext().leaderHeartbeatTimeoutNs(TimeUnit.SECONDS.toNanos(1));

    clusterConfig.mediaDriverContext().threadingMode(DEDICATED);
    clusterConfig.consensusModuleContext().leaderHeartbeatTimeoutNs(TimeUnit.SECONDS.toNanos(1));

    try (ClusteredMediaDriver clusteredMediaDriver =
            launch(
                clusterConfig.mediaDriverContext(),
                archive,
                clusterConfig.consensusModuleContext(),
                initLogPosition);
        ClusteredServiceContainer clusteredServiceContainer =
            ClusteredServiceContainer.launch(clusterConfig.clusteredServiceContext()); ) {

      this.clusteredServiceContainer = clusteredServiceContainer;
      this.clusteredMediaDriver = clusteredMediaDriver;
      LOGGER.info("Started Cluster Node...");

      barrier.await();

      close();
      LOGGER.info("Exiting");
    }
  }

  public ClusteredMediaDriver launch(
      final MediaDriver.Context driverCtx,
      final Archive.Context archiveCtx,
      final ConsensusModule.Context consensusModuleCtx,
      long initLogPosition) {

    Archive archive = null;
    ConsensusModule consensusModule = null;
    MediaDriver driver = null;
    try {
      driver = MediaDriver.launch(driverCtx);

      final int errorCounterId = SystemCounterDescriptor.ERRORS.id();
      final AtomicCounter errorCounter =
          null != archiveCtx.errorCounter()
              ? archiveCtx.errorCounter()
              : new AtomicCounter(driverCtx.countersValuesBuffer(), errorCounterId);
      final ErrorHandler errorHandler =
          null != archiveCtx.errorHandler() ? archiveCtx.errorHandler() : driverCtx.errorHandler();

      archive =
          Archive.launch(
              archiveCtx
                  .mediaDriverAgentInvoker(driver.sharedAgentInvoker())
                  .aeronDirectoryName(driver.aeronDirectoryName())
                  .errorHandler(errorHandler)
                  .errorCounter(errorCounter));

      consensusModuleCtx.initLogPosition(initLogPosition);

      consensusModule =
          ConsensusModule.launch(
              consensusModuleCtx.aeronDirectoryName(driverCtx.aeronDirectoryName()));

      return new ClusteredMediaDriver(driver, archive, consensusModule);
    } catch (Exception ex) {
      CloseHelper.quietCloseAll(consensusModule, archive, driver);
      throw ex;
    }
  }

  @Override
  public void close() {
    CloseHelper.quietClose(clusteredMediaDriver);
    CloseHelper.quietClose(clusteredServiceContainer);
  }

  record ClusteredMediaDriver(MediaDriver driver, Archive archive, ConsensusModule consensusModule)
      implements AutoCloseable {
    @Override
    public void close() {
      CloseHelper.closeAll(this.consensusModule, this.archive, this.driver);
    }
  }
}
