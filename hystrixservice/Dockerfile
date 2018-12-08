FROM openjdk:8u181-jre
VOLUME /tmp
ADD target/hystrixservice.jar app.jar
ENTRYPOINT exec java $JAVA_OPTS -jar /app.jar
