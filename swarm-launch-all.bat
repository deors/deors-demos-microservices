docker service create -p 8888:8888 --name configservice --network microdemonet deors/deors.demos.microservices.configservice:latest
sleep 30
docker service create -p 7878:7878 --name eurekaservice --network microdemonet -e "EUREKA_HOST=eurekaservice" deors/deors.demos.microservices.eurekaservice:latest
sleep 30
docker service create -p 7979:7979 --name hystrixservice --network microdemonet -e "CONFIG_HOST=configservice" -e "EUREKA_HOST=eurekaservice" deors/deors.demos.microservices.hystrixservice:latest
sleep 30
docker service create -p 8989:8989 --name turbineservice --network microdemonet -e "CONFIG_HOST=configservice" -e "EUREKA_HOST=eurekaservice" deors/deors.demos.microservices.turbineservice:latest
sleep 30
docker service create -p 8080:8080 --name bookrecservice --network microdemonet -e "CONFIG_HOST=configservice" -e "EUREKA_HOST=eurekaservice" deors/deors.demos.microservices.bookrecservice:latest
sleep 30
docker service create -p 8181:8181 --name bookrecedgeservice --network microdemonet -e "CONFIG_HOST=configservice" -e "EUREKA_HOST=eurekaservice" deors/deors.demos.microservices.bookrecedgeservice:latest
