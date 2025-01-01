package com.namnv.services.oms;


import com.namnv.services.oms.util.ExecutionResult;
import com.namnv.services.oms.util.Status;

public class BankServiceImpl implements IBankService {

    @Override
    public ExecutionResult onTransfer(double fromId, double toId, double amount) {
        return new ExecutionResult(1, Status.RESTING);
    }

}
