package com.namnv.services.oms;


import com.namnv.services.oms.util.ExecutionResult;

public interface IBankService {
    ExecutionResult onTransfer(double fromId, double toId, double amount);
}
