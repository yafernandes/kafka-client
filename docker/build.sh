cd ..
gradle distTar
cp ./java-tracer/dd-java-agent-0.51.0.jar build/distributions/dd-java-agent.jar
docker build -t kafka-client-example -f docker/Dockerfile 
docker push yaalexf/kafka-client
cd -