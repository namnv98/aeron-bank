package com.namnv.learner.handler;

import com.lmax.disruptor.EventHandler;

public interface ReplayBufferHandler extends EventHandler<ReplayBufferEvent> {}
