package deors.demos.microservices.bookrecservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.cloud.client.discovery.EnableDiscoveryClient
public class BookrecserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookrecserviceApplication.class, args);
    }
}
