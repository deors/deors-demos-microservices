FROM adoptopenjdk/openjdk8:jdk8u202-b08
VOLUME /tmp
ADD target/dependency/jacocoagent.jar jacocoagent.jar
ADD target/configservice.jar app.jar
ENTRYPOINT exec java $JAVA_OPTS -jar /app.jar
