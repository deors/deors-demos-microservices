package deors.demos.microservices.bookrecedgeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@org.springframework.cloud.client.discovery.EnableDiscoveryClient
@org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker
public class BookrecedgeserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookrecedgeserviceApplication.class, args);
    }

    @Bean
    @org.springframework.cloud.client.loadbalancer.LoadBalanced
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
