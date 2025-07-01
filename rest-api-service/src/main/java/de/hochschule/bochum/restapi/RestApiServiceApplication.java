package de.hochschule.bochum.restapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

// Hier starte ich meinen REST-API-Service und aktiviere die MongoDB-Repositories
@SpringBootApplication
@EnableMongoRepositories(basePackages = "de.hochschule.bochum.restapi.repository")
public class RestApiServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RestApiServiceApplication.class, args);
    }
}
