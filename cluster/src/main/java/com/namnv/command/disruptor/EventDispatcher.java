package com.namnv.command.disruptor;

public interface EventDispatcher<T extends BufferEvent> {
    void dispatch(T event);
}
