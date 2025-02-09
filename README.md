

Aeron-Bank is a distributed banking system optimized for high transaction write performance, utilizing Aeron Cluster
Raft technology combined with distributed storage methods to ensure consistency and reliability in a multi-node
environment. By applying the Raft Consensus principle, the system maintains data synchronization across nodes in the
cluster, enabling ultra-fast transaction processing with low latency.

Transaction data is temporarily stored in memory or files on the system's nodes. After transactions are processed and
validated, the system performs a snapshot (capturing the data state at a specific point in time) and saves it to a
database (DB) to ensure data durability and recovery capability in case of a failure.

In the event of a failure, the system can replay the transaction state from the most recent snapshot, ensuring no
transaction data is lost. This replay mechanism restores the system to its last consistent state, ensuring transaction
integrity and data reliability even after a crash or other disruptions.

![Untitled-2025-02-08-0308.png](doc/Untitled-2025-02-08-0308.png)