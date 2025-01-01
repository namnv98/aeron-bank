
package com.namnv.services.infra;

import com.namnv.services.bank.util.ExecutionResult;
import com.namnv.services.bank.util.Status;
import io.aeron.cluster.service.ClientSession;
import org.agrona.concurrent.IdleStrategy;

public interface ClusterClientResponder {
    void onExecutionResult(ClientSession session, long correlationId, ExecutionResult executionResult);

    void onSuccessMessage(ClientSession session, long correlationId);

    void onTransfer(ClientSession session, long correlationId, Status status);

    void setIdleStrategy(IdleStrategy idleStrategy);
}
