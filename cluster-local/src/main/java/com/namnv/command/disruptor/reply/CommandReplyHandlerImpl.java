package com.namnv.command.disruptor.reply;

import com.namnv.command.disruptor.event.RequestEvent;
import lombok.RequiredArgsConstructor;

public class CommandReplyHandlerImpl implements CommandReplyHandler {

    @Override
    public void onEvent(RequestEvent event, long sequence, boolean endOfBatch) {
        event.getCompletableFuture().complete(event);
    }
}
