CREATE TABLE balances
(
    id          BIGINT PRIMARY KEY,
    amount      BIGINT  NOT NULL DEFAULT 0,
    precision   INT     NOT NULL DEFAULT 2,
    active      BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE snapshots
(
    id  VARCHAR(36) PRIMARY KEY,
    value VARCHAR(255) NOT NULL
);

INSERT INTO snapshots (id, value) VALUES ('LAST_KAFKA_OFFSET', '-1');
INSERT INTO snapshots (id, value) VALUES ('LAST_BALANCE_ID', '0');

INSERT INTO balances (id, amount, precision, active) VALUES (1, 10000000, 2, true);
INSERT INTO balances (id, amount, precision, active) VALUES (2, 10000000, 2, true);
