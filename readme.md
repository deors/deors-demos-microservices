# deors-demos-microservices

## Microservices with Spring Boot, Spring Cloud and Netflix OSS

Demonstration of an exemplar microservices stack based in Spring Boot, Spring Cloud and Netflix OSS. The exemplar configuration also includes containerization with Docker and CI/CD pipelines with Jenkins and Apache Maven lifecycle.

HISTORY NOTE: This demo project was first featured as a live coding session in OpenSlava 2016 conference, in the beautiful city of Bratislava, and the Jenkins pipelines are based on the `workshop-pipelines` workshop, first featured as a talk in Oracle Code One San Francisco 2018 conference.

## Microservice list

This project has five microservices working together to deliver the business value:

- **`configservice`**: This service is responsible to provide configuration settings to the other microservices in the stack.
- **`eurekasercice`**: This service acts as the service registry and service discovery component of the stack.
- **`hystrixservice`**: This service provides visibility on the other services health and circuit breaker metrics.
- **`bookrecsercice`**: This service provides with book recommendations from a book inventory stored in an in-memory database.
- **`bookrecedgeservice`**: This service exposes the business logic to clients, being the 'edge' service the one directly exposed externally.

## Building up

### Configuration

Microservices use Spring Config Server to get configuration at run time. For that to work, configuration is stored in an accessible Git repository in [this project](https://github.com/deors/deors-demos-microservices-configstore).

### Run the services locally

Out of the box, services are ready to be executed locallly using the sensible default configuration settings and the embedded runtimes provided by Spring Boot.

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

### Test the services locally

Once all the services are started, they will be available at localhost at different ports:

    - configservice listens to port 6868
    - eurekaservice listens to port 7878
    - hystrixservice port is random
    - bookrecservice port is random
    - bookrecedgeservice port is random

Access the configuration service through the actuator health endpoint (remember it is currently unsecured), to verify it is up & running and responding to requests normally:

    http://localhost:6868/actuator/health

Check that the configuration service is capable of returning the configuration for some of the services:

    http://localhost:6868/eurekaservice/default
    http://localhost:6868/bookrecservice/default

Check that Eureka service is up and the other services are registered (including the ports assigned during startup time):

    http://localhost:7878/

Check that Hystrix service is up and running:

    http://localhost:<hystrix-port>/hystrix

Access the HAL browser on the book recommendation service:

    http://localhost:<bookrec-port>/

Access the book recommendation service itself:

    http://localhost:<bookrec-port>/bookrec

Access the book recommendation edge service itself:

    http://localhost:<bookrecedge-port>/bookrecedge

To verify that Hystrix fault tolerance mechanism is working as expected, stop the book recommendation service, and access the book recommendation edge service again. The default recommended book should be returned instead and the application keeps working.

Go back to Hystrix dashboard and start monitoring the book recommendation edge service by registering the bookrec Hystrix stream in the dashboard (and optionally configuring the delay and page title):

     http://localhost:<bookrecedge-port>/actuator/hystrix.stream

Once the Hystric stream is registered, try again to access the edge service, with and without the book recommendation service up and running, and experiment how thresholds (number of errors in a short period of time) impact the opening and closing of the circuit between the inner and the edge service.

## Running services in Kubernetes with Rancher Desktop, K3s and nerdctl

### Building the images

Building the images is a process that varies depending on the OS, containerization platform and tooling used to manage the image and container life cycle.

The following instructions are based on Rancher Desktop, K3s and nerdctl tooling. Rancher Desktop is an easy way to run Kubernetes on a workstation thanks to the use of K3s lightweight distribution, and nerdctl is very convenient as it leverages many of the well-known docker cli commands but for containerd engine.

To ensure that the container images are available to the local Kubernetes cluster, the namespace parameter must be supplied with ```k8s.io``` value. The following commands must be run in the corresponding folder for each microservice:

    nerdctl -n k8s.io build -t deors-demos-microservices-configservice:1.0-SNAPSHOT .
    nerdctl -n k8s.io build -t deors-demos-microservices-eurekaservice:1.0-SNAPSHOT .
    nerdctl -n k8s.io build -t deors-demos-microservices-hystrixservice:1.0-SNAPSHOT .
    nerdctl -n k8s.io build -t deors-demos-microservices-bookrecservice:1.0-SNAPSHOT .
    nerdctl -n k8s.io build -t deors-demos-microservices-bookrecedgeservice:1.0-SNAPSHOT .

To confirm that images are available, the following command will do the query:

    nerdctl -n k8s.io images | grep deors-demos-microservices

### Running the pods and wiring them all together

Once the images are ready, they can be scheduled in the local cluster. First, proceed with the configuration service:

    kubectl run configservice --image deors-demos-microservices-configservice:1.0-SNAPSHOT

To test the service, use a port forward command:

    kubectl port-forward pods/configservice 6868:6868

And once ready, open the familiar URLs:

    http://localhost:6868/actuator/health
    http://localhost:6868/eurekaservice/default
    http://localhost:6868/bookrecservice/default

Once confirmed that the service is working, the port must be exposed to the other microservices can connect with it (the port forward is just for convenience to our own tests):

    kubectl expose pod configservice --port 6868

Next, run Eureka taking into consideration that it needs to connect with the configuration service, expose the port and verify that it is running as expected:

    kubectl run eurekaservice --image deors-demos-microservices-eurekaservice:1.0-SNAPSHOT --env "CONFIG_HOST=configservice"
    kubectl expose pod eurekaservice --port 7878
    kubectl port-forward pods/eurekaservice 7878:7878
    http://localhost:7878/actuator/health
    http://localhost:7878

Next proceed the same with Hystrix taking into consideration that this service also needs to connect with Eureka:

    kubectl run hystrixservice --image deors-demos-microservices-hystrixservice:1.0-SNAPSHOT --env "CONFIG_HOST=configservice" --env "EUREKA_HOST=eurekaservice" --env "EUREKA_PORT=7878"
    kubectl expose pod hystrixservice --port <hystrix-port>
    kubectl port-forward pods/hystrixservice 7979:<hystrix-port>
    http://localhost:7979/actuator/health
    http://localhost:7979/hystrix

The Hystrix service is not configured to run with a fixed port, which means that it gets a random port assigned at boot time unless passed as a parameter. To discover that port, it is possible to look into the pod logs but as Eureka is configured and Hystrix service is registering there, the easiest way to look for the port is through Eureka web dashboard. However, for convenience during local tests the port forward can be set to a fixed known port (7979 in the example above).

Finally, it is the moment to run the business services, and verify whether they are working fine:

    kubectl run bookrecservice --image deors-demos-microservices-bookrecservice:1.0-SNAPSHOT --env "CONFIG_HOST=configservice" --env "EUREKA_HOST=eurekaservice" --env "EUREKA_PORT=7878"
    kubectl expose pod bookrecservice --port <bookrec-port>
    kubectl port-forward pods/bookrecservice 8080:<bookrec-port>
    http://localhost:8080/actuator/health
    http://localhost:8080/bookrec

    kubectl run bookrecedgeservice --image deors-demos-microservices-bookrecedgeservice:1.0-SNAPSHOT --env "CONFIG_HOST=configservice" --env "EUREKA_HOST=eurekaservice" --env "EUREKA_PORT=7878"
    kubectl expose pod bookrecedgeservice --port <bookrecedge-port>
    kubectl port-forward pods/bookrecedgeservice 8181:<bookrecedge-port>
    http://localhost:8181/actuator/health
    http://localhost:8181/bookrecedge

If everything run as expected and all services are wired through Eureka and Kubernetes services, the book recommendation edge service will be returning valid book recommendations from the defined list. If not, the default book recommendation will be returned.

### Verifying graceful degradation of the book recommendation service

An easy way to check the circuit breaker is, once everything is up and running, to remove the book recommendation Kubernetes service. That way, although the pod is running it will not be accessible to the other pods in our system (but it will be to us through the port forward!), and the edge service will return the default book recommendation:

    kubectl delete service bookrecservice
    http://localhost:8080/bookrec
    http://localhost:8181/bookrecedge

By simply recreating the Kubernetes service the circuit will be closed again and the edge service will return to normal behavior:

    kubectl expose pod bookrecservice --port <bookrec-port>
    http://localhost:8080/bookrec
    http://localhost:8181/bookrecedge

This is an example of graceful degradation with the help or circuit breakers, reducing the impact of internal failure to customer-facing APIs and user interfaces.

### Cleaning up resources

To clean up resources all pods and services need to be terminated. If any port forward is still open they should be stopped (Ctrl-C), too:

    kubectl delete pod bookrecedgeservice
    kubectl delete service bookrecedgeservice

    kubectl delete pod bookrecservice
    kubectl delete service bookrecservice

    kubectl delete pod hystrixservice
    kubectl delete service hystrixservice

    kubectl delete pod eurekaservice
    kubectl delete service eurekaservice

    kubectl delete pod configservice
    kubectl delete service configservice

## Running services in Docker Swarm

NOTE: Section about Docker support is deprecated - kept for historical reasons

### Set up Swarm

The following instructions will show how to create a simple swarm in VirtualBox, in the case that a swarm is not already available. In the case that a Docker Swarm is already available, skip to section 2.2.

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

### Create the Docker images

NOTE: Images are already created and pushed to Docker Hub, so this step can be skipped if no changes have been made to the microservices.

To create the images, a Docker host is needed. If using Docker for Windows, Linux or Mac, or boot2docker (VirtualBox), that Docker host is sufficient. It is also possible to use the swarm created before (one machine is enough). Let's start the swarm:

    docker-machine start docker-swarm-manager-1 docker-swarm-manager-2 docker-swarm-manager-3 docker-swarm-worker-1 docker-swarm-worker-2 docker-swarm-worker-3

Configure Docker client to work with one of the machines. Wwhen in Windows, run this command:

    @FOR /f "tokens=*" %i IN ('docker-machine env docker-swarm-manager-1') DO @%i

When in Linux, run this command:

    eval $(docker-machine env docker-swarm-manager-1)

Build and push images by running this command for each microservice:

    mvn package docker:build -DpushImage

Images must be pushed so they are available to all machines in the swarm, and not only in the machine used to build the image.

### Run the images as services in Swarm

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

### Test services in Swarm

Once all the services are started, they will be available at the defined ports in the local host.

Access the configuration service through the actuator health endpoint (remember it is currently unsecured), to verify it is up & running and responding to requests normally:

    http://192.168.66.100:8888/actuator/health

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

NOTE: When Docker Swarm is configured on a single-node setup with Docker for Windows or Docker for Mac, the Swarm manager address is not accessible from the local computer (Docker runs in an Hyper-V or HyperKit VM, transparently to the user) and `localhost` should be used instead.

### Scale out the book recommendation service

Ask Docker to scale out the book recommendation service

    docker service scale bookrecservice=3

### Make and update and roll out the changes without service downtime

Make some change and deploy a rolling update. For example change the text string returned by BookController class in file `src/main/java/deors/demos/microservices/BookController.java`.

Rebuild and push the new image to the registry:

    mvn package docker:build -DpushImage

Next, the change is deployed. A label is needed to ensure the new version of 'latest' image is downloaded from registry:

    docker service update --container-label-add update_cause="change" --update-delay 30s --image deors/deors-demos-microservices-bookrecservice:latest bookrecservice

To check how the change is being deployed, issue this command repeatedly:

    docker service ps bookrecservice

### Cleanning up resources -- Stopping services and deleting the swarm

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
