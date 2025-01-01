package com.namnv.client;

import com.namnv.services.oms.util.Method;
import com.weareadaptive.sbe.MessageHeaderEncoder;
import com.weareadaptive.sbe.TransferRequestEncoder;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

public class Encoder {
    final private MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    protected final int ORDER_REQUEST_LENGTH = MessageHeaderEncoder.ENCODED_LENGTH + TransferRequestEncoder.BLOCK_LENGTH;
    protected final int HEADER_LENGTH = MessageHeaderEncoder.ENCODED_LENGTH;

    final private TransferRequestEncoder transferRequestEncoder = new TransferRequestEncoder();

    private void setHeaderEncoder(final MutableDirectBuffer buffer, final long correlationId) {
        headerEncoder.wrap(buffer, 0);
        headerEncoder.correlationId(correlationId);
    }


    protected MutableDirectBuffer encodeOrderRequest(long correlationId, long fromId, double toId, double amount) {
        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(ORDER_REQUEST_LENGTH);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        setHeaderEncoder(directBuffer, correlationId);
        transferRequestEncoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        transferRequestEncoder.fromId(fromId);
        transferRequestEncoder.toId(toId);
        transferRequestEncoder.amount(amount);

        return directBuffer;
    }

    public MutableDirectBuffer encodeHeaderMessage(long correlationId, Method method) {
        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(HEADER_LENGTH);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        setHeaderEncoder(directBuffer, correlationId);

        switch (method) {
            case TRANSFER -> transferRequestEncoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);
        }

        return directBuffer;
    }
}
