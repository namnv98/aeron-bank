package com.namnv;

import com.namnv.services.infra.ClusterClientResponder;
import com.namnv.services.infra.ClusterClientResponderImpl;
import com.namnv.services.bank.OMSService;
import com.weareadaptive.sbe.MessageHeaderDecoder;
import io.aeron.ExclusivePublication;
import io.aeron.Image;
import io.aeron.cluster.codecs.CloseReason;
import io.aeron.cluster.service.ClientSession;
import io.aeron.cluster.service.Cluster;
import io.aeron.cluster.service.ClusteredService;
import io.aeron.logbuffer.Header;
import org.agrona.BitUtil;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class ClusterService implements ClusteredService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusteredService.class);
    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    private OMSService omsService;
    private final ClusterClientResponder clusterClientResponder = new ClusterClientResponderImpl();
    private int currentLeader = -1;


    @Override
    public void onStart(final Cluster cluster, final Image snapshotImage) {
        registerOMSService(cluster.idleStrategy());
        if (snapshotImage != null) {
            restoreSnapshot(snapshotImage);
        }
    }

    @Override
    public void onSessionOpen(final ClientSession session, final long timestamp) {
        LOGGER.info("Client ID: " + session.id() + " Connected");
    }

    @Override
    public void onSessionClose(final ClientSession session, final long timestamp, final CloseReason closeReason) {
        LOGGER.info("Client ID: " + session.id() + " Disconnected");
    }
  private final MutableDirectBuffer egressMessageBuffer = new ExpandableArrayBuffer();
  private final IdleStrategy idleStrategy = new BackoffIdleStrategy();
    @Override
    public void onSessionMessage(final ClientSession session, final long timestamp, final DirectBuffer buffer,
                                 final int offset, final int length,
                                 final Header header) {
      long correlationId = buffer.getLong(offset);

      egressMessageBuffer.putLong(0, correlationId);

      idleStrategy.reset();
      while (session.offer(egressMessageBuffer, 0, BitUtil.SIZE_OF_LONG) < 0) {
        idleStrategy.idle();
      }
    }

    @Override
    public void onTakeSnapshot(final ExclusivePublication snapshotPublication) {
        omsService.onTakeSnapshot(snapshotPublication);
    }

    public void restoreSnapshot(final Image snapshotImage) {
        omsService.onRestoreSnapshot(snapshotImage);
    }

    @Override
    public void onTimerEvent(final long correlationId, final long timestamp) {

    }

    @Override
    public void onRoleChange(final Cluster.Role newRole) {
        LOGGER.info("Cluster node is now: " + newRole);
    }

    @Override
    public void onNewLeadershipTermEvent(
            final long leadershipTermId,
            final long logPosition,
            final long timestamp,
            final long termBaseLogPosition,
            final int leaderMemberId,
            final int logSessionId,
            final TimeUnit timeUnit,
            final int appVersion) {
        LOGGER.info("Cluster node " + leaderMemberId + " is now Leader, previous Leader: " + currentLeader);
        currentLeader = leaderMemberId;
    }


    @Override
    public void onTerminate(final Cluster cluster) {
        LOGGER.info("Cluster node is terminating");
    }

    private void registerOMSService(IdleStrategy idleStrategy) {
        omsService = new OMSService(clusterClientResponder, idleStrategy);
    }

    public int getCurrentLeader() {
        return this.currentLeader;
    }
}
