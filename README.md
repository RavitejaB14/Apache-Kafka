# Apache-Kafka

Apache kafka: A High throughput distributed messaging system.

Apache Kafka is an event streaming platform used to collect, process, store, and integrate data at scale. It has numerous use cases including distributed logging, stream processing, data integration, and pub/sub messaging.

Events: An event is any type of action, incident, or change that's identified or recorded by software or applications. For example, a payment, a website click, or a temperature reading, along with a description of what happened.

Kafka models events as key/value pairs. Internally, keys and values are just sequences of bytes, but externally in your programming language of choice, they are often structured objects represented in your language’s type system.

Kafka famously calls the translation between language types and internal bytes serialization and deserialization. The serialized format is usually JSON, JSON Schema, Avro, or Protobuf.

Both Key and values pairs are just sequence of bytes.


Features:

1. Numerous Clients
2. Pull Mechanism
3. Disturbuted
4. Fault tolerant
5. Record retention
6. Scaling
7. Great Community
8. Fast/Real time Streaming


Kafka Core Components:

1. Kafka Broker
2. Kafka Client
3. Kafka Connect(Framework) -> source Connector/Source task and Sink Connector/Sink Task
4. Kafka Streams
5. KSQL


Kafka Core concepts:

1. Producer
2. Consumer
3. Broker/Kafka Server
4. Cluster -> Group of Brokers
5. Topic
6. Partitions
7. Offsets : topic name -> Partition Number -> offset Number > to locate a message
8. Consumer Groups


Single Message Transformations in Kafka Connect: we can do transformations on each meesage for both source and sink connectors

1. Adding a new field in your record using static data or metadata
2. Filter or rename fields.
3. Mask some fields with a Null value
4. Change the Record key
5. Route the key to different Kafka Topic.


Kafka Connect Architecture:

1. Worker -> Kafka connect is a cluster which runs on one or more workers. These workers are fault talorant and self managed. They use the group ID to form a cluster. Start the workers with same group ID. They are responsible for starting connector and run the task. The connectors should be installed within the connect cluster. One of the worker will start the connector process(Task split and Task List and configuration). Tasks will be distributed across the cluster for balancing the load. now the task is responsible for connecting to the source System(poll the data at a regular interval, collecting the records and handing over to workers. workers are responsible for sending the records to kafka broker).
These workers will provide:
 -> Reliability
 -> High Avalibility
 -> Scalability
 -> Load Balancing

2. Connector
3. Task


Kafka Streams:

Data Streams are unbounded (No definite Starting or ending - Often infinite and ever growing)
Sequence of data in small packets(KB)

Kafka Streams are the Java/Scala Library
Input data must be a kafka Topic
You can embed Kafka streams in your microservices.
Deploy anywhere (No Cluster needed)
parallel processing,Scalability,Fault tolerance


Kafka Streams Offers:
1. Working on streams/Tables and interoperating with them
2. Grouping and continuously updating Aggregates
3. Join streams, tables,
4. Create and manage fault- tolerant efficient local state stores
5. Flexible window capabilities
6. Flexible time Schemetics - Event time, Processing time,High Watermark, exactly once processing.. etc
7. Interactive query
8. Unit testing tools
9. Inherant fault tolerant and dynamic scalability
10. Deploy in containers and managed by Kubernetes.


Kafka Stream Architecture:

Kafka Stream is all about continuously reading stream of data from one or more kafka topics. and then you can develop your application logic to process those streams in real time

Consider we have two topics(each have three partitions) now Kafka streams will create three logical tasks Because 
the maximum number of partitions across the input topics T1 and T2  are three partitions.

Kafka streams framework knows that we can create three consumers where each could be consuming from one partitions in parallal.

Kafka framework would allocate partitions evenly that is , one partition from each topic to each task.
So that each task will have two paritions to process.



KSQL: SQL Interface of Kafka Streams

KSQL runs on two Modes:
1. Interactive Mode (CLI/ Web based UI to submit KSQL)
2. Headless Mode( Non Interactive mode which are executed by Ksql Server) - production Environment

KSQL Architecture:

Components:

1. KSQL Engine
2. REST Interface 
3. KSQL Client (CLI/UI)

Ksql Server = KSQL Engine + REST Interface

KSQL Client -> KSQL Server cluster -> Kafka Cluster


KSQL Allows you to 
1. Grouping and Aggregrating on your kafka topics.
2. Grouping and Aggregrating over a time window.
3. Apply Filters
4. Join two topics
5. Sink the result of your query into another topic.

When to use what?

Data Integrations/Ingestion PLatform: Kafka Broker, Kafka Client API, kafka Connect
Real time stream Processing application adopting microservice Architecture: Broker, client API's(Producers) and Kafka Streams
Real time stream Processing in Data Lake and Data Warehourse: Kafka Broker and internal, Interacting with kafka using Spark Structured Streaming



** To inside what is there in Kafka Log:

kafka-dump-log.sh --files <path to the log file>



** Broker Responsibilities:

1. Receives messages from producers and acknowledge the successfull receipt.
2. Store the messages in a log file to safeguard it.
3. Deliver the messages to the consumer whenever they request it.

Apache Kafka is Horizontally Scalable, Fault tolerant, distributed streaming platform.
and it is designed for building real-time streaming data architecture.

Kafka Storage Architecture: Kafka topic, logs, partitions, replication factor, segments, Offsets, Offset-index
Kafka Cluster Architecture: Cluster Formation, Zookeeper, and controller.
Work distribution Architecture:  Leader, Follower, In Sync Replicas, Committed and uncommitted messages.


