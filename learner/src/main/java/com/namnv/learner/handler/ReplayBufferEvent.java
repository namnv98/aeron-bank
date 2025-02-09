package com.namnv.learner.handler;

import com.namnv.command.core.BaseCommand;
import com.namnv.command.disruptor.BufferEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReplayBufferEvent implements BufferEvent {
  private BaseCommand command;
}
