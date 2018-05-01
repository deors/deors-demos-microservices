# deors-demos-microservices

## Microservices with Spring Boot, Spring Cloud and Netflix OSS

Demonstration of an exemplar microservices stack based in Spring Boot, Spring Cloud and Netflix OSS, including step-by-step instructions to recreate the demo stack and run it in different environments.

This demo is organised in iterations, starting from the basics and building up in complexity and features along the way.

NOTE: The following instructions are created on a Windows machine, hence some commands may need slight adjustments when working on Linux/OSX, e.g. replace `%ENV_VAR%` by `${ENV_VAR}`.

## Iteration 1) The basics

### 1.1) Set up the configuration store

The configuration store is a repository where microservice settings are stored, and accessible for microservice initialisation at boot time.

Create and change to a directory for the project:

    mkdir %HOME%\microservices\deors-demos-microservices-configstore
    cd %HOME%\microservices\deors-demos-microservices-configstore

Create the file `application.properties`. This file will contain settings which are common to all microservices:

    debug = true
    spring.jpa.generate-ddl = true
    management.security.enabled = false

The third configuration setting will disable security for actuator endpoints, which allows for remote operations on running applications. Disabling security in this manner should be done only when public access to those endpoints is restricted externally (for example through the web server or reverse proxy). Never expose actuator endpoints publicly and insecurely!

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
    defaultBook = this is the default recommendation: Book {id=-1, title='robots of dawn', author='isaac asimov'}

Initialise the Git repository:

    git init
    git add .
    git commit -m "initial configuration"

Publish it online (replace the actual URL with your own repository):

    git remote add origin https://github.com/deors/deors-demos-microservices-configstore.git
    git push origin master

### 1.2) Set up the configuration service

The configuration service, powered by Spring Cloud Config Server, is the microservice that will provide every other microservice in the system with the configuration settings they need at boot time.

Go to `https://start.spring.io/` and create the project with the following settings:

    group: deors.demos.microservices
    artifact: deors-demos-microservices-configservice
    depedencies:
        actuator
        config server

The actuator depedency, when added, enables useful endpoints to facilitate application operations.

Extract the generated zip to:

    %HOME%\microservices

Change into extracted directory:

    cd %HOME%\microservices\deors-demos-microservices-configservice

Next, let's add the application name and set the configuration store location. Edit `src\main\resources\application.properties`:

    server.port = ${PORT:8888}
    spring.application.name = configservice
    spring.cloud.config.server.git.uri = ${CONFIG_REPO_URL:https://github.com/deors/deors-demos-microservices-configstore.git}

To configure the configuration server to start automatically, edit `src\main\java\deors\demos\microservices\configservice\ConfigserviceApplication.java` and add the following class annotation:

```java
    @org.springframework.cloud.config.server.EnableConfigServer
```

### 1.3) Set up the service registry and discovery service (Eureka)

The service registry and discovery service, powered by Spring Cloud and Netflix Eureka, is the microservice that will enable every other microservice in the system to register 'where' they are physically located, so others can discover them and interact with them.

Go to `https://start.spring.io/` and create the project with the following settings:

    group: deors.demos.microservices
    artifact: deors-demos-microservices-eurekaservice
    depedencies:
        actuator
        config client
        eureka server

Extract the generated zip to:

    %HOME%\microservices

Change into extracted directory:

    cd %HOME%\microservices\deors-demos-microservices-eurekaservice

To ensure that the configuration service is used, properties should be moved to bootstrap phase:

    ren src\main\resources\application.properties bootstrap.properties

Edit `src\main\resources\bootstrap.properties`:

    spring.application.name = eurekaservice
    spring.cloud.config.uri = http://${CONFIG_HOST:localhost}:${CONFIG_PORT:8888}

To configure the Eureka server to start automatically, edit `src\main\java\deors\demos\microservices\eurekaservice\Application.java` and add the following class annotation:

```java
    @org.springframework.cloud.netflix.eureka.server.EnableEurekaServer
```

### 1.4) Set up the circuit breaker dashboard service (Hystrix)

The circuit breaker dashboard, powered by Spring Cloud and Netflix Hystrix, will provide devs and ops teams with real-time views about service calls performance and failures including for example which of them are experimenting repeated failures. When that happens, Hystrix 'opens' the circuit and calls to a default safe method to keep the flow of information up and running smoothly, not causing cascading failures across services which may lead to major downtimes.

Go to `https://start.spring.io/` and create the project with the following settings:

    group: deors.demos.microservices
    artifact: deors-demos-microservices-hystrixservice
    depedencies:
        actuator
        config client
        eureka discovery
        hystrix dashboard

Extract the generated zip to:

    %HOME%\microservices

Change into extracted directory:

    cd %HOME%\microservices\deors-demos-microservices-hystrixservice

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

This is the first microservice with actual functionality on our problem domain. bookrec is the service which provides methods to query, create, update and remove Book entities from the data store.

Go to `https://start.spring.io/` and create the project with the following settings:

    group: deors.demos.microservices
    artifact: deors-demos-microservices-bookrecservice
    depedencies:
        actuator
        config client
        eureka discovery
        web
        rest repositories
        rest repositories hal browser
        hateoas
        jpa
        h2

Extract the generated zip to:

    %HOME%\microservices

Change into extracted directory:

    cd %HOME%\microservices\deors-demos-microservices-bookrecservice

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
    @Entity
    public class Book {
        @Id
        @GeneratedValue
        private Long id;
        private String title;
        private String author;
    }
```

Add bean constructors, getters, setters and toString method, or generate them with your IDE!

Create the BookRepository data access interface:

```java
    @RepositoryRestResource
    public interface BookRepository extends CrudRepository<Book, Long> {
        @Query("select b from Book b order by RAND()")
        List<Book> getBooksRandomOrder();
    }
```

Create the BookController controller class:

```java
    @RestController
    public class BookController {
        @Autowired
        private BookRepository bookRepository;

        @RequestMapping("/bookrec")
        public String getBookRecommendation() throws UnknownHostException {
            return "the host in IP: "
                    + InetAddress.getLocalHost().getHostAddress()
                    + " recommends this book: "
                    + bookRepository.getBooksRandomOrder().get(0);
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

The bookrec edge service is used by clients to interact with the bookrec service. As a general rule, services with business logic and access to data stores, should not be exposed directly to clients. Therefore, they are called inner services. Edge services will wrap access to inner services, as well as adding other non-functional capabilities, like routing, throttling, caching or fault tolerance.

Go to `https://start.spring.io/` and create the project with the following settings:

    group: deors.demos.microservices
    artifact: deors-demos-microservices-bookrecedgeservice
    depedencies:
        actuator
        config client
        eureka discovery
        hystrix
        ribbon
        web
        rest repositories
        rest repositories hal browser
        hateoas

Extract the generated zip to:

    %HOME%\microservices

Change into extracted directory:

    cd %HOME%\microservices\deors-demos-microservices-bookrecedgeservice

To ensure that the configuration service is used, properties should be moved to bootstrap phase:

    ren src\main\resources\application.properties bootstrap.properties

Edit `src\main\resources\bootstrap.properties`:

    spring.application.name = bookrecedgeservice
    spring.cloud.config.uri = http://${CONFIG_HOST:localhost}:${CONFIG_PORT:8888}

There is one dependency that is currently not being added by the Spring Boot starter BOM's and must be added manually. Edit `pom.xml` and add the following dependency:

```xml
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-netflix-hystrix-stream</artifactId>
    </dependency>
```

To configure the service to be discoverable (Eureka) and to use the circuit breaker (Hystrix), edit `src\main\java\deors\demos\microservices\bookrecedgeservice\BookrecedgeserviceApplication.java` and add the following class annotations:

```java
    @org.springframework.cloud.client.discovery.EnableDiscoveryClient
    @org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker
```

Next, add the `restTemplate()` method which will initalize the RestTemplate object which will be used to invoke bookrecservice. Client-side load balancing (Ribbon) is enabled just by adding the corresponding annotation:

```java
    @Bean
    @org.springframework.cloud.client.loadbalancer.LoadBalanced
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
```

Create the Book bean for the edge service:

    public class Book {
        private Long id;
        private String title;
        private String author;
    }

Add bean constructors (including one with the three properties), getters, setters and toString method, or generate them with your IDE!

Create the BookController controller for the edge service, including the call to bookrec through Hystrix and providing the default fallback method in case of problems with calls to bookrec:

```java
    @RestController
    class BookController {
        @Autowired
        RestTemplate restTemplate;

        @Value("${defaultBook}")
        private String defaultBook;

        @RequestMapping("/bookrecedge")
        @com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand(fallbackMethod = "getDefaultBook")
        public String getBookRecommendation() {
            return restTemplate.getForObject("http://bookrecservice/bookrec", String.class);
        }

        public String getDefaultBook() {
            return defaultBook;
        }
    }
```

### 1.7) Run the services locally

Services are now ready to be executed locally, using the sensible default configuration settings and the embedded runtimes provided by Spring Boot.

Each service will be run by executing this command in each folder:

    mvn spring-boot:run

### 1.8) Test services locally

Once all the services are started, they will be available at the defined ports in the local host.

Access the configuration service through one the actuator endpoints (remember it is currently unsecured):

    http://localhost:8888/env

Check that the configuration service is capable of returning the configuration for some of the services:

    http://localhost:8888/bookrecservice/default
    http://localhost:8888/eurekaservice/default

Check that Eureka service is up and the book recommendation service and edge service are registered:

    http://localhost:7878/

Check that Hystrix service is up and running:

    http://localhost:7979/hystrix

Access the HAL browser on the book recommendation service:

    http://localhost:8080/

Access the book recommendation service iself:

    http://localhost:8080/bookrec

Access the HAL browser on the book recommendation edge service:

    http://localhost:8181/

Access the book recommendation edge service itself:

    http://localhost:8181/bookrecedge

To verify that Hystrix fault tolerance mechanism is working as expected, stop the book recommendation service, and access the book recommendation edge service again. The default recommended book should be returned instead and the application keeps working.

Go back to Hystrix dashboard and start monitoring the book recommendation edge service by registering the bookrec Hystrix stream in the dashboard (and optionally configuring the delay and page title):

     http://localhost:8181/hystrix.stream

Once the Hystric stream is registered, try again to access the edge service, with and without the inner service up and running, and experiment how thresholds (number of errors in a short period of time) impact the opening and closing of the circuit between the inner and the edge service.

## Iteration 2) Preparing for Docker and Swarm

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

    cd %HOME%\microservices\deors-demos-microservices-bookrecservice

Edit `pom.xml` and add inside `<properties>` the following property:

```xml
    <docker.image.prefix>deors</docker.image.prefix>
```

Add inside `<build><plugins>` Spotify's Docker Maven plug-in configuration:

```xml
    <plugin>
        <groupId>com.spotify</groupId>
        <artifactId>docker-maven-plugin</artifactId>
        <version>1.0.0</version>
        <configuration>
            <imageName>${docker.image.prefix}/${project.artifactId}</imageName>
            <dockerDirectory>src/main/docker</dockerDirectory>
            <imageTags>
                <imageTag>${project.version}</imageTag>
                <imageTag>latest</imageTag>
            </imageTags>
            <serverId>docker-hub</serverId>
            <resources>
                <resource>
                    <targetPath>/</targetPath>
                    <directory>${project.build.directory}</directory>
                    <include>${project.build.finalName}.jar</include>
                </resource>
            </resources>
        </configuration>
    </plugin>
```

Create a new directory for the Dockerfile:

    mkdir src\main\docker

Create the file `src\main\docker\Dockerfile` and add the following content:

    FROM frolvlad/alpine-oraclejdk8:slim
    VOLUME /tmp
    ADD deors-demos-microservices-bookrecservice-0.0.1-SNAPSHOT.jar app.jar
    ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]

Repeat for the other microservices (don't forget to update Jar file name in ADD command).

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

Access the configuration service through one the actuator endpoints (remember it is currently unsecured):

    http://192.168.66.100:8888/env

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

Access the HAL browser on the book recommendation edge service:

    http://192.168.66.100:8181/

Access the book recommendation edge service itself:

    http://192.168.66.100:8181/bookrecedge

To verify that Hystrix fault tolerance mechanism is working as expected, stop the book recommendation service, and access the book recommendation edge service again. The default recommended book should be returned instead and the application keeps working.

Go back to Hystrix dashboard and start monitoring the book recommendation edge service by registering the bookrec Hystrix stream in the dashboard (and optionally configuring the delay and page title):

     http://192.168.66.100:8181/hystrix.stream

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
