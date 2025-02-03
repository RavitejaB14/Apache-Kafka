Kafka file connector:

Go to Kafka home Directory/config

clone this file: connect-standalone.properties and change these properties

key.converter=org.apache.kafka.connect.storage.StringConverter
value.converter=org.apache.kafka.connect.storage.StringConverter
plugin.path=libs

Add a file sink connector properties:

name=file-sink-standlone
connector.class=FileStreamSink
tasks.max=1
file=/tmp/test-sink-standlone.txt
topics=file-sink-standlone-test


Start zookeeper and kafka server

bin/zookeeper-server-start.sh config/zookeeper.properties
bin/kafka-server-start.sh -daemon config/server.properties

Start the file connector:

bin/connect-standalone.sh config/connect-standalone_file.properties config/file-sink-connector.properties

create a kafka topic:

bin/kafka-topics.sh --bootstrap-server localhost:9092 --create  --topic file-sink-standlone-test --partitions 1 --replication-factor 1

send the messages:
bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic file-sink-standlone-test

It should create a file : /tmp/test-sink-standlone.txt

tail -f /tmp/test-sink-standlone.txt

This should start receiving messages


