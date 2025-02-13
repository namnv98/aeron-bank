package com.namnv.client;

import com.weareadaptive.sbe.ExecutionResultDecoder;
import com.weareadaptive.sbe.MessageHeaderDecoder;
import com.weareadaptive.sbe.SuccessMessageDecoder;
import com.weareadaptive.sbe.TransferResponseDecoder;
import io.aeron.cluster.client.EgressListener;
import io.aeron.cluster.codecs.EventCode;
import io.aeron.logbuffer.Header;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.agrona.DirectBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientEgressListener implements EgressListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientEgressListener.class);
    private int currentLeader = -1;
    private final MessageHeaderDecoder messageHeaderDecoder = new MessageHeaderDecoder();
    private final Map<Long, HttpServerRequest> allHttopRequest = new ConcurrentHashMap<>();
    private final BinaryJsonCodec binaryJsonCodec = new BinaryJsonCodec();

    @Override
    public void onMessage(final long clusterSessionId, final long timestamp, final DirectBuffer buffer,
                          final int offset, final int length,
                          final Header header) {
        int bufferOffset = offset;
        messageHeaderDecoder.wrap(buffer, bufferOffset);
        final int typeOfMessage = messageHeaderDecoder.templateId();
        final long correlationId = messageHeaderDecoder.correlationId();
//        LOGGER.info(String.valueOf(correlationId));
        if (allHttopRequest.containsKey(correlationId)) {
            final int actingBlockLength = messageHeaderDecoder.blockLength();
            final int actingVersion = messageHeaderDecoder.version();
            bufferOffset += messageHeaderDecoder.encodedLength();

            if (typeOfMessage == TransferResponseDecoder.TEMPLATE_ID) {
                //TODO:
//                binaryJsonCodec.addOrderToCollectionOfAllOrders(correlationId, buffer, bufferOffset, actingBlockLength, actingVersion);
                sendMessage(buffer, bufferOffset, typeOfMessage, correlationId, actingBlockLength, actingVersion);

            } else {
                sendMessage(buffer, bufferOffset, typeOfMessage, correlationId, actingBlockLength, actingVersion);
            }
        } else {
            LOGGER.error("This ID is not in use: ".concat(String.valueOf(correlationId)));
        }
    }

    private void sendMessage(final DirectBuffer buffer, final int bufferOffset, final int typeOfMessage, final long correlationId,
                             final int actingBlockLength, final int actingVersion) {
        JsonObject jsonObject = switch (typeOfMessage) {
            case SuccessMessageDecoder.TEMPLATE_ID ->
                    binaryJsonCodec.getSuccessMessageAsJson(buffer, bufferOffset, actingBlockLength, actingVersion);
            case ExecutionResultDecoder.TEMPLATE_ID ->
                    binaryJsonCodec.getExecutionResultAsJson(buffer, bufferOffset, actingBlockLength, actingVersion);
            case TransferResponseDecoder.TEMPLATE_ID ->
                    binaryJsonCodec.getOrderIdResponse(buffer, bufferOffset, actingBlockLength, actingVersion);
            default -> throw new RuntimeException("method not supported");
        };
        allHttopRequest.get(correlationId).response().end(jsonObject.toBuffer());
        allHttopRequest.remove(correlationId);
    }

    @Override
    public void onSessionEvent(final long correlationId, final long clusterSessionId, final long leadershipTermId,
                               final int leaderMemberId,
                               final EventCode code, final String detail) {
        EgressListener.super.onSessionEvent(correlationId, clusterSessionId, leadershipTermId, leaderMemberId, code,
                detail);
    }

    @Override
    public void onNewLeader(final long clusterSessionId, final long leadershipTermId, final int leaderMemberId,
                            final String ingressEndpoints) {
        LOGGER.info("Cluster node " + leaderMemberId + " is now Leader, previous Leader: " + currentLeader);
        currentLeader = leaderMemberId;
    }

    public int getCurrentLeader() {
        return this.currentLeader;
    }

    public void addHttpRequest(final long id, final HttpServerRequest ws) {
        allHttopRequest.put(id, ws);
    }
}
