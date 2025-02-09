package com.namnv.core.exception;

public class Bank4xxException extends BankException {

  public Bank4xxException(String message) {
    super(message);
  }

  public Bank4xxException(String message, Throwable cause) {
    super(message, cause);
  }
}
