package com.namnv;

import com.namnv.core.RecordingDescriptor;
import io.aeron.Aeron;
import io.aeron.CommonContext;
import io.aeron.archive.client.AeronArchive;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.MediaDriver.Context;
import java.nio.file.Paths;
import java.util.function.Predicate;
import org.agrona.collections.MutableReference;

public class RemoveOldLog {
  private static final String AERON_UDP_ENDPOINT = "aeron:udp?endpoint=";
  private static final String THIS_HOST = "127.0.0.1";
  private static final int ARCHIVE_CONTROL_PORT = 8001;
  private static final int ARCHIVE_EVENT_PORT = 8002;

  private static final int STREAM_ID = 100;

  public static void main(String[] args) {

    var aeronPath = Paths.get(CommonContext.generateRandomDirName());

    var mediaDriver =
        MediaDriver.launch(
            new Context().aeronDirectoryName(aeronPath.toString()).spiesSimulateConnection(true));

    var aeron = Aeron.connect(new Aeron.Context().aeronDirectoryName(aeronPath.toString()));

    var aeronArchive =
        AeronArchive.connect(
            new AeronArchive.Context()
                .aeron(aeron)
                .controlRequestChannel(
                    AERON_UDP_ENDPOINT + "localhost" + ":" + ARCHIVE_CONTROL_PORT)
                .recordingEventsChannel(AERON_UDP_ENDPOINT + "localhost" + ":" + ARCHIVE_EVENT_PORT)
                .controlResponseChannel(AERON_UDP_ENDPOINT + THIS_HOST + ":0"));

    var recordingLog = findLastRecording(aeronArchive, rd1 -> rd1.streamId == STREAM_ID);

    var newStartPosition =
        AeronArchive.segmentFileBasePosition(
            recordingLog.startPosition,
            184867968,
            recordingLog.termBufferLength,
            recordingLog.segmentFileLength);

    if (newStartPosition == 0) {
      return;
    }

    aeronArchive.truncateRecording(recordingLog.recordingId, newStartPosition);
  }

  public static RecordingDescriptor findLastRecording(
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
}
