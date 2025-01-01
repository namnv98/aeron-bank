package com.namnv.services.bank;


import com.namnv.services.bank.util.ExecutionResult;
import com.namnv.services.bank.util.Status;

public class BankServiceImpl implements IBankService {

    @Override
    public ExecutionResult onTransfer(double fromId, double toId, double amount) {
        return new ExecutionResult(1, Status.RESTING);
    }

}
