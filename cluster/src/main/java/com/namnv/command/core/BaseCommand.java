package com.namnv.command.core;



public class BaseCommand {
    private CommandLog commandLog;

    public CommandLog getCommandLog() {
        return commandLog;
    }

    public BaseCommand(CommandLog commandLog) {
        this.commandLog = commandLog;
    }
}
