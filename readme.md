# deors-demos-microservices

## Microservices with Spring Boot, Spring Cloud and Netflix OSS

Demonstration of an exemplar microservices stack based in Spring Boot, Spring Cloud and Netflix OSS. The exemplar configuration also includes containerization with Docker and CI/CD pipelines with Jenkins and Apache Maven lifecycle.

HISTORY NOTE: This demo project was first featured as a live coding session in OpenSlava 2016 conference, in the beautiful city of Bratislava, and the Jenkins pipelines are based on the `workshop-pipelines` workshop, first featured as a talk in Oracle Code One San Francisco 2018 conference.

SECOND HISTORY NOTE (Nov 2022): As time passes technologies come and go. In particular, Hystrix project is no longer under active maintenance and it was time to move away from it and replace the circuit breaker functionality with a valid alternative. Therefore, references to Hystrix in the current code version and this guide have been removed, and Resilience4J library is being leveraged through Spring Cloud. If interested in the previous version still using Hystrix checkout the tag `last-with-hystrix` (and do the same with `configstore`repository).

## Microservice list

This project has five microservices working together to deliver the business value:

- **`configservice`**: This service is responsible to provide configuration settings to the other microservices in the stack.
- **`eurekaservice`**: This service acts as the service registry and service discovery component of the stack.
- **`bookrecservice`**: This service provides with book recommendations from a book inventory stored in an in-memory database.
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
    - bookrecservice
    - bookrecedgeservice

### Test the services locally

Once all the services are started, they will be available at localhost at different ports:

    - configservice listens to port 6868
    - eurekaservice listens to port 7878
    - bookrecservice port is random
    - bookrecedgeservice port is random

Access the configuration service through the actuator health endpoint (remember it is currently unsecured), to verify it is up & running and responding to requests normally:

    http://localhost:6868/actuator/health

Check that the configuration service is capable of returning the configuration for some of the services:

    http://localhost:6868/eurekaservice/default
    http://localhost:6868/bookrecservice/default

Check that Eureka service is up and the other services are registered (including the ports assigned during startup time):

    http://localhost:7878/

Access the HAL browser on the book recommendation service:

    http://localhost:<bookrec-port>/

Access the book recommendation service itself:

    http://localhost:<bookrec-port>/bookrec

Access the book recommendation edge service itself:

    http://localhost:<bookrecedge-port>/bookrecedge

To verify that Resilience4J fault tolerance mechanism is working as expected, stop the book recommendation service, and access the book recommendation edge service again. The default recommended book should be returned instead and the application keeps working.

To follow up with the circuit breaker state and useful statistics just access Spring Actuator health endpoint, as the circuit breaker information is enabled in the service properties loaded via `configstore`, specifically the properties `management.endpoint.health.show-details` and `management.health.circuitbreakers.enabled`. In addition to health endpoint, there are other interesting sources of information like these examples:

    http://localhost:<bookrecedge-port>/actuator/health
    http://localhost:<bookrecedge-port>/actuator/metrics/resilience4j.circuitbreaker.state
    http://localhost:<bookrecedge-port>/actuator/metrics/resilience4j.circuitbreaker.calls
    http://localhost:<bookrecedge-port>/circuitbreakerevents/bookrec

## Running services in Kubernetes with Rancher Desktop, K3s and nerdctl

### Building the images

Building the images is a process that varies depending on the OS, containerization platform and tooling used to manage the image and container life cycle.

The following instructions are based on Rancher Desktop, K3s and nerdctl tooling. Rancher Desktop is an easy way to run Kubernetes on a workstation thanks to the use of K3s lightweight distribution, and nerdctl is very convenient as it leverages many of the well-known docker cli commands but for containerd engine.

To ensure that the container images are available to the local Kubernetes cluster, the namespace parameter must be supplied with ```k8s.io``` value. The following commands must be run in the corresponding folder for each microservice:

    nerdctl -n k8s.io build -t deors-demos-microservices-configservice:1.0-SNAPSHOT .
    nerdctl -n k8s.io build -t deors-demos-microservices-eurekaservice:1.0-SNAPSHOT .
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

Once confirmed that the service is working, the port must be exposed so the other microservices can connect with it (the port forward is just for convenience to our own tests):

    kubectl expose pod configservice --port 6868

Next, run Eureka taking into consideration that it needs to connect with the configuration service, expose the port and verify that it is running as expected:

    kubectl run eurekaservice --image deors-demos-microservices-eurekaservice:1.0-SNAPSHOT --env "CONFIG_HOST=configservice"
    kubectl expose pod eurekaservice --port 7878
    kubectl port-forward pods/eurekaservice 7878:7878
    http://localhost:7878/actuator/health
    http://localhost:7878

The business services are not configured to run with a fixed port by default, which means that they get a random port assigned at boot time unless it is passed as a parameter. To discover that port, it is possible to look into the pod logs, or altenatively, as Eureka is configured and the services are registering there, it is possible to look for the port at Eureka web dashboard.

While using random ports is great for local testing, in Kubernetes each pod gets its own IP address and there would not be port conflicts when scheduling other pods. Therefore, to simplify wiring together different services it is recommended to fix ports where services are listening, either in the deployment configuration or passed as a parameter or environment variable.

With that into consideration, proceed with the business services, and verify whether they are working fine:

    kubectl run bookrecservice --image deors-demos-microservices-bookrecservice:1.0-SNAPSHOT --env "CONFIG_HOST=configservice" --env "EUREKA_HOST=eurekaservice" --env "EUREKA_PORT=7878" --env "PORT=8080"
    kubectl expose pod bookrecservice --port 8080
    kubectl port-forward pods/bookrecservice 8080:8080
    http://localhost:8080/actuator/health
    http://localhost:8080/bookrec

    kubectl run bookrecedgeservice --image deors-demos-microservices-bookrecedgeservice:1.0-SNAPSHOT --env "CONFIG_HOST=configservice" --env "EUREKA_HOST=eurekaservice" --env "EUREKA_PORT=7878" --env "PORT=8181"
    kubectl expose pod bookrecedgeservice --port 8181
    kubectl port-forward pods/bookrecedgeservice 8181:8181
    http://localhost:8181/actuator/health
    http://localhost:8181/bookrecedge

If everything run as expected and all services are wired through Eureka and Kubernetes services, the book recommendation edge service will be returning valid book recommendations from the defined list. If not, the default book recommendation will be returned.

### Verifying graceful degradation of the book recommendation service

To check the fault tolerance and graceful degration functionality, once everything is up and running, remove the book recommendation Kubernetes service. That way, although the pod is running it will not be accessible to the other pods in our system (but it will be to us through the port forward!), and the edge service will return the default book recommendation:

    kubectl delete service bookrecservice
    http://localhost:8080/bookrec
    http://localhost:8181/bookrecedge
    http://localhost:8181/actuator/health

By simply recreating the Kubernetes service the circuit will be closed again and the edge service will return to normal behavior:

    kubectl expose pod bookrecservice --port 8080
    http://localhost:8080/bookrec
    http://localhost:8181/bookrecedge
    http://localhost:8181/actuator/health

During these tests the additional endpoints for circuit breaker metrics and events will be useful to confirm the behavior of the circuit breaker:

    http://localhost:8181/actuator/metrics/resilience4j.circuitbreaker.state
    http://localhost:8181/actuator/metrics/resilience4j.circuitbreaker.calls
    http://localhost:8181/circuitbreakerevents/bookrec

This is an example of fault tolerance and graceful degradation with the help or circuit breakers, reducing the impact of internal failure to customer-facing APIs and user interfaces.

### Cleaning up resources

To clean up resources all pods and services need to be terminated. If any port forward is still open they should be stopped (Ctrl-C), too:

    kubectl delete pod bookrecedgeservice
    kubectl delete service bookrecedgeservice

    kubectl delete pod bookrecservice
    kubectl delete service bookrecservice

    kubectl delete pod eurekaservice
    kubectl delete service eurekaservice

    kubectl delete pod configservice
    kubectl delete service configservice
