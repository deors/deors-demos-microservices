FROM openjdk:8u181-jre
VOLUME /tmp
ADD target/configservice.jar app.jar
ENTRYPOINT exec java $JAVA_OPTS -jar /app.jar
