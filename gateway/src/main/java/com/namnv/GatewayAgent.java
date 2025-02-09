package com.namnv;

import static com.namnv.util.ConfigUtils.ingressEndpoints;
import static java.lang.System.setProperty;

import com.namnv.client.ClientEgressListener;
import com.namnv.client.ClientIngressSender;
import com.namnv.util.ConfigUtils;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.vertx.core.Vertx;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.SystemEpochClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GatewayAgent implements Agent {
  private static final Logger LOGGER = LoggerFactory.getLogger(GatewayAgent.class);
  private static ClientEgressListener clientEgressListener;
  private static ClientIngressSender clientIngressSender;
  private AeronCluster aeronCluster;
  private MediaDriver mediaDriver;
  private static Vertx vertx;
  private int initLeader = -1;
  private long lastHeartbeatTime = Long.MIN_VALUE;
  // TODO:
  private static boolean isActive;

  public GatewayAgent(final int maxNodes) {
    startGateway(maxNodes);
  }

  private static void startHttpServer() {
    vertx = Vertx.vertx();
    HttpServer httpServer = new HttpServer(clientIngressSender, clientEgressListener);
    vertx.deployVerticle(httpServer);
    LOGGER.info("HttpServer started...");
  }

  public void startGateway(final int maxNodes) {
    setProperty("aeron.dir", System.getProperty("user.dir") + "/client");

    //        setProperty("aeron.socket.so_sndbuf", "8388608");
    //        setProperty("aeron.socket.so_rcvbuf", "8388608");
    //
    //        setProperty(DISABLE_BOUNDS_CHECKS_PROP_NAME, "true");
    //        setProperty("aeron.mtu.length", "16384");
    //        setProperty("aeron.socket.so_sndbuf", "2097152");
    //        setProperty("aeron.socket.so_rcvbuf", "2097152");
    //        setProperty("aeron.rcv.initial.window.length", "2097152");

    clientEgressListener = new ClientEgressListener();

    mediaDriver =
        MediaDriver.launch(
            new MediaDriver.Context()
                .threadingMode(ThreadingMode.DEDICATED)
                .termBufferSparseFile(false)
                .conductorIdleStrategy(new BusySpinIdleStrategy())
                .receiverIdleStrategy(new BusySpinIdleStrategy())
                .senderIdleStrategy(new BusySpinIdleStrategy())
                .sharedNetworkIdleStrategy(new BusySpinIdleStrategy())
                .errorHandler(
                    throwable -> {
                      throwable.printStackTrace();
                    })
                .dirDeleteOnStart(true)
                .dirDeleteOnShutdown(true));

    aeronCluster =
        AeronCluster.connect(
            new AeronCluster.Context()
                .egressListener(clientEgressListener)
                .ingressChannel("aeron:udp")
                .egressChannel(ConfigUtils.egressChannel())
                .aeronDirectoryName(mediaDriver.aeronDirectoryName())
                .ingressEndpoints(ingressEndpoints(maxNodes))
                .messageTimeoutNs(TimeUnit.SECONDS.toNanos(5))
                .errorHandler(throwable -> LOGGER.error(throwable.getMessage())));

    LOGGER.info("Connected to cluster leader, node " + aeronCluster.leaderMemberId());

    clientIngressSender = new ClientIngressSender(aeronCluster);
    LOGGER.info("Aeron Client started...");
    startHttpServer();

    isActive = true;
    initLeader = aeronCluster.leaderMemberId();
  }

  public int getLeaderId() {
    return clientEgressListener.getCurrentLeader() == -1
        ? initLeader
        : clientEgressListener.getCurrentLeader();
  }

  private static final long CLIENT_SESSION_TIMEOUT = 10; // 10 giây
  private static final long HEARTBEAT_INTERVAL = CLIENT_SESSION_TIMEOUT / 4;

  @Override
  public int doWork() {
    //        sendHeartBeat();
    if (null != aeronCluster && !aeronCluster.isClosed()) {
      return aeronCluster.pollEgress();
    }
    return 0;
  }

  private void sendHeartBeat() {
    final long now = SystemEpochClock.INSTANCE.time();
    if (now >= (lastHeartbeatTime + CLIENT_SESSION_TIMEOUT * 1000)) {
      lastHeartbeatTime = now;
      try {
        System.out.println(LocalDateTime.now());
        aeronCluster.sendKeepAlive();
      } catch (Exception exception) {
        exception.printStackTrace();
      }
    }
  }

  @Override
  public void onClose() {
    if (aeronCluster != null) {
      aeronCluster.close();
    }
    if (mediaDriver != null) {
      mediaDriver.close();
    }
    if (vertx != null) {
      vertx.close();
    }
    isActive = false;
    LOGGER.info("Closed gateway...");
  }

  @Override
  public String roleName() {
    return "cluster";
  }
}
