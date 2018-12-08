FROM openjdk:8u181-jre
VOLUME /tmp
ADD target/bookrecedgeservice.jar app.jar
ENTRYPOINT exec java $JAVA_OPTS -jar /app.jar
