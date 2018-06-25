package deors.demos.microservices.configservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.cloud.config.server.EnableConfigServer
public class ConfigserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigserviceApplication.class, args);
    }
}
