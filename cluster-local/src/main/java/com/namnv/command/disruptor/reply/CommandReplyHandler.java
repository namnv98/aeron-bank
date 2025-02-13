package com.namnv.command.disruptor.reply;

import com.lmax.disruptor.EventHandler;
import com.namnv.command.disruptor.event.RequestEvent;

public interface CommandReplyHandler
        extends EventHandler<RequestEvent> {
}
