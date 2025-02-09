package com.namnv.core.repository;

import com.namnv.core.model.Balance;

import java.util.List;

public interface BalanceRepository {

  Long getLastOffset();

  List<Balance> balances();

  void persistLastOffset(long offset);

  void persistBalances(List<Balance> balances);

  void persistLastOffsetAndBalances(long offset, List<Balance> balances);
}
