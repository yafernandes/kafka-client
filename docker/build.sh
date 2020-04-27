cd ..
gradle distTar
cp ~/java-lib/dd-java-agent-0.48.0.jar build/distributions/dd-java-agent.jar
docker build -t yaalexf/kafka-client -f docker/Dockerfile .
docker push yaalexf/kafka-client
cd -