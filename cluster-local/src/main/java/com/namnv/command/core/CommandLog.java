package com.namnv.command.core;

import lombok.Getter;

@Getter
public class CommandLog {
  private int type;
  private long position;
  private TransferCommand transferCommand;

  public CommandLog(int type, long position, TransferCommand transferCommand) {
    this.type = type;
    this.position = position;
    this.transferCommand = transferCommand;
  }

  public CommandLog(int type, TransferCommand transferCommand) {
    this.type = type;
    this.transferCommand = transferCommand;
  }

  public enum TypeCase {
    CREATEBALANCECOMMAND(1),
    DEPOSITCOMMAND(2),
    WITHDRAWCOMMAND(3),
    TRANSFERCOMMAND(4),
    TYPE_NOT_SET(0);
    private final int value;

    private TypeCase(int value) {
      this.value = value;
    }

    public static TypeCase valueOf(int value) {
      return forNumber(value);
    }

    public static TypeCase forNumber(int value) {
      switch (value) {
        case 1:
          return CREATEBALANCECOMMAND;
        case 2:
          return DEPOSITCOMMAND;
        case 3:
          return WITHDRAWCOMMAND;
        case 4:
          return TRANSFERCOMMAND;
        case 0:
          return TYPE_NOT_SET;
        default:
          return null;
      }
    }

    public int getNumber() {
      return this.value;
    }
  };

  public TypeCase getTypeCase() {
    return TypeCase.forNumber(type);
  }

  public int getType() {
    return type;
  }

  public void setType(int type) {
    this.type = type;
  }

  public TransferCommand getTransferCommand() {
    return transferCommand;
  }

  public void setTransferCommand(TransferCommand transferCommand) {
    this.transferCommand = transferCommand;
  }
}
