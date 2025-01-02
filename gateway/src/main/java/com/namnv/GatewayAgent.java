package com.namnv;

import com.namnv.client.ClientEgressListener;
import com.namnv.client.ClientIngressSender;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.vertx.core.Vertx;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.SystemEpochClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static com.namnv.util.ConfigUtils.egressChannel;
import static com.namnv.util.ConfigUtils.ingressEndpoints;


public class GatewayAgent implements Agent {
    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayAgent.class);
    private static ClientEgressListener clientEgressListener;
    private static ClientIngressSender clientIngressSender;
    private AeronCluster aeronCluster;
    private MediaDriver mediaDriver;
    private static Vertx vertx;
    private int initLeader = -1;
    private long lastHeartbeatTime = Long.MIN_VALUE;
    //TODO:
    private static final long HEARTBEAT_INTERVAL = 2500000;
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
        LOGGER.info("Starting com.namnv.Gateway...");
        clientEgressListener = new ClientEgressListener();

        mediaDriver = MediaDriver.launch(new MediaDriver.Context()
                .threadingMode(ThreadingMode.DEDICATED)
                .dirDeleteOnStart(true)
                .dirDeleteOnShutdown(true));

        aeronCluster = AeronCluster.connect(
                new AeronCluster.Context()
                        .egressListener(clientEgressListener)
                        .egressChannel(egressChannel())
                        .aeronDirectoryName(mediaDriver.aeronDirectoryName())
                        .ingressChannel("aeron:udp")
                        .ingressEndpoints(ingressEndpoints(maxNodes))
                        .messageTimeoutNs(TimeUnit.SECONDS.toNanos(5))
                        .errorHandler(throwable -> {
                            LOGGER.error(throwable.getMessage());
                        }));

        LOGGER.info("Connected to cluster leader, node " + aeronCluster.leaderMemberId());

        clientIngressSender = new ClientIngressSender(aeronCluster);
        LOGGER.info("Aeron Client started...");
        startHttpServer();

        isActive = true;
        initLeader = aeronCluster.leaderMemberId();
    }

    public int getLeaderId() {
        return clientEgressListener.getCurrentLeader() == -1 ? initLeader : clientEgressListener.getCurrentLeader();
    }

    @Override
    public int doWork() {
        final long now = SystemEpochClock.INSTANCE.time();
        if (now >= (lastHeartbeatTime + HEARTBEAT_INTERVAL)) {
            lastHeartbeatTime = now;
            if (isActive) {
                aeronCluster.sendKeepAlive();
            }
        }
        if (null != aeronCluster && !aeronCluster.isClosed()) {
            return aeronCluster.pollEgress();
        }
        return 0;
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
