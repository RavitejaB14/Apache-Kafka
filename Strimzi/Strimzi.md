Develop Kafka Applications with Strimzi and minikube

curl -L0 https://strimzi.io/install/latest | sed 's/namespace: .*/namespace: default/' | kubectl apply -f -


curl -L0 https://strimzi.io/examples/latest/kafka/kafka-persistent.yaml | vi -



Producing some records with strimzi


set -e

kubectl run -n strimzi --image strimzi/kafka producer --comand  -- /opt/kafka/bin/kafka-producer-perf-test.sh \
--topic test-topic \
--num-records 1000000 \
--record-size 100 \ 
--throughput 1000 \ 
--producer-props bootstrap.servers=simple-strimzi-kafka-bootstrap:9092


kubetail producer -n strimzi

