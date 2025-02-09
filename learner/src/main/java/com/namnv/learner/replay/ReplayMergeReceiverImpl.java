package com.namnv.learner.replay;

import static io.aeron.CommonContext.MDC_CONTROL_MODE_MANUAL;
import static io.aeron.CommonContext.UDP_MEDIA;

import io.aeron.cluster.codecs.MessageHeaderDecoder;
import io.aeron.*;
import io.aeron.archive.client.AeronArchive;
import io.aeron.archive.client.ReplayMerge;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.cluster.codecs.SessionMessageHeaderDecoder;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.agrona.DirectBuffer;
import org.agrona.collections.MutableReference;
import org.agrona.concurrent.SleepingMillisIdleStrategy;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReplayMergeReceiverImpl implements ReplayMergeReceiver {
  private static final ChannelUriStringBuilder URI_BUILDER =
      new ChannelUriStringBuilder().media(UDP_MEDIA);
  private static final AtomicBoolean RUNNING = new AtomicBoolean(true);

  private static final SessionMessageHeaderDecoder SESSION_MESSAGE_HEADER_DECODER =
      new SessionMessageHeaderDecoder();
  private static final MessageHeaderDecoder MESSAGE_HEADER_DECODER = new MessageHeaderDecoder();

  private final MessageSubscriberManager messageSubscriberManager = new MessageSubscriberManager();
  private final SleepingMillisIdleStrategy idleStrategy = new SleepingMillisIdleStrategy(100);

  private Aeron aeron;
  private AeronArchive aeronArchive;
  private int streamId;
  private String thisHost;
  private String liveHost;
  private int livePort;
  private int fragmentLimit;

  @Override
  public void start(long lastPosition) {
    var lastRecording = findLastRecording(aeronArchive, rd1 -> rd1.streamId == streamId);

    if (lastRecording == null) {
      throw new NoSuchElementException("Recording not found");
    }

    var subscription =
        aeron.addSubscription(
            new ChannelUriStringBuilder()
                .media(UDP_MEDIA)
                .controlMode(MDC_CONTROL_MODE_MANUAL)
                .build(),
            streamId);

    var replayChannel =
        new ChannelUriStringBuilder().media(UDP_MEDIA).sessionId(lastRecording.sessionId).build();

    var replayDestination =
        new ChannelUriStringBuilder().media(UDP_MEDIA).endpoint(thisHost + ":0").build();

    var liveDestination =
        new ChannelUriStringBuilder()
            .media(UDP_MEDIA)
            .controlEndpoint("localhost" + ":" + livePort)
            .endpoint(thisHost + ":0")
            .build();

    var replayMerge =
        new ReplayMerge(
            subscription,
            aeronArchive,
            replayChannel,
            replayDestination,
            liveDestination,
            lastRecording.recordingId,
            lastPosition);

    FragmentHandler fragmentHandler = this::handleMessage;
    FragmentAssembler fragmentAssembler = new FragmentAssembler(fragmentHandler);

    while (RUNNING.get()) {
      int progress = pollReplayMerge(replayMerge, fragmentHandler, fragmentAssembler);
      //      System.out.println(progress);
      idleStrategy.idle(progress);
    }
    System.out.println("Shutting down...");
    close();
  }

  private int pollReplayMerge(
      ReplayMerge replayMerge,
      FragmentHandler fragmentHandler,
      FragmentAssembler fragmentAssembler) {

    int progress;
    if (replayMerge.isMerged()) {
      final Image image = replayMerge.image();
      progress = image.poll(fragmentAssembler, fragmentLimit);
      if (image.isClosed()) {
        System.err.println("### replayMerge.image is closed, exiting");
        throw new RuntimeException("good bye");
      }
    } else {
      try {
        progress = replayMerge.poll(fragmentHandler, fragmentLimit);
      } catch (io.aeron.exceptions.TimeoutException exception) {
        progress = 0;
      }
      if (replayMerge.hasFailed()) {
        //        System.err.println("### replayMerge has failed, exiting");
        progress = 0;
      }
    }
    return progress;
  }

  private void handleMessage(DirectBuffer buffer, int offset, int length, Header header) {
    MESSAGE_HEADER_DECODER.wrap(buffer, offset);

    int schemaId = MESSAGE_HEADER_DECODER.schemaId();
    if (schemaId != MessageHeaderDecoder.SCHEMA_ID) {
      return;
    }

    int templateId = MESSAGE_HEADER_DECODER.templateId();
    if (templateId == SessionMessageHeaderDecoder.TEMPLATE_ID) {

      SESSION_MESSAGE_HEADER_DECODER.wrap(
          buffer,
          offset + MessageHeaderDecoder.ENCODED_LENGTH,
          MESSAGE_HEADER_DECODER.blockLength(),
          MESSAGE_HEADER_DECODER.version());

      messageSubscriberManager.handleMessage(
          SESSION_MESSAGE_HEADER_DECODER.clusterSessionId(),
          SESSION_MESSAGE_HEADER_DECODER.timestamp(),
          buffer,
          offset + AeronCluster.SESSION_HEADER_LENGTH,
          length - AeronCluster.SESSION_HEADER_LENGTH,
          header);
    }
  }

  public RecordingDescriptor findLastRecording(
      AeronArchive aeronArchive, Predicate<RecordingDescriptor> predicate) {
    MutableReference<RecordingDescriptor> result = new MutableReference<>();
    aeronArchive.listRecordings(
        0,
        Integer.MAX_VALUE,
        (controlSessionId,
            correlationId,
            recordingId,
            startTimestamp,
            stopTimestamp,
            startPosition,
            stopPosition,
            initialTermId,
            segmentFileLength,
            termBufferLength,
            mtuLength,
            sessionId,
            streamId1,
            strippedChannel,
            originalChannel,
            sourceIdentity) -> {
          RecordingDescriptor value =
              new RecordingDescriptor(
                  controlSessionId,
                  correlationId,
                  recordingId,
                  startTimestamp,
                  stopTimestamp,
                  startPosition,
                  stopPosition,
                  initialTermId,
                  segmentFileLength,
                  termBufferLength,
                  mtuLength,
                  sessionId,
                  streamId1,
                  strippedChannel,
                  originalChannel,
                  sourceIdentity);
          if (predicate.test(value)) {
            result.set(value);
          }
        });

    return result.get();
  }

  @Override
  public void addSubscriber(MessageSubscriber subscriber) {
    messageSubscriberManager.addSubscriber(subscriber);
  }

  static void close() {
    RUNNING.set(false);
  }
}
