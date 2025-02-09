package com.namnv.command.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


public class TransferCommand {
    double fromId;
    double toId;
    double amount;

    public TransferCommand(double fromId, double toId, double amount) {
        this.fromId = fromId;
        this.toId = toId;
        this.amount = amount;
    }

    public double getFromId() {
        return fromId;
    }

    public void setFromId(double fromId) {
        this.fromId = fromId;
    }

    public double getToId() {
        return toId;
    }

    public void setToId(double toId) {
        this.toId = toId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
