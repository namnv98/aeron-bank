package com.namnv.services.bank;

import com.namnv.services.infra.ClusterClientResponder;
import com.namnv.services.bank.util.ExecutionResult;
import com.weareadaptive.sbe.TransferRequestDecoder;
import io.aeron.ExclusivePublication;
import io.aeron.Image;
import io.aeron.cluster.service.ClientSession;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.IdleStrategy;


public class OMSService {
//    private final SnapshotManager snapshotManager = new SnapshotManager();
    private BankServiceImpl bankService = new BankServiceImpl();

    private final TransferRequestDecoder transferRequestDecoder = new TransferRequestDecoder();
    private final ClusterClientResponder clusterClientResponder;


    public OMSService(ClusterClientResponder clusterClientResponder, IdleStrategy idleStrategy) {
//        snapshotManager.setIdleStrategy(idleStrategy);
        this.clusterClientResponder = clusterClientResponder;
    }

    public void messageHandler(final ClientSession session, final long correlationId, final int templateId, final DirectBuffer buffer, final int offset,
                               int actingBlockLength, int actingVersion) {
        switch (templateId) {
            case TransferRequestDecoder.TEMPLATE_ID ->
                    transfer(session, correlationId, buffer, offset, actingBlockLength, actingVersion);
        }
    }


    private void transfer(final ClientSession session, final long messageId, final DirectBuffer buffer, final int offset,
                          int actingBlockLength, int actingVersion) {
        transferRequestDecoder.wrap(buffer, offset, actingBlockLength, actingVersion);
        final double fromId = transferRequestDecoder.fromId();
        final double toId = transferRequestDecoder.toId();
        final double amount = transferRequestDecoder.amount();

        final ExecutionResult executionResult = bankService.onTransfer(fromId, toId, amount);

        clusterClientResponder.onTransfer(session, messageId, executionResult.getStatus());
    }


    public void onTakeSnapshot(ExclusivePublication snapshotPublication) {
//        snapshotManager.encodeOrderbookState(snapshotPublication, orderbook.getAsks(), orderbook.getBids(), orderbook.getCurrentOrderId());
    }

    public void onRestoreSnapshot(Image snapshotImage) {
//        orderbook = snapshotManager.loadSnapshot(snapshotImage);
    }
}
