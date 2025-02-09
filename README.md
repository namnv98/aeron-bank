# Aeron-Bank: High-Performance Distributed Banking System with In-Memory Transactions and Fault Tolerance
**Aeron-Bank** is a distributed banking system optimized for high transaction write performance, utilizing **Aeron
Cluster Raft** technology combined with distributed storage methods to ensure consistency and reliability in a
multi-node environment. By applying the **Raft Consensus** principle, the system maintains data synchronization across
nodes in the cluster, enabling ultra-fast transaction processing with low latency.

To achieve high performance, **transactions are executed in memory** rather than relying on traditional database
systems. This in-memory transaction approach significantly reduces the latency and overhead associated with disk I/O
operations, allowing the system to handle a much higher volume of transactions per second. The transaction data is
temporarily stored in memory or files on the system's nodes, allowing for lightning-fast access and updates.

Once the transactions are processed and validated in memory, the system performs a **snapshot** (capturing the data
state at a specific point in time) and stores it in a database (DB) to ensure data durability and recovery capability in
case of failure.

In the event of a failure, even a **severe failure** that causes the entire cluster to go down, the system can still *
*replay the transaction state** from the **Raft log** and the most recent **snapshot** stored in the database. The Raft
log contains a complete sequence of all committed transactions, and by replaying these logs from the point of the last
successful snapshot, the system can restore itself to its last consistent state. This ensures that no transaction data
is lost, even after catastrophic failures, and guarantees system integrity and recovery.

By performing transactions in memory and only periodically saving snapshots to a database, **Aeron-Bank** achieves
significantly higher performance compared to traditional database-driven banking systems while maintaining the
reliability and fault tolerance necessary for financial transactions.

![Untitled-2025-02-08-0308.png](doc/Untitled-2025-02-08-0308.png)