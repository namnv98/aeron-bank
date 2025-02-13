package com.namnv;

import com.namnv.command.core.BaseCommand;
import com.namnv.command.core.CommandLog;
import com.namnv.command.core.TransferCommand;
import com.namnv.command.disruptor.event.RequestEvent;
import com.namnv.command.disruptor.event.RequestEventDispatcher;
import com.namnv.util.Status;
import com.weareadaptive.sbe.MessageHeaderDecoder;
import com.weareadaptive.sbe.MessageHeaderEncoder;
import com.weareadaptive.sbe.SuccessMessageEncoder;
import com.weareadaptive.sbe.TransferRequestDecoder;
import io.aeron.ExclusivePublication;
import io.aeron.Image;
import io.aeron.Publication;
import io.aeron.cluster.codecs.CloseReason;
import io.aeron.cluster.service.ClientSession;
import io.aeron.cluster.service.Cluster;
import io.aeron.cluster.service.ClusteredService;
import io.aeron.logbuffer.Header;
import lombok.Getter;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ClusterService implements ClusteredService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusteredService.class);
  private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
  @Getter private int currentLeader = -1;
  final RequestEventDispatcher requestEventDispatcher;

  public ClusterService(RequestEventDispatcher requestEventDispatcher) {

    this.requestEventDispatcher = requestEventDispatcher;
  }

  @Override
  public void onStart(final Cluster cluster, final Image snapshotImage) {}

  @Override
  public void onSessionOpen(final ClientSession session, final long timestamp) {
    LOGGER.info("Client ID: " + session.id() + " Connected");
  }

  @Override
  public void onSessionClose(
      final ClientSession session, final long timestamp, final CloseReason closeReason) {
    LOGGER.info("Client ID: " + session.id() + " Disconnected");
  }

  private final TransferRequestDecoder transferRequestDecoder = new TransferRequestDecoder();

  @Override
  public void onSessionMessage(
      ClientSession session,
      final long timestamp,
      final DirectBuffer buffer,
      final int offset,
      final int length,
      final Header header) {
    int bufferOffset = offset;
    headerDecoder.wrap(buffer, bufferOffset);
    final int schemaId = headerDecoder.schemaId();
    final int templateId = headerDecoder.templateId();

    if (schemaId != MessageHeaderDecoder.SCHEMA_ID) {
      LOGGER.error("Bad service name");
      return;
    }

    final int actingBlockLength = headerDecoder.blockLength();
    final int actingVersion = headerDecoder.version();

    bufferOffset += headerDecoder.encodedLength();
    final var correlationId = headerDecoder.correlationId();
    System.out.println(correlationId);

    transferRequestDecoder.wrap(buffer, bufferOffset, actingBlockLength, actingVersion);
    final double fromId = transferRequestDecoder.fromId();
    final double toId = transferRequestDecoder.toId();
    final double amount = transferRequestDecoder.amount();
//
//    CompletableFuture<RequestEvent> responseFuture = new CompletableFuture<>();
//    responseFuture.thenAccept(
//        event -> {
          final int encodedLength =
              MessageHeaderEncoder.ENCODED_LENGTH + SuccessMessageEncoder.BLOCK_LENGTH;
          MutableDirectBuffer directBuffer =
              new UnsafeBuffer(ByteBuffer.allocateDirect(encodedLength));

          successMessageEncoder.wrapAndApplyHeader(directBuffer, 0, messageHeaderEncoder);
          messageHeaderEncoder.correlationId(correlationId);
          successMessageEncoder.status(Status.SUCCESS.getByte());
          sendMessageToSession(session, directBuffer, encodedLength);
//        });

//    requestEventDispatcher.dispatch(
//        new RequestEvent(
//            responseFuture,
//            correlationId,
//            new BaseCommand(new CommandLog(4, new TransferCommand(fromId, toId, amount)))));
  }

  private final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder();
  private final SuccessMessageEncoder successMessageEncoder = new SuccessMessageEncoder();
  private final IdleStrategy idleStrategy = new SleepingIdleStrategy();

  public void sendMessageToSession(
      ClientSession session, DirectBuffer directBuffer, int encodedLength) {
    final int offset = 0;
    int retries = 0;
    int RETRY_COUNT = 3;
    do {
      idleStrategy.reset();
      final long result = session.offer(directBuffer, offset, encodedLength);
      if (result >= 0L) {
        return;
      } else if (result == Publication.ADMIN_ACTION) {
        LOGGER.warn("admin action on snapshot");
      } else if (result == Publication.BACK_PRESSURED) {
        LOGGER.warn("backpressure");
      } else if (result == Publication.NOT_CONNECTED
          || result == Publication.MAX_POSITION_EXCEEDED) {
        LOGGER.error("unexpected publication state on snapshot: {}", result);
        return;
      }
      idleStrategy.idle();
      retries += 1;
    } while (retries < RETRY_COUNT);

    LOGGER.error("failed to offer snapshot within {} retries", RETRY_COUNT);
  }

  @Override
  public void onTakeSnapshot(final ExclusivePublication snapshotPublication) {}

  @Override
  public void onTimerEvent(final long correlationId, final long timestamp) {}

  Cluster.Role role;

  @Override
  public void onRoleChange(final Cluster.Role newRole) {
    LOGGER.info("Cluster node is now: " + newRole);
    role = newRole;
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
    LOGGER.info(
        "Cluster node " + leaderMemberId + " is now Leader, previous Leader: " + currentLeader);
    currentLeader = leaderMemberId;
  }

  @Override
  public void onTerminate(final Cluster cluster) {
    LOGGER.info("Cluster node is terminating");
  }
}
