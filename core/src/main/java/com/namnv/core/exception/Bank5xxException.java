package com.namnv.core.exception;

public class Bank5xxException extends BankException {
  public Bank5xxException(String message) {
    super(message);
  }

  public Bank5xxException(String message, Throwable cause) {
    super(message, cause);
  }
}
