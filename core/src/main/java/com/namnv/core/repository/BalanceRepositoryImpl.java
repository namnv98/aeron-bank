package com.namnv.core.repository;

import com.google.inject.Inject;
import com.namnv.core.model.Balance;
import com.namnv.core.model.SnapshotType;
import java.util.List;
import java.util.stream.Collectors;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BalanceRepositoryImpl implements BalanceRepository {

  private static final Logger logger = LoggerFactory.getLogger(BalanceRepositoryImpl.class);
  private final Jdbi jdbi;

  @Inject
  public BalanceRepositoryImpl(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public Long getLastOffset() {
    return jdbi.withHandle(
        handle -> {
          String query = "SELECT value FROM snapshots WHERE id = :id";
          return handle
              .createQuery(query)
              .bind("id", SnapshotType.LAST_KAFKA_OFFSET.getType())
              .mapTo(String.class)
              .findOne()
              .map(Long::parseLong)
              .orElse(-1L);
        });
  }

  @Override
  public List<Balance> balances() {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT id, amount, precision, active FROM balances")
                .map(
                    (rs, ctx) -> {
                      var balance = new Balance();
                      balance.setId(rs.getLong("id"));
                      balance.setAmount(rs.getLong("amount"));
                      balance.setPrecision(rs.getInt("precision"));
                      balance.setActive(rs.getBoolean("active"));
                      return balance;
                    })
                .list());
  }

  @Override
  public void persistLastOffsetAndBalances(long offset, List<Balance> balances) {
    jdbi.useTransaction(
        handle -> {
          persistLastOffset(offset);
          persistBalances(balances);
        });
  }

  @Override
  public void persistLastOffset(long offset) {
    jdbi.useHandle(
        handle -> {
          try {
            String updateOffsetQuery = "UPDATE snapshots SET value = :value WHERE id = :id";
            handle
                .createUpdate(updateOffsetQuery)
                .bind("value", String.valueOf(offset))
                .bind("id", SnapshotType.LAST_KAFKA_OFFSET.getType())
                .execute();
          } catch (Exception e) {
            logger.error("An error occurred while persisting the last offset: ", e);
            throw new RuntimeException("Error persisting last offset", e);
          }
        });
  }

  @Override
  public void persistBalances(List<Balance> balances) {
    if (balances == null || balances.isEmpty()) {
      logger.warn("Received empty or null balances list.");
      return;
    }

    jdbi.useHandle(
        handle -> {
          try {
            handle.execute(
                """
                        CREATE TEMPORARY TABLE temp_balances(
                            id          BIGINT PRIMARY KEY,
                            amount      BIGINT  NOT NULL DEFAULT 0,
                            precision   INT     NOT NULL DEFAULT 2,
                            active      BOOLEAN NOT NULL DEFAULT FALSE
                        );
                    """);

            String values =
                balances.stream()
                    .map(
                        balance ->
                            String.format(
                                "(%s,%s,%s,%s)",
                                balance.getId(),
                                balance.getAmount(),
                                balance.getPrecision(),
                                balance.isActive()))
                    .collect(Collectors.joining(","));

            String insertTempBalance =
                String.format("INSERT INTO temp_balances VALUES %s;", values);
            handle.execute(insertTempBalance);

            handle.execute(
                """
                        INSERT INTO balances (id, amount, precision, active)
                        SELECT id, amount, precision, active FROM temp_balances
                        ON CONFLICT (id)
                        DO UPDATE SET
                            amount = EXCLUDED.amount,
                            precision = EXCLUDED.precision,
                            active = EXCLUDED.active;
                    """);

            handle.execute("DROP TABLE IF EXISTS temp_balances CASCADE;");
          } catch (Exception e) {
            logger.error("An error occurred while persisting balances: ", e);
            throw new RuntimeException("Error persisting balances", e);
          }
        });
  }
}
