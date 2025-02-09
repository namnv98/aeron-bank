package com.namnv.command.disruptor.event;


import com.namnv.command.core.BaseCommand;
import com.namnv.command.core.BaseResult;
import com.namnv.command.disruptor.BufferEvent;
import io.aeron.cluster.service.ClientSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.CompletableFuture;

public class RequestEvent implements BufferEvent {
    private long correlationId;
    private BaseCommand command;
    private BaseResult result;
    private CompletableFuture<RequestEvent> completableFuture;

    public long getCorrelationId() {
        return correlationId;
    }

    public BaseCommand getCommand() {
        return command;
    }

    public BaseResult getResult() {
        return result;
    }

    public CompletableFuture<RequestEvent> getCompletableFuture() {
        return completableFuture;
    }

    public RequestEvent() {
    }

    public void setCorrelationId(long correlationId) {
        this.correlationId = correlationId;
    }

    public void setCommand(BaseCommand command) {
        this.command = command;
    }

    public void setResult(BaseResult result) {
        this.result = result;
    }

    public void setCompletableFuture(CompletableFuture<RequestEvent> completableFuture) {
        this.completableFuture = completableFuture;
    }

    public RequestEvent(CompletableFuture<RequestEvent> completableFuture, long correlationId, BaseCommand command) {
        this.completableFuture = completableFuture;
        this.correlationId = correlationId;
        this.command = command;
    }

    public void copy(RequestEvent event) {
        this.correlationId = event.correlationId;
        this.command = event.command;
        this.result = event.result;
        this.completableFuture = event.completableFuture;
    }
}
