package com.namnv.command.disruptor.request;

import com.namnv.command.disruptor.event.RequestEvent;
import com.namnv.command.handler.CommandHandler;
import lombok.RequiredArgsConstructor;

public class CommandBufferHandlerImpl implements CommandBufferHandler {

    private final CommandHandler commandHandler;

    public CommandBufferHandlerImpl(CommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    @Override
    public void onEvent(RequestEvent event, long sequence, boolean endOfBatch) {
        event.setResult(commandHandler.onCommand(event.getCommand()));
    }
}
