FROM adoptopenjdk/openjdk11:jdk-11.0.2.9
VOLUME /tmp
ADD target/dependency/jacocoagent.jar jacocoagent.jar
ADD target/hystrixservice.jar app.jar
ENTRYPOINT exec java $JAVA_OPTS -jar /app.jar
