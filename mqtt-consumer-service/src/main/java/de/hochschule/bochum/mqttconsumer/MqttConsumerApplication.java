package de.hochschule.bochum.mqttconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Hier starte ich meinen MQTT-Consumer-Microservice mit Spring Boot
@SpringBootApplication
public class MqttConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(MqttConsumerApplication.class, args);
    }
}
