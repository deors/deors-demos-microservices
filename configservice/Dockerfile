FROM eclipse-temurin:18.0.1_10-jdk
VOLUME /tmp
ADD target/dependency/jacocoagent.jar jacocoagent.jar
ADD target/configservice.jar app.jar
ENTRYPOINT exec java $JAVA_OPTS -jar /app.jar
