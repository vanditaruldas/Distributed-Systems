<h2>CSE 486/586 Distributed Systems</h2>
<h2>Programming Assignment 4</h2>
<h2>Replicated Key-Value Storage</h2>
<h3>Introduction</h3>
At this point, most of you are probably ready to understand and implement a Dynamo-style key-value storage; this assignment is about implementing a simplified version of Dynamo. (And you might argue that it’s not Dynamo any more ;-) There are three main pieces you need to implement: 1) Partitioning, 2) Replication, and 3) Failure handling.

The main goal is to provide both availability and linearizability at the same time. In other words, your implementation should always perform read and write operations successfully even under failures. At the same time, a read operation should always return the most recent value. To accomplish this goal, this document gives you a guideline of the implementation. However, you have freedom to come up with your own design as long as you provide availability and linearizability at the same time (that is, to the extent that the tester can test). The exception is partitioning and replication, which should be done exactly the way Dynamo does.

This document assumes that you are already familiar with Dynamo. If you are not, that is your first step. There are many similarities between this assignment and the previous assignment for the most basic functionalities, and you are free to reuse your code from the previous assignment.

<h3>References</h3>
Before we discuss the requirements of this assignment, here are two references for the Dynamo design:
  
  1) Lecture slides
  
  2) Dynamo paper

The lecture slides give an overview, but do not discuss Dynamo in detail, so it should be a good reference to get an overall idea. The paper presents the detail, so it should be a good reference for actual implementation.

<h3>Step 1: Writing the Content Provider</h3>
Just like the previous assignment, the content provider should implement all storage functionalities. For example, it should create server and client threads (if this is what you decide to implement), open sockets, and respond to incoming requests. When writing your system, you can make the following assumptions:
  
  1) Just like the previous assignment, you need to support insert/query/delete operations. Also, you need to support @ and * queries.
  
  2) There are always 5 nodes in the system. There is no need to implement adding/removing nodes from the system.
     
  3) However, there can be at most 1 node failure at any given time. We will emulate a failure only by force closing an app instance. We      will not emulate a failure by killing an entire emulator instance.

  4) All failures are temporary; you can assume that a failed node will recover soon, i.e., it will not be permanently unavailable            during a run.
  5) When a node recovers, it should copy all the object writes it missed during the failure. This can be done by asking the right nodes      and copy from them.

  6) Please focus on correctness rather than performance. Once you handle failures correctly, if you still have time, you can improve        your performance.

  7) Your content provider should support concurrent read/write operations.

  8) Your content provider should handle a failure happening at the same time with read/write operations.
  
  9) Replication should be done exactly the same way as Dynamo does. In other words, a (key, value) pair should be replicated over three      consecutive partitions, starting from the partition that the key belongs to.

  10) Unlike Dynamo, there are two things you do not need to implement.

        a) Virtual nodes: Your implementation should use physical nodes rather than virtual nodes, i.e., all partitions are static and              fixed.

        b) Hinted handoff: Your implementation do not need to implement hinted handoff. This means that when there is a failure, it is              OK to replicate on only two nodes.
        
  11) All replicas should store the same value for each key. This is “per-key” consistency. There is no consistency guarantee you need         to provide across keys. More formally, you need to implement per-key linearizability.
  
  12) Each content provider instance should have a node id derived from its emulator port. This node id should be obtained by applying         the above hash function (i.e., genHash()) to the emulator port. For example, the node id of the content provider instance running       on emulator-5554 should be, node_id = genHash(“5554”). This is necessary to find the correct position of each node in the Dynamo         ring.

  13) Your content provider’s URI should be “content://edu.buffalo.cse.cse486586.simpledynamo.provider”, which means that any app should       be able to access your content provider using that URI. This is already defined in the template, so please don’t change this. Your       content provider does not need to match/support any other URI pattern.

  14) Any app (not just your app) should be able to access (read and write) your content provider. As with the previous assignment,           please do not include any permission to access your content provider.

  15) Please read the notes at the end of this document. You might run into certain problems, and the notes might give you some ideas         about a couple of potential problems.

The following is a guideline for your content provider based on the design of Amazon Dynamo:

1) Membership

Just as the original Dynamo, every node can know every other node. This means that each node knows all other nodes in the system and also knows exactly which partition belongs to which node; any node can forward a request to the correct node without using a ring-based routing.

2) Request routing

Unlike Chord, each Dynamo node knows all other nodes in the system and also knows exactly which partition belongs to which node.

Under no failures, a request for a key is directly forwarded to the coordinator (i.e., the successor of the key), and the coordinator should be in charge of serving read/write operations.

3) Quorum replication

For linearizability, you can implement a quorum-based replication used by Dynamo.

Note that the original design does not provide linearizability. You need to adapt the design.

The replication degree N should be 3. This means that given a key, the key’s coordinator as well as the 2 successor nodes in the Dynamo ring should store the key.

Both the reader quorum size R and the writer quorum size W should be 2.

The coordinator for a get/put request should always contact other two nodes and get a vote from each (i.e., an acknowledgement for a write, or a value for a read).

For write operations, all objects can be versioned in order to distinguish stale copies from the most recent copy.

For read operations, if the readers in the reader quorum have different versions of the same object, the coordinator should pick the most recent version and return it.

4) Chain replication

Another replication strategy you can implement is chain replication, which provides linearizability.

If you are interested in more details, please take a look at the following paper: http://www.cs.cornell.edu/home/rvr/papers/osdi04.pdf

In chain replication, a write operation always comes to the first partition; then it propagates to the next two partitions in sequence. The last partition returns the result of the write.

A read operation always comes to the last partition and reads the value from the last partition.

5) Failure handling

Handling failures should be done very carefully because there can be many corner cases to consider and cover.

Just as the original Dynamo, each request can be used to detect a node failure.

For this purpose, you can use a timeout for a socket read; you can pick a reasonable timeout value, e.g., 100 ms, and if a node does not respond within the timeout, you can consider it a failure.

Do not rely on socket creation or connect status to determine if a node has failed. Due to the Android emulator networking setup, it is not safe to rely on socket creation or connect status to judge node failures. Please use an explicit method to test whether an app instance is running or not, e.g., using a socket read timeout as described above.

When a coordinator for a request fails and it does not respond to the request, its successor can be contacted next for the request.
