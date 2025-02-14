package com.namnv.node1;

import static com.namnv.util.ConfigUtils.*;
import static io.aeron.driver.ThreadingMode.DEDICATED;
import static java.lang.System.setProperty;

import com.namnv.ClusterService;
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
import org.agrona.concurrent.ShutdownSignalBarrier;
import org.agrona.concurrent.status.AtomicCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Setter
@AllArgsConstructor
public class ClusterNode1 implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterNode1.class);

  private ClusteredServiceContainer clusteredServiceContainer;
  private ClusteredMediaDriver clusteredMediaDriver;
  private ClusterService clusterService;

  public ClusterNode1(ClusterService clusterService) {
    this.clusterService = clusterService;
  }

  public void startNode(int node, int maxNodes, long initLogPosition) {

    final int portBase = getBasePort();
    final String hosts =
        maxNodes == 1 ? getClusterAddresses() : getMultiNodeClusterAddresses(maxNodes);
    final List<String> hostAddresses = List.of(hosts.split(","));

    var barrier = new ShutdownSignalBarrier();

    var dirBase = System.getProperty("user.dir") + "/aeron-cluster/node-" + node + "/";
    setProperty("aeron.dir", dirBase);

    var baseDir = new File(dirBase);

    var clusterConfig =
        createClusterConfig(node, baseDir, portBase, hostAddresses, initLogPosition);

    try (ClusteredMediaDriver clusteredMediaDriver =
            launch(clusterConfig, clusterConfig.archiveContext());
        ClusteredServiceContainer clusteredServiceContainer =
            ClusteredServiceContainer.launch(clusterConfig.clusteredServiceContext())) {

      this.clusteredServiceContainer = clusteredServiceContainer;
      this.clusteredMediaDriver = clusteredMediaDriver;
      LOGGER.info("Started Cluster Node...");

      barrier.await();

      close();
      LOGGER.info("Exiting");
    } catch (Exception e) {
      LOGGER.error("Error during node startup", e);
    }
  }

  private ClusterConfig createClusterConfig(
      int node, File baseDir, int portBase, List<String> hostAddresses, long initLogPosition) {
    var clusterConfig =
        ClusterConfig.create(node, hostAddresses, hostAddresses, portBase, clusterService);
    var clusterDir = new File(baseDir, "cluster");

    clusterConfig
        .consensusModuleContext()
        .ingressChannel("aeron:udp")
        .logStreamId(100)
        .deleteDirOnStart(false)
        .sessionTimeoutNs(TimeUnit.MINUTES.toNanos(10))
        .leaderHeartbeatTimeoutNs(TimeUnit.SECONDS.toNanos(1))
        .initLogPosition(initLogPosition)
        .clusterDir(clusterDir);

    clusterConfig
        .archiveContext()
        .archiveDir(new File(baseDir, "archive"))
        .controlChannel("aeron:udp?endpoint=127.0.0.1:9001")
        .localControlChannel("aeron:ipc")
        .recordingEventsChannel("aeron:udp?endpoint=127.0.0.1:9002")
        .recordingEventsStreamId(1009)
        .replicationChannel("aeron:udp?endpoint=127.0.0.1:9006")
        .segmentFileLength(16 * 1024 * 1024)
        .recordingEventsEnabled(true);

    clusterConfig.mediaDriverContext().threadingMode(DEDICATED);
    clusterConfig.clusteredServiceContext().clusterDir(clusterDir);

    return clusterConfig;
  }

  private ClusteredMediaDriver launch(ClusterConfig clusterConfig, Archive.Context archiveContext) {
    MediaDriver.Context driverCtx = clusterConfig.mediaDriverContext();
    Archive archive = null;
    ConsensusModule consensusModule = null;
    MediaDriver driver = null;

    try {
      driver = MediaDriver.launch(driverCtx);
      var errorCounter = createErrorCounter(archiveContext, driverCtx);
      var errorHandler = driverCtx.errorHandler();

      archive =
          Archive.launch(
              archiveContext
                  .mediaDriverAgentInvoker(driver.sharedAgentInvoker())
                  .aeronDirectoryName(driver.aeronDirectoryName())
                  .errorHandler(errorHandler)
                  .errorCounter(errorCounter));

      consensusModule =
          ConsensusModule.launch(
              clusterConfig
                  .consensusModuleContext()
                  .aeronDirectoryName(driverCtx.aeronDirectoryName()));

      return new ClusteredMediaDriver(driver, archive, consensusModule);
    } catch (Exception ex) {
      CloseHelper.quietCloseAll(consensusModule, archive, driver);
      throw ex;
    }
  }

  private AtomicCounter createErrorCounter(
      Archive.Context archiveContext, MediaDriver.Context driverCtx) {
    int errorCounterId = SystemCounterDescriptor.ERRORS.id();
    return archiveContext.errorCounter() != null
        ? archiveContext.errorCounter()
        : new AtomicCounter(driverCtx.countersValuesBuffer(), errorCounterId);
  }

  @Override
  public void close() {
    CloseHelper.closeAll(clusteredMediaDriver, clusteredServiceContainer);
  }

  record ClusteredMediaDriver(MediaDriver driver, Archive archive, ConsensusModule consensusModule)
      implements AutoCloseable {
    @Override
    public void close() {
      CloseHelper.closeAll(this.consensusModule, this.archive, this.driver);
    }
  }
}
