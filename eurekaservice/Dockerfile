FROM openjdk:8u181-jre
VOLUME /tmp
ADD target/deors-demos-microservices-eurekaservice.jar app.jar
ENTRYPOINT exec java $JAVA_OPTS -jar /app.jar
