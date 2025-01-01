package com.namnv.services.infra;

import com.namnv.services.bank.util.ExecutionResult;
import com.namnv.services.bank.util.Status;
import com.weareadaptive.sbe.ExecutionResultEncoder;
import com.weareadaptive.sbe.MessageHeaderEncoder;
import com.weareadaptive.sbe.SuccessMessageEncoder;
import com.weareadaptive.sbe.TransferResponseEncoder;
import io.aeron.Publication;
import io.aeron.cluster.service.ClientSession;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class ClusterClientResponderImpl implements ClusterClientResponder {
    private final Logger LOGGER = LoggerFactory.getLogger(ClusterClientResponderImpl.class);
    private final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder();
    private final ExecutionResultEncoder executionResultEncoder = new ExecutionResultEncoder();
    private final SuccessMessageEncoder successMessageEncoder = new SuccessMessageEncoder();
    private final TransferResponseEncoder transferEncoder = new TransferResponseEncoder();
    private IdleStrategy idleStrategy = new SleepingIdleStrategy();

    @Override
    public void onExecutionResult(ClientSession session, long correlationId, ExecutionResult executionResult) {
        int encodedLength = MessageHeaderEncoder.ENCODED_LENGTH + ExecutionResultEncoder.BLOCK_LENGTH;
        MutableDirectBuffer directBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(encodedLength));
        executionResultEncoder.wrapAndApplyHeader(directBuffer, 0, messageHeaderEncoder);

        messageHeaderEncoder.correlationId(correlationId);
        executionResultEncoder.status(executionResult.getStatus().getByte());
        sendMessageToSession(session, directBuffer, encodedLength);
    }

    @Override
    public void onSuccessMessage(ClientSession session, long correlationId) {
        final int encodedLength = MessageHeaderEncoder.ENCODED_LENGTH + SuccessMessageEncoder.BLOCK_LENGTH;
        MutableDirectBuffer directBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(encodedLength));

        successMessageEncoder.wrapAndApplyHeader(directBuffer, 0, messageHeaderEncoder);
        messageHeaderEncoder.correlationId(correlationId);
        successMessageEncoder.status(Status.SUCCESS.getByte());
        sendMessageToSession(session, directBuffer, encodedLength);
    }

    @Override
    public void onTransfer(ClientSession session, long correlationId, Status status) {
        final int encodedLength = MessageHeaderEncoder.ENCODED_LENGTH + TransferResponseEncoder.BLOCK_LENGTH;
        MutableDirectBuffer directBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(encodedLength));

        transferEncoder.wrapAndApplyHeader(directBuffer, 0, messageHeaderEncoder);
        messageHeaderEncoder.correlationId(correlationId);
        transferEncoder.status(Status.SUCCESS.getByte());
        sendMessageToSession(session, directBuffer, encodedLength);
    }


    @Override
    public void setIdleStrategy(IdleStrategy idleStrategy) {
        this.idleStrategy = idleStrategy;
    }

    public void sendMessageToSession(ClientSession session, DirectBuffer directBuffer, int encodedLength) {
        final int offset = 0;
        int retries = 0;
        int RETRY_COUNT = 3;
        do {
            idleStrategy.reset();
            final long result = session.offer(directBuffer, offset, encodedLength);
            if (result >= 0L) {
                return;
            } else if (result == Publication.ADMIN_ACTION || result == Publication.BACK_PRESSURED) {
                LOGGER.warn("backpressure or admin action on snapshot");
            } else if (result == Publication.NOT_CONNECTED || result == Publication.MAX_POSITION_EXCEEDED) {
                LOGGER.error("unexpected publication state on snapshot: {}", result);
                return;
            }
            idleStrategy.idle();
            retries += 1;
        }
        while (retries < RETRY_COUNT);

        LOGGER.error("failed to offer snapshot within {} retries", RETRY_COUNT);
    }
}
