package com.namnv.command.handler;


import com.namnv.command.core.BaseCommand;
import com.namnv.command.core.BaseResult;

public interface CommandHandler {
    BaseResult onCommand(BaseCommand command);
}
