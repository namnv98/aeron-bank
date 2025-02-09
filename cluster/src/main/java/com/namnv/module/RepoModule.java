package com.namnv.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.namnv.config.ApplicationConfig;
import com.namnv.core.repository.BalanceRepository;
import com.namnv.core.repository.BalanceRepositoryImpl;
import com.namnv.core.repository.Balances;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import org.jdbi.v3.core.Jdbi;

@AllArgsConstructor
public class RepoModule extends AbstractModule {
  private final ApplicationConfig applicationConfig;

  @Override
  protected void configure() {}

  @Provides
  @Singleton
  Jdbi jdbi() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(applicationConfig.getDatabase().getUrl());
    config.setUsername(applicationConfig.getDatabase().getUsername());
    config.setPassword(applicationConfig.getDatabase().getPassword());
    var dataSource = new HikariDataSource(config);
    return Jdbi.create(dataSource);
  }

  @Provides
  @Singleton
  public Balances balances() {
    return new Balances();
  }

  @Provides
  @Singleton
  BalanceRepository balanceRepository(Jdbi jdbi) {
    return new BalanceRepositoryImpl(jdbi);
  }
}
