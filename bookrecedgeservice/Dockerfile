FROM openjdk:8u181-jre
VOLUME /tmp
ADD target/deors-demos-microservices-bookrecedgeservice.jar app.jar
ENTRYPOINT exec java $JAVA_OPTS -jar /app.jar
