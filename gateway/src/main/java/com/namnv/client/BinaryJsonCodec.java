package com.namnv.client;


import com.namnv.services.oms.util.ExecutionResult;
import com.namnv.services.oms.util.Status;
import com.weareadaptive.sbe.ExecutionResultDecoder;
import com.weareadaptive.sbe.SuccessMessageDecoder;
import com.weareadaptive.sbe.TransferResponseDecoder;
import io.vertx.core.json.JsonObject;
import org.agrona.DirectBuffer;

public class BinaryJsonCodec {
    final ExecutionResult executionResult = new ExecutionResult();
    private final ExecutionResultDecoder executionResultDecoder = new ExecutionResultDecoder();
    private final SuccessMessageDecoder successMessageDecoder = new SuccessMessageDecoder();
    private final TransferResponseDecoder transferResponseDecoder = new TransferResponseDecoder();

    protected JsonObject getOrderIdResponse(final DirectBuffer buffer, final int offset, final int actingBlockLength, final int actingVersion) {
        transferResponseDecoder.wrap(buffer, offset, actingBlockLength, actingVersion);
        final Status status = Status.fromByteValue((byte) transferResponseDecoder.status());
        return JsonObject.of("status", status.name());
    }

    protected JsonObject getExecutionResultAsJson(final DirectBuffer buffer, final int offset, final int actingBlockLength, final int actingVersion) {
        executionResultDecoder.wrap(buffer, offset, actingBlockLength, actingVersion);
        final long orderId = 0;
        final Status status = Status.fromByteValue((byte) executionResultDecoder.status());
        executionResult.setStatus(status);
        executionResult.setOrderId(orderId);
        return JsonObject.mapFrom(executionResult);
    }

    protected JsonObject getSuccessMessageAsJson(final DirectBuffer buffer, final int offset, final int actingBlockLength, final int actingVersion) {
        successMessageDecoder.wrap(buffer, offset, actingBlockLength, actingVersion);
        final Status status = Status.fromByteValue((byte) successMessageDecoder.status());
        return JsonObject.of("status", status.name());
    }
}
