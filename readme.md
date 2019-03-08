# deors-demos-microservices

## Microservices with Spring Boot, Spring Cloud and Netflix OSS

Demonstration of an exemplar microservices stack based in Spring Boot, Spring Cloud and Netflix OSS, including step-by-step instructions to recreate the demo stack and run it in different environments.

This demo is organised in iterations, starting from the basics and building up in complexity and features along the way.

NOTE: The following labs are created on a Linux/OSX machine, hence some commands may need slight adjustments when working on Windows, e.g. replace `${ENV_VAR}` by `%ENV_VAR%`, and replace forward-slashes by back-slashes (although in some commands Windows also understand forward-slashes).

NOTE: The following labs have been tested with Spring Boot 2.1.3, Apache Maven 3.6.0 and Java 11.0.2, the latest versions of them available at the time of publishing this.

HISTORY NOTES: This demonstration and guideline was first featured in a live coding session in OpenSlava 2016 conference, in the beautiful city of Bratislava, and the pipelines lesson was first presented in Oracle Code One San Francisco 2018 conference.

## Iteration 1) The basics

### 1.1) Set up the configuration store

The configuration store is a repository where microservice settings are stored, and accessible for microservice initialization at boot time.

Create and change to a directory for the project:

    mkdir ~/microservices/configstore
    cd ~/microservices/configstore

Create the file `application.properties`. This file will contain settings which are common to all microservices:

    debug = true
    spring.jpa.generate-ddl = true
    management.security.enabled = false
    management.endpoints.web.exposure.include = *

The third configuration setting will disable security for actuator endpoints, which allows for remote operations on running applications. Disabling security in this manner should be done only when public access to those endpoints is restricted externally (for example through the web server or reverse proxy). Never expose actuator endpoints publicly and insecurely!

The fourth configuration setting will expose all actuator endpoint via http, which will allow us to easily inspect and monitor each service, including the endpoint used by Hystrix (see later) to monitor performance of individual services.

Next, we will add the setting for all microservices. These settings may not be obvious now, but they will later once they are needed.

Create the file `eurekaservice.properties`:

    server.port = ${PORT:7878}
    eureka.client.register-with-eureka = false
    eureka.client.fetch-registry = false
    eureka.client.serviceUrl.defaultZone = http://${EUREKA_HOST:localhost}:${EUREKA_PORT:7878}/eureka/

Create the file `hystrixservice.properties`:

    server.port = ${PORT:7979}
    eureka.client.serviceUrl.defaultZone = http://${EUREKA_HOST:localhost}:${EUREKA_PORT:7878}/eureka/

Create the file `bookrecservice.properties`:

    server.port = ${PORT:8080}
    eureka.client.serviceUrl.defaultZone = http://${EUREKA_HOST:localhost}:${EUREKA_PORT:7878}/eureka/

Create the file `bookrecedgeservice.properties`:

    server.port = ${PORT:8181}
    eureka.client.serviceUrl.defaultZone = http://${EUREKA_HOST:localhost}:${EUREKA_PORT:7878}/eureka/
    ribbon.eureka.enabled = true
    defaultBookId = -1
    defaultBookTitle = robots of dawn
    defaultBookAuthor = isaac asimov

Initialise the Git repository:

    git init
    git add .
    git commit -m "initial configuration"

Publish it to any remote repository online (replace the actual URL with your own repository):

    git remote add origin https://github.com/deors/deors-demos-microservices-configstore.git
    git push origin master

It is also possible to use the local repository, without pushing it to a remote repository. A local repository is enough to test services locally (Iteration 1), but a remote repository will be needed later, as services are distributed across multiple nodes and hence the local repository might not be accessible to every service instance.

### 1.2) Set up the configuration service

The configuration service, powered by Spring Cloud Config Server, is the microservice that will provide every other microservice in the system with the configuration settings they need at boot time.

Go to `https://start.spring.io/` and create the project with the following settings:

    group: deors.demos.microservices
    artifact: configservice
    depedencies:
        actuator
        config server

The actuator depedency, when added, enables useful endpoints to facilitate application operations.

Extract the generated zip to:

    ~/microservices

Change into extracted directory:

    cd ~/microservices/configservice

First, let's modify `pom.xml` file to upgrade the Java version to 11:

```xml
    <properties>
        ...
        <java.version>11</java.version>
        ...
    </properties>
```

Next, let's add the application name and set the configuration store location. Edit `src/main/resources/application.properties`:

    server.port = ${PORT:8888}
    spring.application.name = configservice
    spring.cloud.config.server.git.uri = ${CONFIG_REPO_URL:https://github.com/deors/deors-demos-microservices-configstore.git}
    management.security.enabled = false
    management.endpoints.web.exposure.include = *

If the configuration store is local, leverage the File protocol in Git:

    spring.cloud.config.server.git.uri = ${CONFIG_REPO_URL:file:///%HOME%/microservices/configstore}

To configure the configuration server to start automatically, edit `src/main/java/deors/demos/microservices/configservice/ConfigserviceApplication.java` and add the following class annotation:

```java
@org.springframework.cloud.config.server.EnableConfigServer
```

### 1.3) Set up the service registry and discovery service (Eureka)

The service registry and discovery service, powered by Spring Cloud and Netflix Eureka, is the microservice that will enable every other microservice in the system to register 'where' they are physically located, so others can discover them and interact with them.

Go to `https://start.spring.io/` and create the project with the following settings:

    group: deors.demos.microservices
    artifact: eurekaservice
    depedencies:
        actuator
        config client
        eureka server

Extract the generated zip to:

    ~/microservices

Change into extracted directory:

    cd ~/microservices/eurekaservice

First, let's modify `pom.xml` file to upgrade the Java version to 11:

```xml
    <properties>
        ...
        <java.version>11</java.version>
        ...
    </properties>
```

When using Java 9+, Spring Boot does not add JAX-B module (java.xml.bind) to Tomcat module path automatically, causing service registry service to fail on startup. In that case, the missing module should be added to Tomcat module path. When using `spring-boot:run` Maven goal to run the application, it is possible to use the `JDK_JAVA_OPTIONS` environment variable:

    set JDK_JAVA_OPTIONS=--add-modules java.xml.bind
    mvnw spring-boot:run

Or, when executing the fat Jar directly:

    java -jar target/<name-of-the-fat.jar> --add-modules java.xml.bind

But the preferred way to fix this is directly in `pom.xml` file by adding the dependencies with JAX-B API and implementation:

```xml
    <dependencies>
        ...
        <dependency>
            <groupId>javax.xml.bind</groupId>
            <artifactId>jaxb-api</artifactId>
            <version>2.3.1</version>
        </dependency>
        <dependency>
            <groupId>org.glassfish.jaxb</groupId>
            <artifactId>jaxb-runtime</artifactId>
            <version>2.3.1</version>
        </dependency>
        ...
    </dependencies>
```

To ensure that the configuration service is used, properties should be moved to bootstrap phase:

    mv src/main/resources/application.properties src/main/resources/bootstrap.properties

Edit `src/main/resources/bootstrap.properties`:

    spring.application.name = eurekaservice
    spring.cloud.config.uri = http://${CONFIG_HOST:localhost}:${CONFIG_PORT:8888}

To configure the Eureka server to start automatically, edit `src/main/java/deors/demos/microservices/eurekaservice/EurekaserviceApplication.java` and add the following class annotation:

```java
@org.springframework.cloud.netflix.eureka.server.EnableEurekaServer
```

### 1.4) Set up the circuit breaker dashboard service (Hystrix)

The circuit breaker dashboard, powered by Spring Cloud and Netflix Hystrix, will provide devs and ops teams with real-time views about service calls performance and failures including for example which of them are experimenting repeated failures. When that happens, Hystrix 'opens' the circuit and calls to a default safe method to keep the flow of information up and running smoothly, not causing cascading failures across services which may lead to major downtimes.

Go to `https://start.spring.io/` and create the project with the following settings:

    group: deors.demos.microservices
    artifact: hystrixservice
    depedencies:
        actuator
        config client
        eureka discovery
        hystrix dashboard

Extract the generated zip to:

    ~/microservices

Change into extracted directory:

    cd ~/microservices/hystrixservice

First, let's modify `pom.xml` file to upgrade the Java version to 11:

```xml
    <properties>
        ...
        <java.version>11</java.version>
        ...
    </properties>
```

To ensure that the configuration service is used, properties should be moved to bootstrap phase:

    mv src/main/resources/application.properties src/main/resources/bootstrap.properties

Edit `src/main/resources/bootstrap.properties`:

    spring.application.name = hystrixservice
    spring.cloud.config.uri = http://${CONFIG_HOST:localhost}:${CONFIG_PORT:8888}

To configure the Hystrix dashboard to start automatically, edit `src/main/java/deors/demos/microservices/hystrixservice/HystrixserviceApplication.java` and add the following class annotations:

```java
@org.springframework.cloud.client.discovery.EnableDiscoveryClient
@org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard
```

### 1.5) Set up the book recommendation service

This is the first microservice with actual functionality on the application functional domain. bookrec service is the service which provides methods to query, create, update and remove Book entities from the data store.

Go to `https://start.spring.io/` and create the project with the following settings:

    group: deors.demos.microservices
    artifact: bookrecservice
    depedencies:
        actuator
        config client
        eureka discovery
        web
        rest repositories
        rest repositories hal browser
        jpa
        h2

Extract the generated zip to:

    ~/microservices

Change into extracted directory:

    cd ~/microservices/bookrecservice

First, let's modify `pom.xml` file to upgrade the Java version to 11:

```xml
    <properties>
        ...
        <java.version>11</java.version>
        ...
    </properties>
```

To ensure that the configuration service is used, properties should be moved to bootstrap phase:

    mv src/main/resources/application.properties src/main/resources/bootstrap.properties

Edit `src/main/resources/bootstrap.properties`:

    spring.application.name = bookrecservice
    spring.cloud.config.uri = http://${CONFIG_HOST:localhost}:${CONFIG_PORT:8888}

To configure the service to be discoverable, edit `src/main/java/deors/demos/microservices/bookrecservice/BookrecserviceApplication.java` and add the class annotation:

```java
@org.springframework.cloud.client.discovery.EnableDiscoveryClient
```

Create the Book entity class:

```java
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String author;
}
```

Add bean constructors (including the default constructor and one that initalizes the three properties), getters, setters and toString method, or generate them with the IDE!

Create the BookRepository data access interface:

```java
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface BookRepository extends CrudRepository<Book, Long> {

    @Query("select b from Book b order by RAND()")
    List<Book> getBooksRandomOrder();
}
```

Create the BookController controller class:

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookController {

    @Autowired
    private BookRepository bookRepository;

    @RequestMapping("/bookrec")
    public Book getBookRecommendation() {
        return bookRepository.getBooksRandomOrder().get(0);
    }
}
```

Let's add some test data. For that, create the file `src/main/resources/import.sql` and populate it with some test data for the bookrec service:

```sql
insert into book(id, title, author) values (1, 'second foundation', 'isaac asimov')
insert into book(id, title, author) values (2, 'speaker for the dead', 'orson scott card')
insert into book(id, title, author) values (3, 'the player of games', 'iain m. banks')
insert into book(id, title, author) values (4, 'the lord of the rings', 'j.r.r. tolkien')
insert into book(id, title, author) values (5, 'the warrior apprentice', 'lois mcmaster bujold')
insert into book(id, title, author) values (6, 'blood of elves', 'andrzej sapkowski')
insert into book(id, title, author) values (7, 'harry potter and the prisoner of azkaban', 'j.k. rowling')
insert into book(id, title, author) values (8, '2010: odyssey two', 'arthur c. clarke')
insert into book(id, title, author) values (9, 'starship troopers', 'robert a. heinlein')
```

### 1.6) Set up the book recommendation edge service

The bookrec edge service is used by clients to interact with the bookrec service. As a general rule, services with business logic and access to data stores, should not be exposed directly to clients. Therefore, they are called inner services. Edge services will wrap access to inner services, as well as adding other non-functional capabilities, like routing, throttling, caching, data aggregation or fault tolerance.

Go to `https://start.spring.io/` and create the project with the following settings:

    group: deors.demos.microservices
    artifact: bookrecedgeservice
    depedencies:
        actuator
        config client
        eureka discovery
        hystrix
        ribbon
        web

Extract the generated zip to:

    ~/microservices

Change into extracted directory:

    cd ~/microservices/bookrecedgeservice

First, let's modify `pom.xml` file to upgrade the Java version to 11:

```xml
    <properties>
        ...
        <java.version>11</java.version>
        ...
    </properties>
```

To ensure that the configuration service is used, properties should be moved to bootstrap phase:

    mv src/main/resources/application.properties src/main/resources/bootstrap.properties

Edit `src/main/resources/bootstrap.properties`:

    spring.application.name = bookrecedgeservice
    spring.cloud.config.uri = http://${CONFIG_HOST:localhost}:${CONFIG_PORT:8888}

To configure the service to be discoverable (Eureka) and to use the circuit breaker (Hystrix), edit `src/main/java/deors/demos/microservices/bookrecedgeservice/BookrecedgeserviceApplication.java` and add the following class annotations:

```java
@org.springframework.cloud.client.discovery.EnableDiscoveryClient
@org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker
```

Next, add the `restTemplate()` method to initalize the RestTemplate object which will be used to invoke bookrecservice. Client-side load balancing (Ribbon) is enabled just by adding the corresponding annotation:

```java
@org.springframework.context.annotation.Bean
@org.springframework.cloud.client.loadbalancer.LoadBalanced
RestTemplate restTemplate() {
    return new RestTemplate();
}
```

Create the Book bean for the edge service, which is analogous to the Book bean for the inner service but without any persistence related code or configuration:

```java
public class Book {

    private Long id;

    private String title;

    private String author;
}
```

Add bean constructors (including the default constructor and one that initalizes the three properties), getters, setters and toString method, or generate them with the IDE!

Create the BookController controller for the edge service, including the call to bookrec through Hystrix and providing the default fallback method in case of problems with calls to bookrec:

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class BookController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${defaultBookId}")
    private long defaultBookId;

    @Value("${defaultBookTitle}")
    private String defaultBookTitle;

    @Value("${defaultBookAuthor}")
    private String defaultBookAuthor;

    @RequestMapping("/bookrecedge")
    @com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand(fallbackMethod = "getDefaultBook")
    public Book getBookRecommendation() {
        return restTemplate.getForObject("http://bookrecservice/bookrec", Book.class);
    }

    public Book getDefaultBook() {
        return new Book(defaultBookId, defaultBookTitle, defaultBookAuthor);
    }
}
```

At this point, executing a `mvn test` command ends in failure, as the default properties which values are autowired cannot be found by Spring while executing the application tests. Once all services are running, this will not be an issue as those properties will be provided by the configuration server, but there should be an alternate way to locate those values at test time.

For that, it is possible to add an application properties file in the test folder, specifically `src/test/resources/application.properties`. In this file, the properties needed to run the tests should be added:

    defaultBookId = -1
    defaultBookTitle = robots of dawn [test]
    defaultBookAuthor = isaac asimov [test]

It is also a good idea to add a 'flag' to let it be obvious that the property values are defined in the test application properties file.

### 1.7) Run the services locally

Services are now ready to be executed locally, using the sensible default configuration settings and the embedded runtimes provided by Spring Boot.

Each service will be run by executing this command in each folder:

    mvnw spring-boot:run

Alternatively, the Spring Boot fat Jar can be created and executed directly:

    mvnw package
    java -jar target/<name-of-the-fat.jar>

Follow the next sequence for running the services in order so they are able to leverage the configuration and registry services during startup:

    - configservice
    - eurekaservice
    - hystrixservice
    - bookrecservice
    - bookrecedgeservice

### 1.8) Test services locally

Once all the services are started, they will be available at the defined ports in the local host.

Access the configuration service through the actuator health endpoint (remember it is currently unsecured), to verify it is up & running and responding to requests normally:

    http://localhost:8888/actuator/health

Check that the configuration service is capable of returning the configuration for some of the services:

    http://localhost:8888/bookrecservice/default
    http://localhost:8888/eurekaservice/default

Check that Eureka service is up and the book recommendation service and edge service are registered:

    http://localhost:7878/

Check that Hystrix service is up and running:

    http://localhost:7979/hystrix

Access the HAL browser on the book recommendation service:

    http://localhost:8080/

Access the book recommendation service itself:

    http://localhost:8080/bookrec

Access the book recommendation edge service itself:

    http://localhost:8181/bookrecedge

To verify that Hystrix fault tolerance mechanism is working as expected, stop the book recommendation service, and access the book recommendation edge service again. The default recommended book should be returned instead and the application keeps working.

Go back to Hystrix dashboard and start monitoring the book recommendation edge service by registering the bookrec Hystrix stream in the dashboard (and optionally configuring the delay and page title):

     http://localhost:8181/actuator/hystrix.stream

Once the Hystric stream is registered, try again to access the edge service, with and without the inner service up and running, and experiment how thresholds (number of errors in a short period of time) impact the opening and closing of the circuit between the inner and the edge service.

## Iteration 2) Adding Docker images and running services in Docker Swarm

### 2.1) Set up Swarm

The following instructions will show how to create a simple one in VirtualBox, in the case that a swarm is not already available. In the case that a Docker Swarm is already available, skip to section 2.2.

In this setup, the swarm will be formed by three manager nodes, and three worker nodes, named:

    docker-swarm-manager-1
    docker-swarm-manager-2
    docker-swarm-manager-3
    docker-swarm-worker-1
    docker-swarm-worker-2
    docker-swarm-worker-3

The machines will be deployed in its own network:

    192.168.66.1/24

Being the first IP in the DHCP pool:

    192.168.66.100

To create each machine in VirtualBox, launch the following command setting the right machine name each time:

    docker-machine create --driver virtualbox --virtualbox-cpu-count 1 --virtualbox-memory 1024 --virtualbox-hostonly-cidr "192.168.66.1/24" <docker-machine-name>

Before beginning to configure the swarm, set the environment to point to the first machine. When in Windows, run this command:

    @FOR /f "tokens=*" %i IN ('docker-machine env docker-swarm-manager-1') DO @%i

When in Linux, run this command:

    eval $(docker-machine env docker-swarm-manager-1)

Next, to initialise a swarm the following command is used:

    docker swarm init --advertise-addr 192.168.66.100

Upon initialisation, the swarm exposes two tokens: one to add new manager nodes, one to add new worker nodes. The commands needed to get the tokens are:

    docker swarm join-token manager -q
    docker swarm join-token worker -q

With the tokens at hand, change the environment to point to each machine, every manager and worker nodes. When in Windows, run this command:

    @FOR /f "tokens=*" %i IN ('docker-machine env <docker-machine-name>') DO @%i

When in Linux, run this command:

    eval $(docker-machine env <docker-machine-name>)

And use the swarm join command in each node as it corresponds to a manager or a worker:

    docker swarm join --token <manager-or-worker-token> 192.168.66.100:2377

Once it is ready, the swarm can be stopped with the following command:

    docker-machine stop docker-swarm-manager-1 docker-swarm-manager-2 docker-swarm-manager-3 docker-swarm-worker-1 docker-swarm-worker-2 docker-swarm-worker-3

And to start it again, this command:

    docker-machine start docker-swarm-manager-1 docker-swarm-manager-2 docker-swarm-manager-3 docker-swarm-worker-1 docker-swarm-worker-2 docker-swarm-worker-3

### 2.2) Update Eureka configuration to leverage internal Swarm network

When services are deployed inside Docker Swarm, there are multiple networks active in the running container. To be able to use correctly the client-side load balancing, each running instance must register in Eureka with the IP address corresponding to the internal network (and not the ingress network).

Move to bookrecservice folder, edit bootstrap.properties and add the following configuration lines:

    spring.cloud.inetutils.preferredNetworks[0] = 192.168
    spring.cloud.inetutils.preferredNetworks[1] = 10.0

Also move to bookrecedgeservice folder, edit bootstrap.properties and add the same lines:

    spring.cloud.inetutils.preferredNetworks[0] = 192.168
    spring.cloud.inetutils.preferredNetworks[1] = 10.0

Finally, do the same for eurekaservice and hystrixservice. In general, this configuration must be added in every service which is going to register in Eureka.

With these changes, the services will register in Eureka with the right IP address, both when they are running standalone (192.168 network) and when they are running inside Docker Swarm (10.0 network).

### 2.3) Configure Docker image build in Maven and create the Dockerfiles

In this section, pom files will be configured with Spotify's Docker Maven plugin, and Dockerfile files will be created, to allow each service to run as a Docker image. This process must be done for each of the microservices in the stack.

Let's proceed with bookrec service as an example. Change to its directory:

    cd ~/microservices/bookrecservice

Edit `pom.xml` and add inside `<properties>` the following property:

```xml
    <properties>
        ...
        <docker.image.prefix>deors</docker.image.prefix>
        ...
    </properties>
```

This property will be used to identify the registry organisation where the generated images will be published. Hence, `deors` should be replaced by any organisation name in which there are permissions to publish images.

Add inside `<build>`the following configuration to make the generated Jar file name to not contain the version string. That will help make the Dockerfile not dependant on the artefact version.

```xml
    <build>
        ...
        <finalName>${project.artifactId}</finalName>
        ...
    </build>
```

Add inside `<build><plugins>` Spotify's Docker Maven plugin configuration:

```xml
    <build>
        ...
        <plugins>
            ...
            <plugin>
                <groupId>com.spotify</groupId>
                <artifactId>docker-maven-plugin</artifactId>
                <version>1.2.0</version>
                <configuration>
                    <dockerDirectory>${project.basedir}</dockerDirectory>
                    <imageName>${docker.image.prefix}/${project.name}</imageName>
                    <imageTags>
                        <imageTag>${project.version}</imageTag>
                        <imageTag>latest</imageTag>
                    </imageTags>
                    <serverId>docker-hub</serverId>
                </configuration>
            </plugin>
            ...
        </plugins>
        ...
    </build>
```

The server id in Spotify's plugin configuration must match an existing credential in Maven's settings. This is the credential that will be used to publish new and updated images.

Create the file `Dockerfile` and add the following content:

```dockerfile
    FROM adoptopenjdk/openjdk11:jdk-11.0.2.9
    VOLUME /tmp
    ADD target/bookrecservice.jar app.jar
    ENTRYPOINT exec java $JAVA_OPTS -jar /app.jar
```

Repeat for the other microservices. Don't forget to update Jar file name in ADD command, using for each one the `artifactId`, matching the configured `finalName` in `pom.xml`.

### 2.4) Create the images

To create the images, a Docker host is needed. If using Docker for Windows, Linux or Mac, or boot2docker (VirtualBox), that Docker host is sufficient. It is also possible to use the swarm created before (one machine is enough). Let's start the swarm:

    docker-machine start docker-swarm-manager-1 docker-swarm-manager-2 docker-swarm-manager-3 docker-swarm-worker-1 docker-swarm-worker-2 docker-swarm-worker-3

Configure Docker client to work with one of the machines. Wwhen in Windows, run this command:

    @FOR /f "tokens=*" %i IN ('docker-machine env docker-swarm-manager-1') DO @%i

When in Linux, run this command:

    eval $(docker-machine env docker-swarm-manager-1)

Build and push images by running this command for each microservice:

    mvn package docker:build -DpushImage

Images must be pushed so they are available to all machines in the swarm, and not only in the machine used to build the image.

### 2.5) Run the images as services in Swarm

The first step is to create an overlay network for all the services:

    docker network create -d overlay microdemonet

Once the network is ready, launch configservice and check the status:

    docker service create -p 8888:8888 --name configservice --network microdemonet deors/deors-demos-microservices-configservice:latest
    docker service ps configservice

Next, launch eurekaservice and check the status:

    docker service create -p 7878:7878 --name eurekaservice --network microdemonet -e "CONFIG_HOST=configservice" deors/deors-demos-microservices-eurekaservice:latest
    docker service ps eurekaservice

Once eurekaservice is ready to accept other service registrations, launch hystrixservice and check the status:

    docker service create -p 7979:7979 --name hystrixservice --network microdemonet -e "CONFIG_HOST=configservice" -e "EUREKA_HOST=eurekaservice" deors/deors-demos-microservices-hystrixservice:latest
    docker service ps hystrixservice

Launch bookrecservice and check the status:

    docker service create -p 8080:8080 --name bookrecservice --network microdemonet -e "CONFIG_HOST=configservice" -e "EUREKA_HOST=eurekaservice" deors/deors-demos-microservices-bookrecservice:latest
    docker service ps bookrecservice

And, finally, launch bookrecedgeservice and check the status:

    docker service create -p 8181:8181 --name bookrecedgeservice --network microdemonet -e "CONFIG_HOST=configservice" -e "EUREKA_HOST=eurekaservice" deors/deors-demos-microservices-bookrecedgeservice:latest
    docker service ps bookrecedgeservice

To quickly check whether all services are up and their configuration, use this command:

    docker service ls

### 2.6) Test services in Swarm

Once all the services are started, they will be available at the defined ports in the local host.

Access the configuration service through the actuator health endpoint (remember it is currently unsecured), to verify it is up & running and responding to requests normally:

    http://localhost:8888/actuator/health

Check that the configuration service is capable of returning the configuration for some of the services:

    http://192.168.66.100:8888/bookrecservice/default
    http://192.168.66.100:8888/eurekaservice/default

Check that Eureka service is up and the book recommendation service and edge service are registered:

    http://192.168.66.100:7878/

Check that Hystrix service is up and running:

    http://192.168.66.100:7979/hystrix

Access the HAL browser on the book recommendation service:

    http://192.168.66.100:8080/

Access the book recommendation service iself:

    http://192.168.66.100:8080/bookrec

Access the book recommendation edge service itself:

    http://192.168.66.100:8181/bookrecedge

To verify that Hystrix fault tolerance mechanism is working as expected, stop the book recommendation service, and access the book recommendation edge service again. The default recommended book should be returned instead and the application keeps working.

Go back to Hystrix dashboard and start monitoring the book recommendation edge service by registering the bookrec Hystrix stream in the dashboard (and optionally configuring the delay and page title):

     http://192.168.66.100:8181/actuator/hystrix.stream

Once the Hystric stream is registered, try again to access the edge service, with and without the inner service up and running, and experiment how thresholds (number of errors in a short period of time) impact the opening and closing of the circuit between the inner and the edge service.

### 2.7) Scale out the book recommendation service

Ask Docker to scale out the book recommendation service

    docker service scale bookrecservice=3

### 2.8) Make and update and roll out the changes without service downtime

Make some change and deploy a rolling update. For example change the text string returned by BookController class in file `src/main/java/deors/demos/microservices/BookController.java`:

Rebuild and push the new image to the registry:

    mvn package docker:build -DpushImage

Next, the change is deployed. A label is needed to ensure the new version of 'latest' image is downloaded from registry:

    docker service update --container-label-add update_cause="change" --update-delay 30s --image deors/deors-demos-microservices-bookrecservice:latest bookrecservice

To check how the change is being deployed, issue this command repeatedly:

    docker service ps bookrecservice

## Iteration 3) Configuring continuous integration pipelines with Jenkins

### 3.1) The anatomy of a Jenkins pipeline

A Jenkins pipeline, written in the form of a declarative pipeline with a rich DSL and semantics, the *Jenkinsfile*, is a model for any process, understood as a sucession of stages and steps, sequential, parallel or any combinatiof both. In this context, the process is a build process, following the principles of continuous integration, continuous code inspection and continuous testing (continuous integration pipeline, for short).

Jenkins pipelines are written in Groovy, and the pipeline DSL is designed to be pluggable, so any given plugin may contribute with its own idioms to the pipeline DSL, as well as extended through custom functions bundled in Jenkins libraries.

The combination of a powerful dynamic language as Groovy, with the rich semantics of the available DSLs, allows developers to write simple, expressive pipelines, while having all freedom to customize the pipeline behavior up to the smallest detail.

The pipeline main construct is the `pipeline` block element. Inside any `pipeline` element there will be any number of second-level constructs, being the main ones:

- `agent`: Used to define how the pipeline will be executed. For example, in a specific slave or in a container created from an existing Docker image.
- `environment`: Used to define pipeline properties. For example, define a container name from the given build number, or define a credential password by reading its value from Jenkins credentials manager.
- `stages`: The main block, where all stages and steps are defined.
- `post`: Used to define any post-process activities, like resource cleaning or results publishing.

Inside the `stages` element, there will be nested at least one `stage` element, each stage with a given name. Inside each `stage` element, typically there will be one `steps` element, although other elements can be there too, for example when stage-specific configuration is needed, or to model parallel execution of certain steps.

In a nutshell, the following is a skeleton of a typical Jenkins pipeline:

```groovy
#!groovy

pipeline {
    agent {
        // how the pipeline will be built
    }

    environment {
        // properties or environment variables, new or derived
    }

    stages {
        stage('stage-1-name') {
            steps {
                // steps for stage 1 come here
            }
        }

        ...

        stage('stage-n-name') {
            steps {
                // steps for stage n come here
            }
        }
    }

    post {
        // post-process activities, e.g. cleanup or publish
    }
}
```

Typical steps include the following:

- `echo`: Step to... echo stuff to the console.
- `sh`: Used to execute any command. Probably the most common one.
- `junit`: Used to publish results of unit test execution with JUnit.
- `archiveArtifacts`: Used to archive any artifact produced during the build.
- `script`: As the name suggest, it is used to contain any arbitrary block of Groovy code.

With the building blocks just explained, as well as others, it is possible to model any continuous integration process.

### 3.2) Verification activities along the pipeline

An effective continuous integration pipeline must have sufficient verification steps as to give confidence in the process. Verification steps will include code inspection, and testing:

Code inspection activities are basically three:

- **Gathering of metrics**: Size, complexity, code duplications, and others related with architecture and design implementation.
- **Static code profiling**: Analysis of sources looking for known patterns that may result in vulnerabilities, reliability issues, performance issues, or affect maintainability.
- **Dependency analysis**: Analysis of dependency manifests (e.g. those included in `pom.xml` or `require.js` files), looking for known vulnerabilities in those dependencies, as published in well-known databases like CVE.

Testing activities will include the following:

- **Unit tests**.
- **Unit-integration tests**: Those that, although not requiring the application to be deployed, are testing multiple components together. For example, in-container tests.
- **Integration tests**: Including in this group all those kinds of tests that require the application to be deployed. Typically external dependencies will be mocked up in this step. Integration tests will include API tests and UI tests.
- **Performance tests**: Tests verifying how the service or component behaves under load. Performance tests in this step are not meant to assess the overall system capacity (which can be virtually infinite with the appropriate scaling patterns), but to assess the capacity of one node, uncover concurrence issues due to the parallel execution of tasks, as well as to pinpoint possible bottlenecks or resource leaks when studying the trend. Very useful at this step to leverage APM tools to gather internal JVM metrics, e.g. to analyze gargabe collection.
- **Security tests**: Tests assessing possible vulnerabilities exposed by the application. In this step, the kind of security tests performed are typically DAST analysis.

In addition to the previous kinds of tests, there is one more which is meant to assess the quality of tests:

- **Mutation tests**: Mutation testing, usually executed only on unit tests for the sake of execution time, is a technique that identifies changes in source code, the so called mutations, applies them and re-execute the corresponding unit tests. If after a change in the source code, unit tests do not fail, that means that either the test code does not have assertions, or there are assertions but test coverage is unsufficient (typically test cases with certain conditions not tested). Mutation testing will uncover untested test cases, test cases without assertions and test cases with insufficient or wrong assertions.

To enable these tools along the lifecycle, and to align developer workstation usage with CI server pipeline usage, the recommended approach is to configure these activities with the appropriate tools in Maven's `pom.xml`, storing the corresponding test scripts, data and configuration, along with the source code in the `src/test` folder (very commonly done for unit tests, and also recommended for the other kinds of tests).

### 3.3) Upgrading JUnit to version 5

Unit tests are already configured by default in Spring Boot thanks to the addition of the `spring-boot-starter-test` dependency. Unit tests are configured to run with JUnit 4, so we will upgrade the configuration to leverage JUnit 5.

To enable JUnit 5, it is needed to suppress the dependency on JUnit 4, and add the newer version to `pom.xml`:

```xml
    <dependencies>
        ...
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
            <exclusions>
                <exclusion>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <version>5.3.2</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <version>5.3.2</version>
            <scope>test</scope>
        </dependency>
        ...
    </dependencies>
```

### 3.4) Adapting existing unit tests to work with JUnit 5

The upgrade to JUnit 5 requires to adapt the existing unit tests - the ApplicationTests automatically generated by Spring Initializr - to be compatible with the new Jupiter API which is the new default in JUnit 5. This upgrade must be done in all the existing tests in all the projects.

Let's proceed with bookrec service as an example. Change to its directory:

    cd ~/microservices/bookrecservice

Edit `src/test/java/deors/demos/microservices/bookrecservice/BookrecserviceApplicationTests.java`, modify the `Test` class package, and replace the JUnit 4 `Runner` with a JUnit 5 `Extension`:

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class BookrecserviceApplicationTests {

    @Test
    public void contextLoads() {
    }
}
```

The same upgrade process must be done with the other microservices in the system.

### 3.5) Adding JaCoCo agent to gather code coverage metrics during tests

One of the actions to be done along the pipeline, is to enable code coverage metrics when unit tests and integration tests are executed. To do that, there are a few actions needed in preparation for the task.

First, the JaCoCo agent must be added as a Maven dependency in `pom.xml`:

```xml
    <dependencies>
        ...
        <dependency>
            <groupId>org.jacoco</groupId>
            <artifactId>org.jacoco.agent</artifactId>
            <version>0.8.3</version>
            <classifier>runtime</classifier>
            <scope>test</scope>
        </dependency>
        ...
    </dependencies>
```

To enable the gathering of code coverage metrics during unit tests, the agent provides a goal to prepare the needed JVM argument. Another possible approach, to ensure that the agent is always enabled, is to pass the JVM argument directly to Surefire plugin:

```xml
    <build>
        ...
        <plugins>
            ...
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>2.22.1</version>
                <configuration>
                    <argLine>-javaagent:${settings.localRepository}/org/jacoco/org.jacoco.agent/0.8.3/org.jacoco.agent-0.8.3-runtime.jar=destfile=${project.build.directory}/jacoco.exec</argLine>
                    <excludes>
                        <exclude>**/*IntegrationTest.java</exclude>
                    </excludes>
                </configuration>
            </plugin>
            ...
        </plugins>
        ...
    </build>
```

For integration tests, the code coverage setup is a bit more complicated. Instead of enabling the agent in the test executor, it is the test server the process that must have the agent enabled. The former approach works for unit tests because the same JVM process holds both the test code and the code for the application being tested. However for integration tests, the test execution is a separate process from the application being tested.

As the application is packaged and runs as a Docker image, the agent file must be present at the image build time. Later, during the execution of integration tests, the JaCoCo CLI tool will be needed to dump the coverage data from the test server. To do that, both dependencies will be copied into the expected folder with the help of the Maven Dependency plugin:

```xml
    <build>
        ...
        <plugins>
            ...
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-dependency-plugin</artifactId>
                <version>3.1.1</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>copy</goal>
                        </goals>
                        <configuration>
                            <artifactItems>
                                <artifactItem>
                                    <groupId>org.jacoco</groupId>
                                    <artifactId>org.jacoco.agent</artifactId>
                                    <version>0.8.3</version>
                                    <classifier>runtime</classifier>
                                    <destFileName>jacocoagent.jar</destFileName>
                                </artifactItem>
                                <artifactItem>
                                    <groupId>org.jacoco</groupId>
                                    <artifactId>org.jacoco.cli</artifactId>
                                    <version>0.8.3</version>
                                    <classifier>nodeps</classifier>
                                    <destFileName>jacococli.jar</destFileName>
                                </artifactItem>
                            </artifactItems>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            ...
        </plugins>
        ...
    </build>
```

And finally, the JaCoCo agent needs to be copied into the Docker image. Edit the file `Dockerfile` and add a new `ADD` instruction after `VOLUME`:

```dockerfile
    ...
    VOLUME /tmp
    ADD target/dependency/jacocoagent.jar jacocoagent.jar
    ...
```

### 3.6) Configuring Failsafe for integration test execution

Although Maven Surefire plugin is enabled by default, Failsafe, the Surefire twin for integration tests, is disabled by default. To enable Failsafe, its targets must be called explicitely or alternatively may be binded to the corresponding lifecycle goals. For the microservices pipeline is preferred to have it disabled by default:

```xml
    <build>
        ...
        <plugins>
            ...
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
                <version>2.22.1</version>
                <configuration>
                    <includes>
                        <include>**/*IntegrationTest.java</include>
                    </includes>
                </configuration>
                <!-- if activated, will run failsafe automatically on integration-test and verify goals -->
                <!--<executions>
                    <execution>
                        <goals>
                            <goal>integration-test</goal>
                            <goal>verify</goal>
                        </goals>
                    </execution>
                </executions>-->
            </plugin>
            ...
        </plugins>
        ...
    </build>
```

In addition to the optional automatic activation of Failsafe, the configuration includes the execution filter: the pattern to recognize which test classes are integration tests vs. unit tests.

### 3.7) Adding performance tests with Apache JMeter

The next addition to the project configuration is the addition of performance tests with Apache JMeter.

Besides the addition of the plugin, and optionally enabling the automatic execution of the plugin targets, the configuration will include three properties that will be injected into the scripts. Those properties - host, port, context root - are needed so the script can be executed regardless of where the application being tested is exposed, which is usually only known at runtime when the container is run:

```xml
    <build>
        ...
        <plugins>
            ...
            <!-- performance tests -->
            <plugin>
                <groupId>com.lazerycode.jmeter</groupId>
                <artifactId>jmeter-maven-plugin</artifactId>
                <version>2.8.5</version>
                <configuration>
                    <testResultsTimestamp>false</testResultsTimestamp>
                    <propertiesUser>
                        <host>${jmeter.target.host}</host>
                        <port>${jmeter.target.port}</port>
                        <root>${jmeter.target.root}</root>
                    </propertiesUser>
                </configuration>
                <!-- if activated, will run jmeter automatically on integration-test and verify goals -->
                <!-- <executions>
                    <execution>
                        <phase>integration-test</phase>
                        <goals>
                            <goal>jmeter</goal>
                            <goal>results</goal>
                        </goals>
                    </execution>
                </executions> -->
            </plugin>
            ...
        </plugins>
        ...
    </build>
```

### 3.8) Creating brand new unit tests

To ensure that all services are behaving as expected, unit tests validating the behavior should be added. Application tests created by Spring Initializr are insufficient, as they just validate that the container start and all beans are properly wired and configured.

It is not in the scope of this guide to explain how to write the unit tests, but as they are useful, and needed for the pipeline to be valuable, the repository code includes unit tests for bookrecservice and bookrecedgeservice.

### 3.9) Creating brand new integration and performance tests

Similarly to unit tests, both integration and performance tests are required for the pipeline to be valuable.

Again, the repository code includes integration (API) tests and performance tests written with JMeter.

### 3.10) Configuring mutation testing

The next step is to configure mutation testing with Pitest.

As mutation testing works better with strict unit tests, the plugin configuration should exclude application (in-container) tests and integration tests. If left enabled, mutation testing is likely to take a very long time to finish, and results obtained are likely to not be useful at all.

```xml
    <build>
        ...
        <plugins>
            ...
            <!-- mutation tests -->
            <plugin>
                <groupId>org.pitest</groupId>
                <artifactId>pitest-maven</artifactId>
                <version>1.4.5</version>
                <configuration>
                    <excludedTestClasses>
                        <param>*ApplicationTests</param>
                        <param>*IntegrationTest</param>
                    </excludedTestClasses>
                    <outputFormats>
                        <outputFormat>XML</outputFormat>
                    </outputFormats>
                </configuration>
                <!-- enable support for JUnit 5 in Pitest -->
                <dependencies>
                    <dependency>
                        <groupId>org.pitest</groupId>
                        <artifactId>pitest-junit5-plugin</artifactId>
                        <version>0.8</version>
                    </dependency>
                </dependencies>
                <!-- if activated, will run pitest automatically on integration-test goal -->
                <!--<executions>
                    <execution>
                        <goals>
                            <goal>mutationCoverage</goal>
                        </goals>
                    </execution>
                </executions>-->
            </plugin>
            ...
        </plugins>
        ...
    </build>
```

Due to how Pitest plugin works, it will fail when there are no mutable tests i.e. no strict unit tests. Considering this, the pipelines corresponding to services without mutable tests should skip the execution of Pitest.

### 3.11) Configuring dependency vulnerability tests with OWASP

OWASP is a global organization focused on secure development practices. OWASP also owns several open source tools, including OWASP Dependency Check. Dependency Check scans dependencies from a project manifest, like the `pom.xml` file, and checks them with the online repository of known vulnerabilities (CVE, maintained by NIST), for every framework artefact, and version.

```xml
    <build>
        ...
        <plugins>
            ...
            <plugin>
                <groupId>org.owasp</groupId>
                <artifactId>dependency-check-maven</artifactId>
                <version>3.3.2</version>
                <configuration>
                    <format>ALL</format>
                </configuration>
            </plugin>
            ...
        </plugins>
        ...
    </build>
```

To ensure that unsecure vulnerabilities are not carried onto a live environment, the configuration may include the setting to fail builds in case of vulnerabilities detected of higher severity:

```xml
            ...
            <plugin>
                <groupId>org.owasp</groupId>
                <artifactId>dependency-check-maven</artifactId>
                <version>3.3.2</version>
                <configuration>
                    <format>ALL</format>
                    <failBuildOnCVSS>5</failBuildOnCVSS>
                </configuration>
            </plugin>
            ...
```

### 3.12) Orchestrating the build - the continuous integration pipeline

The stages that are proposed as a best practice, are the following:

- **Environment preparation**: This stage is used to get and configure all needed dependencies. Typically in a Java with Maven or Gradle pipeline, it is skipped as Maven and Gradle handle dependency resolution and acquisition (download from central, project/organization repository, local cache) as needed. In a Python with pip pipeline, this stage will mean the execution of `pip install` command, and similarly in a JavaScript with npm pipeline, `npm install` command.
- **Compilation**: This stage is used to transform source code to binaries, or in general to transform source code into the final executable form, including transpilation, uglyfication or minification. For interpreted languages, whenever possible this stage should also include checking for syntax errors.
- **Unit tests**: This stage is used to execute unit tests (understood as tests which do not need the application to be installed or deployed, like unit-integration tests). Along with test execution, the stage should also gather code coverage metrics.
- **Mutation tests**: This stage is used to run mutation testing to measure how thorough (and hence useful) automated unit tests are.
- **Package**: This stage is used to package all application resources that are required at runtime, for example: binaries, static resources, and application configuration that does not depend on the environment. As a best practice, all environment specific configuration must be externalized (package-one-run-everywhere).
- **Build Docker image**: This stage will create the application image by putting together all pieces required: a base image, the build artifacts that were packaged in the previous stage, and any dependencies needed (e.g. third-party libraries).
- **Run Docker image**: This stage will prepare the test environment for the following stages. This stage tests whether the application actually runs and then makes it available for the tests.
- **Integration tests**: This stage will execute integration tests on the test environment just provisioned. As with unit tests, code coverage metrics should be gathered.
- **Performance tests**: This stage will run tests to validate the application behavior under load. This kind of tests, although provides with useful information on response times, are better used to uncover any issue due to concurrent usage.
- **Dependency vulnerability tests**: This stage is used to assess the application vulnerabilities and determine whether there are known security vulnerabilities which should prevent the application to be deployed any further.
- **Code inspection**: This stage is used to run static code analysis and gather useful metrics (like object-oriented metrics). Typically this stage will also include observations from previous stages to calculate the final quality gate for the build.
- **Push Docker image**: The final stage, if all quality gates are passed, is to push the image to a shared registry, from where it is available during tests to other applications that depend on this image, as well as to be promoted to stage or production environments.

Once the pipeline to be created is known, it is the time of putting together all the pieces and commands needed to execute every activity.

### 3.13) The pipeline code: Configuring the build execution environment

First, the pipeline is opened with the agent to be used for the build execution, and the build properties that will be leveraged later during stage definition, to make stages reusable for every microservice in the system. As an example, let's create the pipeline for the configuration service:

```groovy
#!groovy

pipeline {
    agent {
        docker {
            image 'adoptopenjdk/openjdk11:jdk-11.0.2.9'
            args '--network ci'
        }
    }

    environment {
        ORG_NAME = "deors"
        APP_NAME = "deors-demos-microservices-configservice"
        APP_CONTEXT_ROOT = "/"
        APP_LISTENING_PORT = "8888"
        TEST_CONTAINER_NAME = "ci-${APP_NAME}-${BUILD_NUMBER}"
        DOCKER_HUB = credentials("${ORG_NAME}-docker-hub")
    }
    ...
}
```

The network used to create the builder container should be the same where the test container is launched, to ensure that integration and performance tests, that are executed from the builder container, have connectivity with the application under test. That network must exist in the Docker machine or Docker Swarm cluster before the builds are launched.

The property `DOCKER_HUB` will hold the value of the credentials needed to push images to Docker Hub (or to any other Docker registry). The credentials are stored in Jenkins credential manager, and injected into the pipeline with the `credentials` function. This is a very elegant and clean way to inject credentials as well as any other secret, without hard-coding them (and catastrophically storing them in version control).

As the build is currently configured, it will run completely clean every time, including the acquisition of dependencies by Maven. In those cases in which it is not advisable to download all dependencies in every build, for example because build execution time is more critical than ensuring that dependencies remain accessible, builds can be accelerated by caching dependencies (the local Maven repository) in a volume:

```groovy
    ...
    agent {
        docker {
            image 'adoptopenjdk/openjdk11:jdk-11.0.2.9'
            args '--network ci --mount type=volume,source=ci-maven-home,target=/root/.m2'
        }
    }
    ...
```

### 3.1415926) The pipeline code: Compilation, unit tests, mutation tests and packaging

The first four stages will take care of compilation, unit tests, muration tests and packaging tasks:

```groovy
    ...
    stages {
        stage('Compile') {
            steps {
                echo "-=- compiling project -=-"
                sh "./mvnw clean compile"
            }
        }

        stage('Unit tests') {
            steps {
                echo "-=- execute unit tests -=-"
                sh "./mvnw test"
                junit 'target/surefire-reports/*.xml'
                jacoco execPattern: 'target/jacoco.exec'
            }
        }

        stage('Mutation tests') {
            steps {
                echo "-=- execute mutation tests -=-"
                sh "./mvnw org.pitest:pitest-maven:mutationCoverage"
            }
        }

        stage('Package') {
            steps {
                echo "-=- packaging project -=-"
                sh "./mvnw package -DskipTests"
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
        ...
    }
    ...
```

It's worth noting that as JaCoCo agent is already configured in `pom.xml` file, running the `test` goal in Maven will also gather the code coverage metrics.

As explained before, Pitest will fail when there are no mutable tests i.e. no strict unit tests. Considering this, the pipelines for configservice, eurekaservice and hystrixservice should skip the execution of Pitest.

```groovy
    ...
    stages {
        ...
        stage('Mutation tests') {
            steps {
                echo "-=- skipped as there are no mutable unit tests -=-"
                //echo "-=- execute mutation tests -=-"
                //sh "./mvnw org.pitest:pitest-maven:mutationCoverage"
            }
        }
        ...
    }
    ...
```

Of course, bookrecservice, bookrecesgeservice, as well as any other application service, should have muration testing enabled in the pipeline.

### 3.15) The pipeline code: Build the Docker image and provision the test environment

The next two stages will build the Docker image and provision the test environment by running it:

```groovy
    ...
    stages {
        ...
        stage('Build Docker image') {
            steps {
                echo "-=- build Docker image -=-"
                sh "./mvnw docker:build"
            }
        }

        stage('Run Docker image') {
            steps {
                echo "-=- run Docker image -=-"
                sh "docker run --name ${TEST_CONTAINER_NAME} --detach --rm --network ci --expose ${APP_LISTENING_PORT} --expose 6300 --env JAVA_OPTS='-Dserver.port=${APP_LISTENING_PORT} -Dspring.profiles.active=ci -javaagent:/jacocoagent.jar=output=tcpserver,address=*,port=6300' ${ORG_NAME}/${APP_NAME}:latest"
            }
        }
        ...
    }
    ...
```

When the test environment is created, there are some important notes to take into consideration.

The network used to run the container, as explained before, should be the same in which the build is running. This way, there is network visibility and the network DNS can be use to resolve easily where the test container is running.

The test container name is set to include the build number, which allows for parallel execution of builds, i.e. when rapid feedback is required for every commit.

The port in which the application listens is configured with a property which is used consistently to ensure that connectivity works fine, i.e. the server starts in a known port, that port is exposed (but not needed to be published outside the network), and later, the test executors point to that very same port.

The application runs with a given Spring profile activated. This is used to inject test environment specific configuration properties, for example settings that would be pulled from configservice which is not available during this test phase.

The application runs with JaCoCo agent activated and listening in port 6300. Later during integration tests, the build will connect to that port to dump code coverage information from the server.

### 3.16) The pipeline code: Running integration and performance tests

The following two steps will execute the integration and performance tests, once the application is deployed and available in the test environment:

```groovy
    ...
    stages {
        ...
        stage('Integration tests') {
            steps {
                echo "-=- execute integration tests -=-"
                sh "curl --retry 5 --retry-connrefused --connect-timeout 5 --max-time 5 http://${TEST_CONTAINER_NAME}:${APP_LISTENING_PORT}/"
                sh "./mvnw failsafe:integration-test failsafe:verify -DargLine=\"-Dtest.target.server.url=http://${TEST_CONTAINER_NAME}:${APP_LISTENING_PORT}/${APP_CONTEXT_ROOT}\""
                sh "java -jar target/dependency/jacococli.jar dump --address ${TEST_CONTAINER_NAME} --port 6300 --destfile target/jacoco-it.exec"
                junit 'target/failsafe-reports/*.xml'
                jacoco execPattern: 'target/jacoco-it.exec'
            }
        }

        stage('Performance tests') {
            steps {
                echo "-=- execute performance tests -=-"
                sh "./mvnw jmeter:jmeter jmeter:results -Djmeter.target.host=${TEST_CONTAINER_NAME} -Djmeter.target.port=${APP_LISTENING_PORT} -Djmeter.target.root=${APP_CONTEXT_ROOT}"
                perfReport sourceDataFiles: 'target/jmeter/results/*.csv'
            }
        }
        ...
    }
    ...
```

There a few outstanding pieces that are worth noting.

Before the integration tests are launched, it is good idea to ensure that the application is fully initialised and responding. The Docker run command will return once the container is up, but this does not mean that the application is up and running and able to respond to requests. With a simple `curl` command it is possible to configure the pipeline to wait for the application to be available.

Integration and performance tests are executing by passing as a parameter the root URL where the application is to be found, including the test container name that will be resolved thanks to the network DNS, and the configured port.

Code coverage information is being gathered in the test container. Therefore, to have it available for the quality gate and report publishing, the JaCoCo CLI `dump` command is executed. The JaCoCo CLI is available in the `target/dependency` folder as it was configured before with the help of the Maven dependency plugin.

For performance tests, it is possible to include a quality gate in the `perfReport` function, causing the build to fail if any of the thresholds are not passed, as well as flagging a build as unstable. As an example, this is a quality gate flagging the build as unstable in case of at least one failed request or if average response time exceeds 100 ms, and failing the build if there are 5% or more of failing requests.

```groovy
        ...
        stage('Performance tests') {
            steps {
                echo "-=- execute performance tests -=-"
                sh "./mvnw jmeter:jmeter jmeter:results -Djmeter.target.host=${TEST_CONTAINER_NAME} -Djmeter.target.port=${APP_LISTENING_PORT} -Djmeter.target.root=${APP_CONTEXT_ROOT}"
                perfReport sourceDataFiles: 'target/jmeter/results/*.csv', errorUnstableThreshold: 0, errorFailedThreshold: 5, errorUnstableResponseTimeThreshold: 'default.jtl:100'
            }
        }
        ...
```

### 3.17) The pipeline code: Dependency vulnerability tests, code inspection and quality gate

The next two stages will check dependencies for known security vulnerabilities, and execute code inspection with SonarQube (and tools enabled through plugins), including any compound quality gate defined in SonarQube for the technology or project:

```groovy
    ...
    stages {
        ...
        stage('Dependency vulnerability tests') {
            steps {
                echo "-=- run dependency vulnerability tests -=-"
                sh "./mvnw dependency-check:check"
                dependencyCheckPublisher
            }
        }

        stage('Code inspection & quality gate') {
            steps {
                echo "-=- run code inspection & check quality gate -=-"
                withSonarQubeEnv('ci-sonarqube') {
                    sh "./mvnw sonar:sonar"
                }
                timeout(time: 10, unit: 'MINUTES') {
                    //waitForQualityGate abortPipeline: true
                    script {
                        def qg = waitForQualityGate()
                        if (qg.status != 'OK' && qg.status != 'WARN') {
                            error "Pipeline aborted due to quality gate failure: ${qg.status}"
                        }
                    }
                }
            }
        }
        ...
    }
    ...
```

For dependency check, it is possible to include a quality gate in the `dependencyCheckPublisher` function, causing the build to fail if any of the thresholds are not passed, as well as flagging a build as unstable. As an example, this is a quality gate flagging the build as unstable in case of more than one high severity or more than 5 normal severity issues, and failing the build if there are at least one high severity or more than 2 normal severity issues.

```groovy
        ...
        stage('Dependency vulnerability tests') {
            steps {
                echo "-=- run dependency vulnerability tests -=-"
                sh "./mvnw dependency-check:check"
                dependencyCheckPublisher failedTotalHigh: '0', unstableTotalHigh: '1', failedTotalNormal: '2', unstableTotalNormal: '5'
            }
        }
        ...
```

It's worth noting that the code analysis and calculation of the quality gate by SonarQube is an asynchronous proces. Depending on SonarQube server load, it might take some time for results to be available, and as a design decision, the `sonar:sonar` goal will not wait, blocking the build, until then. This has the beneficial side effect that the Jenkins executor is not blocked and other builds might be built in the meantime, maximizing utilization of Jenkins build farm resources.

The default behavior for SonarQube quality gate, as coded in the `waitForQualityGate` function,  is to break the build in case or warning or error. However, it is better to fail the build only when the quality gate is in error status. To code that behavior in the pipeline, there is a custom `script` block coding that logic.

### 3.18) The pipeline code: Pushing the Docker image

At this point of the pipeline, if all quality gates have passed, the produced image can be considered as a stable, releasable version, and hence might be published to a shared Docker registry, like Docker Hub, as the final stage of the pipeline:

```groovy
    ...
    stages {
        ...
        stage('Push Docker image') {
            steps {
                echo "-=- push Docker image -=-"
                sh "./mvnw docker:push"
            }
        }
    }
    ...
```

For this command to work, the right credentials might be set. The Spotify Docker plugin that is being used, configures registry credentials in various ways but particularily one is perfect for pipeline usage, as it does not require any pre-configuration in the builder container: credential injection through Maven settings.

So far, the following configuration pieces are set: Spotify plugin sets the server id with the following configuration setting `<serverId>docker-hub</serverId>`, and the pipeline gets the credentials injected as the `DOCKER_HUB` property.

Actually, unseen by the eye, other two properties are created by Jenkins `credentials` function: `DOCKER_HUB_USR` and `DOCKER_HUB_PSW`.

To finally connect all the dots together, it is needed that Maven processes, specifically this one, has special settings to inject the credential so it's available to Spotify plugin.

The easiest way to do that, is to create a `.mvn/maven.config` file with the reference to the settings file to be used, and the Maven wrapper, the `mvnw` command that is being used along the pipeline, will pick those settings automatically.

These are the contents for the `.mvn/maven.config` file:

    -s .mvn/settings.xml

And these are the contents for the `.mvn/settings.xml` file which is being referenced:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">

    <interactiveMode>true</interactiveMode>
    <offline>false</offline>
    <pluginGroups/>
    <proxies/>
    <servers>
        <server>
            <id>docker-hub</id>
            <username>${env.DOCKER_HUB_USR}</username>
            <password>${env.DOCKER_HUB_PSW}</password>
        </server>
    </servers>
    <mirrors/>
    <profiles/>

</settings>
```

### 3.19) The pipeline code: Cleaning up resources

The final piece to set is the `post` block to clean up any resources. In this case, the test container should be removed:

```groovy
#!groovy

pipeline {
    ...
    post {
        always {
            echo "-=- remove deployment -=-"
            sh "docker stop ${TEST_CONTAINER_NAME}"
        }
    }
}
```

### 3.20) Running the pipeline

Once all the pieces are together, pipelines configured for every service in our system, it's the time to add the job to Jenkins and execute it.

Green balls!

## Appendixes

### Clean up the swarm

To remove running services:

    docker service rm configservice eurekaservice hystrixservice bookrecservice bookrecedgeservice

To verify they are all removed:

    docker service ls

To remove all stored images (this must be done in every machine if more than one was used), when in Windows, run these commands:

    for /F %f in ('docker ps -a -q') do (docker rm %f)
    for /F %f in ('docker images -q') do (docker rmi --force %f)

When in Linux, use these commands:

    docker rm $(docker ps -a -q)
    docker rmi --force $(docker images -q)

To remove the nework inside swarm:

    docker network rm microdemonet

Finally, stop the machines:

    docker-machine stop docker-swarm-manager-1 docker-swarm-manager-2 docker-swarm-manager-3 docker-swarm-worker-1 docker-swarm-worker-2 docker-swarm-worker-3

If desired, the swarm can be disposed, too, by removing all the machines included in it:

    docker-machine rm docker-swarm-manager-1 docker-swarm-manager-2 docker-swarm-manager-3 docker-swarm-worker-1 docker-swarm-worker-2 docker-swarm-worker-3

### Launching Jenkins and SonarQube

Both Jenkins and SonarQube servers are required for running the pipelines and code inspection. Although there are many ways to have Jenkins and SonarQube up and running, this is probably the easiest, fastest one -- running them as Docker containers:

    docker run --name ci-jenkins \
        --user root \
        --detach \
        --network ci \
        --publish 9080:8080 --publish 50000:50000 \
        --mount type=volume,source=ci-jenkins-home,target=/var/jenkins_home \
        --mount type=bind,source=/var/run/docker.sock,target=/var/run/docker.sock \
        --mount type=bind,source=/usr/local/bin/docker,target=/usr/local/bin/docker \
        --env JAVA_OPTS="-Xmx2048M" \
        --env JENKINS_OPTS="--prefix=/jenkins" \
        jenkins/jenkins:2.150.3

    docker run --name ci-sonarqube-data \
        --detach \
        --network ci \
        --mount type=volume,source=ci-sonarqube-data,target=/var/lib/mysql \
        --env MYSQL_DATABASE="sonar" \
        --env MYSQL_USER="sonar" \
        --env MYSQL_PASSWORD="sonarsonar" \
        --env MYSQL_ROOT_PASSWORD="adminadmin" \
        mysql:5.6.41

    sleep 10

    docker run --name ci-sonarqube \
        --detach \
        --network ci \
        --publish 9000:9000 \
        --mount type=volume,source=ci-sonarqube-extensions,target=/opt/sonarqube/extensions \
        --mount type=volume,source=ci-sonarqube-esdata,target=/opt/sonarqube/data \
        --env SONARQUBE_JDBC_URL="jdbc:mysql://ci-sonarqube-data:3306/sonar?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true" \
        --env SONARQUBE_JDBC_USERNAME="sonar" \
        --env SONARQUBE_JDBC_PASSWORD="sonarsonar" \
        sonarqube:6.7.6-community -Dsonar.web.context=/sonarqube

Note that the preceding commands will set up persistent volumes so all configuration, plugins and data persists across server restarts.

### Troubleshooting

If using more than one machine in the swarm, images must be published to Docker Hub or another registry (for example a local registry) so they are accessible to all hosts in the swarm.

To troubleshoot connectivity with curl in alpine-based images, install and use it in this way:

    docker exec <container-id> apk add --update curl && curl <url>

To check wich IP addresses are active in a container:

    docker exec <container-id> ifconfig -a

To check the environment variables in a container:

    docker exec <container-id> env
