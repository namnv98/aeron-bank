package com.namnv.core.state;

import com.namnv.core.repository.Balances;
import com.namnv.core.exception.BankException;
import com.namnv.core.model.StateMachineStatus;
import com.namnv.core.repository.BalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class StateMachineManagerImpl implements StateMachineManager {

  private final Balances balances;
  private final BalanceRepository balanceRepository;
  private StateMachineStatus status = StateMachineStatus.INITIALIZING;
  long offset;

  public void reloadSnapshot() {
    if (status != StateMachineStatus.INITIALIZING) {
      throw new BankException("Cannot reload snapshot when status is not INITIALIZING");
    }
    status = StateMachineStatus.LOADING_SNAPSHOT;

    balanceRepository.balances().forEach(balances::putBalance);
    offset = Optional.ofNullable(balanceRepository.getLastOffset()).orElse(-1L);
    balances.setOffset(offset);
    status = StateMachineStatus.LOADED_SNAPSHOT;
    log.info("Loaded snapshot with offset: {}", offset);
  }

  @Override
  public void takeSnapshot() {
    balanceRepository.persistLastOffsetAndBalances(
        balances.getOffset(), balances.getChangedBalances());
    balances.clearChangedBalances();
  }
}
