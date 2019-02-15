# deors-demos-microservices

## Microservices with Spring Boot, Spring Cloud and Netflix OSS

Demonstration of an exemplar microservices stack based in Spring Boot, Spring Cloud and Netflix OSS, including step-by-step instructions to recreate the demo stack and run it in different environments.

This demo is organised in iterations, starting from the basics and building up in complexity and features along the way.

NOTE: The following labs are created on a Windows machine, hence some commands may need slight adjustments when working on Linux/OSX, e.g. replace `%ENV_VAR%` by `${ENV_VAR}`, and replace back-slashes by forward-slashes.

NOTE: The following labs have been tested with Spring Boot 2.1.2, Apache Maven 3.6.0 and Java 11.0.2, the latest versions of them available at the time of publishing this.

## Iteration 1) The basics

### 1.1) Set up the configuration store

The configuration store is a repository where microservice settings are stored, and accessible for microservice initialisation at boot time.

Create and change to a directory for the project:

    mkdir %HOME%\microservices\configstore
    cd %HOME%\microservices\configstore

Create the file `application.properties`. This file will contain settings which are common to all microservices:

    debug = true
    spring.jpa.generate-ddl = true
    management.security.enabled = false
    management.endpoints.web.exposure.include = hystrix.stream

The third configuration setting will disable security for actuator endpoints, which allows for remote operations on running applications. Disabling security in this manner should be done only when public access to those endpoints is restricted externally (for example through the web server or reverse proxy). Never expose actuator endpoints publicly and insecurely!

The fourth configuration setting will expose the hystrix.stream actuator endpoint via http, which will allow Hystrix (see later) to monitor performance of individual services.

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

    %HOME%\microservices

Change into extracted directory:

    cd %HOME%\microservices\configservice

First, let's modify `pom.xml` file to upgrade the Java version to 11:

```xml
    <properties>
    ...
        <java.version>11</java.version>
    ...
    </properties>
```

Next, let's add the application name and set the configuration store location. Edit `src\main\resources\application.properties`:

    server.port = ${PORT:8888}
    spring.application.name = configservice
    spring.cloud.config.server.git.uri = ${CONFIG_REPO_URL:https://github.com/deors/deors-demos-microservices-configstore.git}
    management.security.enabled = false

If the configuration store is local, leverage the File protocol in Git:

    spring.cloud.config.server.git.uri = ${CONFIG_REPO_URL:file:///%HOME%/microservices/configstore}

To configure the configuration server to start automatically, edit `src\main\java\deors\demos\microservices\configservice\ConfigserviceApplication.java` and add the following class annotation:

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

    %HOME%\microservices

Change into extracted directory:

    cd %HOME%\microservices\eurekaservice

First, let's modify `pom.xml` file to upgrade the Java version to 11:

```xml
    <properties>
    ...
        <java.version>11</java.version>
    ...
    </properties>
```

When using Java 9+, Spring Boot does not add JAX-B module (java.xml.bind) to Tomcat module path automatically, causing service registry service to fail on startup. In that case, the missing module should be added to Tomcat module path. When using `spring-boot:run` Maven goal to run the application, use the `JDK_JAVA_OPTIONS` environment variable:

    set JDK_JAVA_OPTIONS=--add-modules java.xml.bind
    mvnw spring-boot:run

Or, when executing the fat Jar directly:

    java -jar target/<name-of-the-fat.jar> --add-modules java.xml.bind

But the preferred way to fix this is directly in `pom.xml` file:

```xml
    <dependencies>
    ...
        <dependency>
            <groupId>javax.xml.bind</groupId>
            <artifactId>jaxb-api</artifactId>
            <version>2.3.1</version>
        </dependency>
    ...
    </dependencies>
```

To ensure that the configuration service is used, properties should be moved to bootstrap phase:

    ren src\main\resources\application.properties bootstrap.properties

Edit `src\main\resources\bootstrap.properties`:

    spring.application.name = eurekaservice
    spring.cloud.config.uri = http://${CONFIG_HOST:localhost}:${CONFIG_PORT:8888}

To configure the Eureka server to start automatically, edit `src\main\java\deors\demos\microservices\eurekaservice\EurekaserviceApplication.java` and add the following class annotation:

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

    %HOME%\microservices

Change into extracted directory:

    cd %HOME%\microservices\hystrixservice

First, let's modify `pom.xml` file to upgrade the Java version to 11:

```xml
    <properties>
    ...
        <java.version>11</java.version>
    ...
    </properties>
```

To ensure that the configuration service is used, properties should be moved to bootstrap phase:

    ren src\main\resources\application.properties bootstrap.properties

Edit `src\main\resources\bootstrap.properties`:

    spring.application.name = hystrixservice
    spring.cloud.config.uri = http://${CONFIG_HOST:localhost}:${CONFIG_PORT:8888}

To configure the Hystrix dashboard to start automatically, edit `src\main\java\deors\demos\microservices\hystrixservice\HystrixserviceApplication.java` and add the following class annotations:

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

    %HOME%\microservices

Change into extracted directory:

    cd %HOME%\microservices\bookrecservice

First, let's modify `pom.xml` file to upgrade the Java version to 11:

```xml
    <properties>
    ...
        <java.version>11</java.version>
    ...
    </properties>
```

To ensure that the configuration service is used, properties should be moved to bootstrap phase:

    ren src\main\resources\application.properties bootstrap.properties

Edit `src\main\resources\bootstrap.properties`:

    spring.application.name = bookrecservice
    spring.cloud.config.uri = http://${CONFIG_HOST:localhost}:${CONFIG_PORT:8888}

To configure the service to be discoverable, edit `src\main\java\deors\demos\microservices\bookrecservice\BookrecserviceApplication.java` and add the class annotation:

```java
@org.springframework.cloud.client.discovery.EnableDiscoveryClient
```

Create the Book entity class:

```java
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class Book {

    @Id @GeneratedValue private Long id;
    private String title;
    private String author;
}
```

Add bean constructors (including the default constructor and one that initalizes the three properties), getters, setters and toString method, or generate them with your IDE!

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

    %HOME%\microservices

Change into extracted directory:

    cd %HOME%\microservices\bookrecedgeservice

First, let's modify `pom.xml` file to upgrade the Java version to 11:

```xml
    <properties>
    ...
        <java.version>11</java.version>
    ...
    </properties>
```

To ensure that the configuration service is used, properties should be moved to bootstrap phase:

    ren src\main\resources\application.properties bootstrap.properties

Edit `src\main\resources\bootstrap.properties`:

    spring.application.name = bookrecedgeservice
    spring.cloud.config.uri = http://${CONFIG_HOST:localhost}:${CONFIG_PORT:8888}

To configure the service to be discoverable (Eureka) and to use the circuit breaker (Hystrix), edit `src\main\java\deors\demos\microservices\bookrecedgeservice\BookrecedgeserviceApplication.java` and add the following class annotations:

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

Add bean constructors (including the default constructor and one that initalizes the three properties), getters, setters and toString method, or generate them with your IDE!

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
    RestTemplate restTemplate;

    @Value("${defaultBookId}") private long defaultBookId;
    @Value("${defaultBookTitle}") private String defaultBookTitle;
    @Value("${defaultBookAuthor}") private String defaultBookAuthor;

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

In this section, pom files will be configured with Spotify's Docker Maven plug-in, and Dockerfile files will be created, to allow each service to run as a Docker image. This process must be done for each of the microservices in the stack.

Let's proceed with bookrec service as an example. Change to its directory:

    cd %HOME%\microservices\bookrecservice

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

Add inside `<build><plugins>` Spotify's Docker Maven plug-in configuration:

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

The server id in Spotify's plug-in configuration must match an existing credential in Maven's settings. This is the credential that will be used to publish new and updated images.

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

Make some change and deploy a rolling update. For example change the text string returned by BookController class in file `src\main\java\deors\demos\microservices\BookController.java`:

Rebuild and push the new image to the registry:

    mvn package docker:build -DpushImage

Next, the change is deployed. A label is needed to ensure the new version of 'latest' image is downloaded from registry:

    docker service update --container-label-add update_cause="change" --update-delay 30s --image deors/deors-demos-microservices-bookrecservice:latest bookrecservice

To check how the change is being deployed, issue this command repeatedly:

    docker service ps bookrecservice

## Iteration 3) Configuring continuous integration pipelines with Jenkins

### 3.1) The anatomy of a Jenkins pipeline

TBD

### 3.2) Configuring quality tools along the Maven lifecycle

An effective continuous integration pipeline must have sufficient verification steps as to give confidence in the process. Verification steps will include code inspection, and testing:

Code inspection activities are basically three:

- Gathering of metrics, like: size, complexity, code duplications, and others related with architecture and design implementation.
- Static code profiling: analysis of sources looking for known patterns that may result in vulnerabilities, reliability issues, performance issues, or affect maintainability.
- Dependency analysis: analysis of dependency manifests (e.g. those included in `pom.xml` or `require.js` files), looking for known vulnerabilities in those dependencies, as published in well-known databases like CVE.

Testing activities will include the following:

- Unit tests.
- Unit-integration tests: Those that, although not requiring the application to be deployed, are testing multiple components together. For example, in-container tests.
- Integration tests: Including in this group all those kinds of tests that require the application to be deployed. Typically external dependencies will be mocked up in this step. Integration tests will include API tests and UI tests.
- Performance tests: Tests verifying how the service or component behaves under load. Performance tests in this step are not meant to assess the overall system capacity (which can be virtually infinite with the appropriate scaling patterns), but to assess the capacity of one node, uncover concurrence issues due to the parallel execution of tasks, as well as to pinpoint possible bottlenecks or resource leaks when studying the trend. Very useful at this step to leverage APM tools to gather internal JVM metrics, e.g. to analyze gargabe collection.
- Security tests: Tests assessing possible vulnerabilities exposed by the application. In this step, the kind of security tests performed are typically DAST analysis.

In addition to the previous kinds of tests, there is one more which is meant to assess the quality of tests: Mutation tests. Mutation testing, usually executed only on unit tests for the sake of execution time, is a technique that identifies changes in source code, the so called mutations, applies them and re-execute the corresponding unit tests. If after a change in the source code, unit tests do not fail, that means that either the test code does not have assertions, or there are assertions but test coverage is unsufficient (typically test cases with certain conditions not tested).

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

### 3.4) Adding JaCoCo agent to gather code coverage metrics during tests

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

As the application is packaged and runs as a Docker image, the agent file must be present at the image build time. Later, during the execution of integration tests, the JaCoCo CLI tool will be needed to dump the coverage data from the test server. To do that, both dependencies will be copied into the expected folder with the help of the Maven Dependency plug-in:

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

### 3.5) Configuring Failsafe for integration test execution

Although Maven Surefire plug-in is enabled by default, Failsafe, the Surefire twin for integration tests, is disabled by default. To enable Failsafe, its targets must be called explicitely or alternatively may be binded to the corresponding lifecycle goals. For the microservices pipeline is preferred to have it disabled by default:

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

### Troubleshooting

If using more than one machine in the swarm, images must be published to Docker Hub or another registry (for example a local registry) so they are accessible to all hosts in the swarm.

To troubleshoot connectivity with curl in alpine-based images, install and use it in this way:

    docker exec <container-id> apk add --update curl && curl <url>

To check wich IP addresses are active in a container:

    docker exec <container-id> ifconfig -a

To check the environment variables in a container:

    docker exec <container-id> env
