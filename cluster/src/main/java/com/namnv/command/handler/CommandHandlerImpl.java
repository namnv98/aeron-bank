package com.namnv.command.handler;

import com.namnv.command.core.BalanceResult;
import com.namnv.command.core.BaseCommand;
import com.namnv.command.core.BaseResult;
import com.namnv.command.core.TransferCommand;
import com.namnv.core.repository.Balances;
import com.namnv.core.exception.Bank4xxException;
import com.namnv.core.exception.Bank5xxException;


public class CommandHandlerImpl implements CommandHandler {

  private final Balances balances;

  public CommandHandlerImpl(Balances balances) {
    this.balances = balances;
  }

  @Override
  public BaseResult onCommand(BaseCommand command) {
    balances.setOffset(command.getCommandLog().getPosition());
    try {
      return switch (command.getCommandLog().getTypeCase()) {
        case TRANSFERCOMMAND -> handle(command.getCommandLog().getTransferCommand());
        default ->
            throw new IllegalStateException(
                "Unexpected value: " + command.getCommandLog().getType());
      };
    } catch (Bank4xxException e) {
      return new BalanceResult(e.getMessage(), 400);
    } catch (Bank5xxException e) {
      return new BalanceResult(e.getMessage(), 500);
    } catch (Exception e) {
      return new BalanceResult("Unknown exception", 500);
    }
  }

  private BalanceResult handle(TransferCommand command) {
    balances.transfer(
        (long) command.getFromId(), (long) command.getToId(), (long) command.getAmount());
    //    System.out.println(command);
    return new BalanceResult("Transfer success", 0);
  }
}
