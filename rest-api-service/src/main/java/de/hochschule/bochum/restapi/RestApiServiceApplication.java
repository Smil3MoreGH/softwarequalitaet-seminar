package de.hochschule.bochum.restapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "de.hochschule.bochum.restapi.repository")
public class RestApiServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RestApiServiceApplication.class, args);
    }
}