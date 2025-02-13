package com.namnv.command.disruptor.request;

import com.lmax.disruptor.EventHandler;
import com.namnv.command.disruptor.event.RequestEvent;

public interface CommandBufferHandler
        extends EventHandler<RequestEvent> {
}
