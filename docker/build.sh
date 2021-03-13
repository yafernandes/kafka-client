cd ..
gradle assemble
docker build -t yaalexf/kafka-client -f docker/Dockerfile .
docker push yaalexf/kafka-client
cd -