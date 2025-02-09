package com.namnv.command.disruptor.request;

import com.lmax.disruptor.dsl.Disruptor;
import com.namnv.command.disruptor.event.RequestEvent;
import com.namnv.command.disruptor.event.RequestEventDispatcher;

public class RequestEventDispatcherImpl implements RequestEventDispatcher {

    private final Disruptor<RequestEvent> replyBufferEventDisruptor;

    public RequestEventDispatcherImpl(Disruptor<RequestEvent> requestEventDisruptor) {
        this.replyBufferEventDisruptor = requestEventDisruptor;
    }

    @Override
    public void dispatch(RequestEvent requestEvent) {
        replyBufferEventDisruptor.publishEvent(
                (event, sequence) -> {
                    event.setCorrelationId(requestEvent.getCorrelationId());
                    event.setResult(requestEvent.getResult());
                    event.setCompletableFuture(requestEvent.getCompletableFuture());
                    event.setCommand(requestEvent.getCommand());
                });
    }
}
