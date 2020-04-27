FROM openjdk:11
ADD build/distributions/kafka-client.tar /
COPY build/distributions/dd-java-agent.jar /
ENV JAVA_OPTS -javaagent:/dd-java-agent.jar
CMD [ "/kafka-client/bin/kafka-client" ]
