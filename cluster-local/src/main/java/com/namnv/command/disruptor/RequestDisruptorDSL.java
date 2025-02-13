package com.namnv.command.disruptor;

import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.namnv.command.disruptor.event.RequestEvent;
import com.namnv.command.disruptor.reply.CommandReplyHandler;
import com.namnv.command.disruptor.request.CommandBufferHandler;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RequestDisruptorDSL implements DisruptorDSL<RequestEvent> {
    private final CommandBufferHandler commandBufferHandler;
    private final CommandReplyHandler commandBufferReply;

    @Override
    public Disruptor<RequestEvent> build(int bufferSize, WaitStrategy waitStrategy) {
        Disruptor<RequestEvent> disruptor =
                new Disruptor<RequestEvent>(
                        RequestEvent::new,
                        bufferSize,
                        DaemonThreadFactory.INSTANCE,
                        ProducerType.SINGLE,
                        waitStrategy);
        disruptor
                .handleEventsWith(commandBufferHandler)
                .then(commandBufferReply);
        return disruptor;
    }
}
