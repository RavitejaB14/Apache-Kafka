Kafka file connector:

Go to Kafka home Directory/config

clone this file: connect-distributed.properties

cp connect-distributed.properties connect-distributed_demo.properties

 and change these properties

key.converter=org.apache.kafka.connect.storage.StringConverter
value.converter=org.apache.kafka.connect.storage.StringConverter
plugin.path=libs


Start zookeeper and kafka server

bin/zookeeper-server-start.sh config/zookeeper.properties
bin/kafka-server-start.sh -daemon config/server.properties


create a kafka topic:

bin/kafka-topics.sh --bootstrap-server localhost:9092 --create  --topic file-sink-test --partitions 1 --replication-factor 1

Start the connector:

 bin/connect-distributed.sh --daemon config/connect-distributed_demo.properties 

Check the status:

curl localhost:8083                                                   
{"version":"3.9.0","commit":"a60e31147e6b01ee","kafka_cluster_id":"vr5Sa6fnRNykhJWT4Vh5ig"}%  

curl localhost:8083/connectors
[]%                                

Currently No connectors are running

now create the json file: connect-file-sink.json

{
  "name": "file-sink-test"
  "config":{
     "connector.class": "FileStreamSink"
     "task.max": "1",
     "file": "/tmp/file-sink-test.txt",
     "topics": "file-sink-test",
     "name": "file-sink-test"
	}
}

Submit the config to Kafka connect:

curl -X POST -H "Content-Type: application/json" --data @config/connect-file-sink.json http://localhost:8083/connectors

{"name":"file-sink-test","config":{"connector.class":"FileStreamSink","tasks.max":"1","file":"/tmp/file-sink-test.txt","topics":"file-sink-test","name":"file-sink-test"},"tasks":[],"type":"sink"}%                        


curl localhost:8083/connectors/file-sink-test | jq                                                                     
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100   234  100   234    0     0   2793      0 --:--:-- --:--:-- --:--:--  2819
{
  "name": "file-sink-test",
  "config": {
    "connector.class": "FileStreamSink",
    "file": "/tmp/file-sink-test.txt",
    "tasks.max": "1",
    "topics": "file-sink-test",
    "name": "file-sink-test"
  },
  "tasks": [
    {
      "connector": "file-sink-test",
      "task": 0
    }
  ],
  "type": "sink"
}

Check the status:

curl localhost:8083/connectors/file-sink-test/status | jq
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100   174  100   174    0     0   2371      0 --:--:-- --:--:-- --:--:--  2383
{
  "name": "file-sink-test",
  "connector": {
    "state": "RUNNING",
    "worker_id": "192.168.0.100:8083"
  },
  "tasks": [
    {
      "id": 0,
      "state": "RUNNING",
      "worker_id": "192.168.0.100:8083"
    }
  ],
  "type": "sink"
}



send the messages:
bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic file-sink-test

It should create a file : /tmp/file-sink-test.txt

tail -f /tmp/file-sink-test.txt

This should start receiving messages


