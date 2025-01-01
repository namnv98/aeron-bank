package com.namnv.services.bank;


import com.namnv.services.bank.util.ExecutionResult;

public interface IBankService {
    ExecutionResult onTransfer(double fromId, double toId, double amount);
}
