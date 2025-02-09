package com.namnv.config;

import java.time.Duration;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationConfig {
  private int nodeID;
  private int maxNodes;
  private boolean test;
  private Database database;
  private LearnerProperties learner = new LearnerProperties();

  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Database {
    private String url;
    private String username;
    private String password;
  }

  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class LearnerProperties {
    private int bufferSize = 1 << 5;
    private int pollingInterval = 100;
    private int maxSnapshotCheckCircles = 50;
    private int snapshotFragmentSize = 10_000;
    private Duration snapshotLifeTime = Duration.ofSeconds(5);
  }
}
