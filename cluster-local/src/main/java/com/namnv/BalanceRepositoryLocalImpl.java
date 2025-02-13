package com.namnv;

import com.namnv.core.model.Balance;
import com.namnv.core.repository.BalanceRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BalanceRepositoryLocalImpl implements BalanceRepository {

  private static final Logger logger = LoggerFactory.getLogger(BalanceRepositoryLocalImpl.class);

  public BalanceRepositoryLocalImpl() {
  }

  @Override
  public Long getLastOffset() {
    return 0L;
  }

  @Override
  public List<Balance> balances() {
    return List.of(new Balance(1, 1000000000, 2, true), new Balance(2, 1000000000, 2, true));
  }

  @Override
  public void persistLastOffsetAndBalances(long offset, List<Balance> balances) {}

  @Override
  public void persistLastOffset(long offset) {}

  @Override
  public void persistBalances(List<Balance> balances) {}
}
