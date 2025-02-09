package com.namnv.core.model;

import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class Balance {
  private long id;
  private long amount;
  private int precision;
  private boolean active;

  public void increase(long amount) {
    this.amount += amount;
  }

  public void decrease(long amount) {
    if (this.amount < amount) {
      throw new RuntimeException("Insufficient balance");
    }
    this.amount -= amount;
  }

  public void active() {
    this.active = true;
  }

  public void inactive() {
    this.active = false;
  }
}
