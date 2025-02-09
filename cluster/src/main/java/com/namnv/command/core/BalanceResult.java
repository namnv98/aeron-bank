package com.namnv.command.core;

public class BalanceResult extends BaseResult {
    private String message;
    private int code;

    public BalanceResult(String message, int code) {
        this.message = message;
        this.code = code;
    }

    @Override
    public String toString() {
        return String.format("%s::%s", code, message);
    }
}
